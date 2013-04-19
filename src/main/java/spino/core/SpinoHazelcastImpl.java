/**
 * Copyright 2013 Matteo Caprari
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spino.core;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.hazelcast.config.Config;
import com.hazelcast.config.Join;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 *  How it works:
 *
 *  Data:
 *      ServiceMap: an Hazelcast Multimap service -> (member, service, address)
 *      RoutingTable: a local Table (Hazelcat Member, Server, (member, service, address), Status)
 *
 *  Service Activation:
 *      When a service is activated on an address using activateServiceLocation
 *      an element is added to ServiceMap  service -> (LocalMember, service, address)
 *
 *      When ServiceMap notifies entryAdded(sender, (member, service, address)),
 *      an entry is added to the Routing Table: (member, service, (member, service, address), Active)
 *
 *      When Hazelcast notifies that a member has been added, all RoutingTable
 *      entries for that Member are set to Active.
 *
 *  Service Deactivation:
 *      When a service is deactivated on an address using deactivateServiceLocation,
 *      an element is removed from the ServiceMap service -> (LocalMember, service, address)
 *
 *      When ServiceMap notifies entryRemoved, the entry is removed from RoutingTable
 *
 *      When Hazelcast notifies that a member has been removed, all RoutingTable
 *      entries for that Member are set to Inactive.
 *
 *  Listing Active Service Locations:
 *      to list all locations for a service, it is enough to
 *      query the Routing Table by service.
 *
 *      Because the Active flag in the routing table is managed when
 *      members come and leave, it should always reflect the actual state
 *      of the cluster.
 *
 *  Setting Initial State:
 *      At startup, populate
 *
 *  TODO: before adding an entry in the routing table, always check if the member is active
 *  TODO: allow users to listen for changes in service availability
 *
 */
final class SpinoHazelcastImpl implements RoutingTable.RoutingTableListener {
    private static final Logger LOG = LoggerFactory.getLogger(SpinoHazelcastImpl.class);

    private final Multimap<String, SpinoServiceListener> listeners = ArrayListMultimap.create();

    private static final String SERVICES_MAP = "spino-services";
    private static final String GROUP_NAME = "SPINO";

    private final RoutingTable routingTable;

    SpinoHazelcastImpl() {
        routingTable = new RoutingTable(this);
    }

    private HazelcastInstance hz;
    private HazelcastListener handler;
    private Cluster cluster;

    void start() {
        start(null);
    }

    void start(String... seeds) {
        handler = this.new HazelcastListener();
        Config hzConfig = new Config();

        if (seeds != null) {
            NetworkConfig networkConfig = hzConfig.getNetworkConfig();
            Join join = networkConfig.getJoin();
            join.getMulticastConfig().setEnabled(false);
            for(String address : seeds) {
                join.getTcpIpConfig().addMember(address);
            }
        }

        hzConfig.getGroupConfig().setName(GROUP_NAME);
        this.hz = Hazelcast.newHazelcastInstance(hzConfig);
        this.cluster = hz.getCluster();
        cluster.addMembershipListener(handler);
        getServicesMap().addEntryListener(handler, true);
        syncServiceMap(cluster);
    }

    void shutdown() {
        Cluster cluster = hz.getCluster();
        cluster.removeMembershipListener(handler);
        getServicesMap().removeEntryListener(handler);
    }

    void activateServiceLocation(String service, URL address) {
        LOG.info("Activating endpoint for endpoint " + service + " at " + address);
        LocationBinding binding = new LocationBinding(service, address, cluster.getLocalMember());
        getServicesMap().put(service, binding);
    }

    void activateServiceLocation(String service, String address) {
        try {
            activateServiceLocation(service, new URL(address));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    void deactivateServiceLocation(String service, URL address) {
        LOG.info("Deactivating endpoint for endpoint " + service + " at " + address);
        LocationBinding binding = new LocationBinding(service, address, cluster.getLocalMember());
        getServicesMap().remove(service, binding);
    }

    void deactivateServiceLocation(String service, String address) {
        try {
            deactivateServiceLocation(service, new URL(address));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    Collection<URL> getServiceAddresses(String service) {
        return routingTable.getServiceAddresses(service);
    }

    /**
     * Add a service listener. The listener is invoked whenever locations are added or removed.
     * @param service
     * @param listener
     */
    void addServiceListener(String service, SpinoServiceListener listener) {
        listeners.put(service, listener);
    }

    /**
     * Remove a service listener for ase
     * @param service
     * @param listener
     */
    void removeServiceListener(String service, SpinoServiceListener listener) {
        listeners.remove(service, listener);
    }

    private MultiMap<String, LocationBinding> getServicesMap() {
        return hz.getMultiMap(SERVICES_MAP);
    }

    private void syncServiceMap(Cluster cluster) {
        Set<Member> onlineMembers = cluster.getMembers();
        for (Map.Entry<String, LocationBinding> entry : getServicesMap().entrySet()) {
            LOG.info("Importing existing endpoint from distributed map: {}", entry);
            Member member = entry.getValue().getMember();
            if (!onlineMembers.contains(member)) {
                LOG.info("Skipping endpoint import from distributed map, because its member is not online: {} ", entry);
                continue;
            }
            routingTable.putService(entry.getValue());
        }
    }

    @Override
    public void onRoutingTableChange(Collection<String> services) {
        for (String service : services) {
            for(SpinoServiceListener listener : listeners.get(service)) {
                try {
                    listener.onServiceChange(service);
                }
                catch(Exception ex) {
                    LOG.error("listener.OnServiceChange(" + service + ") threw an Exception. Listener: " + listener, ex);
                }
            }
        }

    }

    /**
     * Handles events for
     *  - members added and removed to a cluster
     *  - entries added/removed/updated to a multimap
     */
    private class HazelcastListener implements  MembershipListener, EntryListener<String, LocationBinding> {
        private final Logger LOG = LoggerFactory.getLogger(HazelcastListener.class);

        @Override
        public void entryAdded(EntryEvent<String, LocationBinding> event) {
            if (LOG.isDebugEnabled())
                LOG.debug("entryAdded {}", event);
            routingTable.putService(event.getValue());
        }

        @Override
        public void entryRemoved(EntryEvent<String, LocationBinding> event) {
            if (LOG.isDebugEnabled())
                LOG.debug("entryRemoved {}", event);
            routingTable.removeService(event.getValue());
        }

        @Override
        public void entryUpdated(EntryEvent<String, LocationBinding> event) {
            if (LOG.isDebugEnabled())
                LOG.debug("entryUpdated {}", event);
            routingTable.putService(event.getValue());
        }

        @Override
        public void entryEvicted(EntryEvent<String, LocationBinding> event) {
            if (LOG.isDebugEnabled())
                LOG.debug("entryEvicted {}", event);
            routingTable.removeService(event.getValue());
        }

        @Override
        public void memberAdded(MembershipEvent membershipEvent) {
            if (LOG.isDebugEnabled())
                LOG.debug("memberAdded {}", membershipEvent);
            routingTable.addMember(membershipEvent.getMember());
        }

        @Override
        public void memberRemoved(MembershipEvent membershipEvent) {
            if (LOG.isDebugEnabled())
                LOG.debug("memberRemoved {}", membershipEvent);
            routingTable.removeMember(membershipEvent.getMember());
        }
    }

}
