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

import com.hazelcast.config.Config;
import com.hazelcast.config.Join;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

final class SpinoImpl {
    private static final Logger LOG = LoggerFactory.getLogger(SpinoImpl.class);

    private static final String SERVICES_MAP = "spino-services";
    private static final String GROUP_NAME = "SPINO";

    private final RoutingTable routingTable = new RoutingTable();

    private HazelcastInstance hz;
    private HazelcastListener handler;
    private Cluster cluster;


    public void start() {
        start(null);
    }

    public void start(String... seeds) {
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

    public void shutdown() {
        Cluster cluster = hz.getCluster();
        cluster.removeMembershipListener(handler);
        getServicesMap().removeEntryListener(handler);
    }

    public void activateServiceEndpoint(String serviceName, URL address) {
        LOG.info("Activating endpoint for endpoint " + serviceName + " at " + address);
        EndpointMember service = new EndpointMember(serviceName, address, cluster.getLocalMember());
        getServicesMap().put(service.getService(), service);
    }

    public void activateServiceEndpoint(String serviceName, String address) {
        try {
            activateServiceEndpoint(serviceName, new URL(address));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deactivateLocalService(String name, URL address) {
        LOG.info("Deactivating endpoint for endpoint " + name + " at " + address);
        EndpointMember service = new EndpointMember(name, address, cluster.getLocalMember());
        getServicesMap().remove(service.getService(), service);
    }

    public void deactivateLocalService(String name, String address) {
        try {
            deactivateLocalService(name, new URL(address));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<URL> getServiceAddresses(String serviceName) {
        ArrayList<URL> endpoints = new ArrayList<URL>();
        Map<EndpointMember, Boolean> services = routingTable.services(serviceName);
        for(Map.Entry<EndpointMember, Boolean> entry : services.entrySet()) {
            if(entry.getValue()) {
                endpoints.add(entry.getKey().getEndpoint().getAddress());
            }
        }
        return endpoints;
    }

    private MultiMap<String, EndpointMember> getServicesMap() {
        return hz.getMultiMap(SERVICES_MAP);
    }

    private void syncServiceMap(Cluster cluster) {
        Set<Member> members = cluster.getMembers();
        for (Map.Entry<String, EndpointMember> entry : getServicesMap().entrySet()) {
            LOG.info("Importing existing endpoint from distributed map: {}", entry);
            Member member = entry.getValue().getMember();
            if (!members.contains(member)) {
                LOG.info("Skipping endpoint import from distributed map, because its member is not online: {} ", entry);
                continue;
            }
            routingTable.putService(entry.getValue());
        }
    }

    /**
     * Handles events for
     *  - members added and removed to a cluster
     *  - entries added/removed/updated to a multimap
     */
    private class HazelcastListener implements  MembershipListener, EntryListener<String, EndpointMember> {
        private final Logger LOG = LoggerFactory.getLogger(HazelcastListener.class);

        @Override
        public void entryAdded(EntryEvent<String, EndpointMember> event) {
            if (LOG.isDebugEnabled())
                LOG.debug("entryAdded {}", event);
            routingTable.putService(event.getValue());
        }

        @Override
        public void entryRemoved(EntryEvent<String, EndpointMember> event) {
            if (LOG.isDebugEnabled())
                LOG.debug("entryRemoved {}", event);
            routingTable.removeService(event.getValue());
        }

        @Override
        public void entryUpdated(EntryEvent<String, EndpointMember> event) {
            if (LOG.isDebugEnabled())
                LOG.debug("entryUpdated {}", event);
            routingTable.putService(event.getValue());
        }

        @Override
        public void entryEvicted(EntryEvent<String, EndpointMember> event) {
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
