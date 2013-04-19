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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.hazelcast.config.Config;
import com.hazelcast.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Spino is a simple tool to coordinate a cluster of Http services.
 */
public class Spino {

    private final static Logger LOG = LoggerFactory.getLogger(Spino.class);
    private static final String SERVICES_MAP = "spino-services";

    private static final String GROUP_NAME = "SPINO";

    private final RoutingTable routingTable = new RoutingTable();

    private HazelcastInstance hz;
    private HazelcastListener handler;
    private Cluster cluster;

    /**
     * Join the Spino cluster.
     */
    public void start() {
        handler = this.new HazelcastListener();
        Config hzConfig = new Config();
        hzConfig.getGroupConfig().setName(GROUP_NAME);
        this.hz = Hazelcast.newHazelcastInstance(hzConfig);
        this.cluster = hz.getCluster();
        cluster.addMembershipListener(handler);
        getServicesMap().addEntryListener(handler, true);
        syncServiceMap(cluster);
    }

    /**
     * Leave the Spino cluster.
     */
    public void shutdown() {
        Cluster cluster = hz.getCluster();
        cluster.removeMembershipListener(handler);
        getServicesMap().removeEntryListener(handler);
    }

    /**
     * Activate an endpoint for a endpoint. The caller
     * is declaring that an instance of endpoint `serviceName` is now available
     * to accept connections. All other nodes in the cluster will be notified.
     *
     * Re-activating an active endpoint has no effect.
     *
     * @param serviceName - the name of the endpoint. Must be unique across the cluster.
     * @param address URL of the endpoint
     */
    public void activateServiceEndpoint(String serviceName, URL address) {
        LOG.info("Activating endpoint for endpoint " + serviceName + " at " + address);
        EndpointMember service = new EndpointMember(serviceName, address, cluster.getLocalMember());
        getServicesMap().put(service.getName(), service);
    }

    public void activateServiceEndpoint(String serviceName, String address) {
        try {
            activateServiceEndpoint(serviceName, new URL(address));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deactivate a endpoint endpoint. The caller
     * is declaring that the instance of `serviceName` will not
     * be available at this address.
     *
     * De-activating a deactivated or non-existing endpoint has no effect.
     *
     * @param name - the name of the endpoint.
     * @param address URL where the endpoint can be reached.
     */
    public void deactivateLocalService(String name, URL address) {
        LOG.info("Deactivating endpoint for endpoint " + name + " at " + address);
        EndpointMember service = new EndpointMember(name, address, cluster.getLocalMember());
        getServicesMap().remove(service.getName(), service);
    }

    public void deactivateLocalService(String name, String address) {
        try {
            deactivateLocalService(name, new URL(address));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * List all active endpoints for a endpoint. An endpoint is active when it has been de
     * @param name - the name of the endpoint
     * @return
     */
    public List<Endpoint> getServiceEndpoints(String name) {
        ArrayList<Endpoint> endpoints = new ArrayList<Endpoint>();
        Map<EndpointMember, Boolean> services = routingTable.services(name);
        for(Map.Entry<EndpointMember, Boolean> entry : services.entrySet()) {
            if(entry.getValue()) {
                endpoints.add(entry.getKey().getEndpoint());
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

    private static final class RoutingTable {
        private final Logger LOG = LoggerFactory.getLogger(RoutingTable.class);

        // a table  | Member | EndpointMember Name | EndpointMember |
        // to maintain a double index on EndpointMember
        private HashBasedTable<Member, String, EndpointMember> serviceTable = HashBasedTable.create();

        // maintains enabled/disabled status for services
        private HashMap<EndpointMember, Boolean> statusIndex = new HashMap<EndpointMember, Boolean>();

        synchronized public void putService(EndpointMember service) {
            LOG.info("put endpoint {}", service);
            serviceTable.put(service.getMember(), service.getName(), service);
            statusIndex.put(service, true);
            DumpTable();
        }

        synchronized public void removeService(EndpointMember service) {
            LOG.info("remove endpoint {}", service);
            serviceTable.remove(service.getMember(), service.getName());
            statusIndex.remove(service);
            DumpTable();
        }

        /**
         * Retrieve all services offered by a member.
         * @param member
         * @return a Map EndpointMember -> boolean enabled/disabled status for services
         */
        synchronized public Map<EndpointMember, Boolean> services(Member member) {
            return statusMap(serviceTable.row(member).values());
        }

        /**
         * Retrieve all endpoint instances by name.
         * @param name
         * @return a Map EndpointMember -> boolean enabled/disabled status for services
         */
        synchronized public Map<EndpointMember, Boolean> services(String name) {
            return statusMap(serviceTable.column(name).values());
        }

        synchronized public void removeMember(Member member) {
            LOG.info("Disabling all entries for removed [{}]", member);
            for (EndpointMember service: serviceTable.row(member).values()) {
                statusIndex.put(service, false);
            }
            DumpTable();
        }

        synchronized public void addMember(Member member) {
            LOG.info("Enabling all entries for added [{}]", member);
            for (EndpointMember service: serviceTable.row(member).values()) {
                statusIndex.put(service, true);
            }
            DumpTable();
        }

        synchronized private HashMap<EndpointMember, Boolean> statusMap(Collection<EndpointMember> services) {
            HashMap<EndpointMember, Boolean> statusMap = new HashMap<EndpointMember, Boolean>();
            for (EndpointMember service : services) {
                statusMap.put(service, statusIndex.get(service));
            }
            return statusMap;
        }

        synchronized private void DumpTable() {
            for(Table.Cell<Member, String, EndpointMember> cell : serviceTable.cellSet()) {
                LOG.info("{} | {} | {} | {}", cell.getRowKey(), cell.getColumnKey(), cell.getValue(), statusIndex.get(cell.getValue()));
            }
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

    /**
     * An endpoint is the location where an instance of a service
     * is available.
     */
    public static final class Endpoint implements Serializable {
        private final String name;
        private final URL address;

        Endpoint(String name, URL address) {
            this.name = name;
            this.address = address;
        }

        public String getName() {
            return name;
        }

        public URL getAddress() {
            return address;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Endpoint endpoint = (Endpoint) o;

            if (!address.equals(endpoint.address)) return false;
            if (!name.equals(endpoint.name)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + address.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return String.format("Endpoint(name=%s, address=%s)", name, address);
        }
    }

    /**
     * Internal representation of a endpoint
     */
    private static final class EndpointMember implements Serializable {

        private final Member member;
        private final Endpoint endpoint;

        /**
         * Creates a new instance of EndpointMember
         * @param endpoint - the endpoint
         * @param member - the cluster member that published information about the endpoint
         */
        public EndpointMember(Endpoint endpoint, Member member) {
            this.member = member;
            this.endpoint = endpoint;
        }

        /**
         * Creates a new instance of EndpointMember
         * @param name - name of the endpoint
         * @param address - URL of the endpoint
         * @param member - the cluster member that published information about the endpoint
         */
        public EndpointMember(String name, URL address, Member member) {
            this(new Endpoint(name, address), member);
        }

        public URL getAddress() {
            return endpoint.getAddress();
        }

        public Member getMember() {
            return member;
        }

        public String getName() {
            return endpoint.getName();
        }

        public Endpoint getEndpoint() {
            return endpoint;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EndpointMember other = (EndpointMember) o;

            if (!endpoint.equals(other.endpoint)) return false;
            if (!member.equals(other.member)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = member.hashCode();
            result = 31 * result + endpoint.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return String.format("EndpointMember(member=%s, endpoint=%s)", member, endpoint);
        }
    }
}
