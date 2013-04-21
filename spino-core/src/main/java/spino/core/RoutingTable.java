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
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.hazelcast.core.Member;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;

final class RoutingTable {

    private volatile Multimap<String, URL> serviceMap = ArrayListMultimap.create();

    public interface RoutingTableListener {
        /**
         * Invoked by routingTable when modified.
         * @param services services that have been affected by this change
         */
        void onRoutingTableChange(Collection<String> services);
    }

    private final RoutingTableListener listener;

    private final Object lock = new Object();

    private final Logger LOG = LoggerFactory.getLogger(RoutingTable.class);

    // a table  | Member | Service | LocationBinding |
    // to maintain a double index on LocationBinding
    private HashBasedTable<Member, String, LocationBinding> serviceTable = HashBasedTable.create();

    // maintains enabled/disabled status for getLocations
    private HashMap<LocationBinding, Boolean> statusIndex = new HashMap<LocationBinding, Boolean>();

    RoutingTable(RoutingTableListener listener) {
        this.listener = listener;
    }

    /**
     * Retrieve all addresses by service
     * @param service
     * @return a Collection of URLs available for this service
     */
     Collection<URL> getServiceAddresses(String service) {
        return serviceMap.get(service);
     }

    void addLocation(LocationBinding binding) {
        LOG.info("Adding {}", binding);
        synchronized (lock) {
            serviceTable.put(binding.getMember(), binding.getService(), binding);
            statusIndex.put(binding, true);
            updateCaches();
        }
        notifyChange(Arrays.asList(binding.getService()));
    }

    void removeLocation(LocationBinding binding) {
        LOG.info("Removing {}", binding);
        synchronized (lock) {
            serviceTable.remove(binding.getMember(), binding.getService());
            statusIndex.remove(binding);
            updateCaches();
        }
        notifyChange(Arrays.asList(binding.getService()));
    }

    void removeMember(Member member) {
        LOG.info("Disabling all entries for removed [{}]", member);
        Set<String> affected = new HashSet<String>();
        synchronized (lock) {
            for (LocationBinding binding: serviceTable.row(member).values()) {
                statusIndex.put(binding, false);
                affected.add(binding.getService());
                updateCaches();
            }
        }
        notifyChange(affected);
    }

    void addMember(Member member) {
        LOG.info("Enabling all entries for added [{}]", member);
        Set<String> affected = new HashSet<String>();
        synchronized (lock) {
            for (LocationBinding binding: serviceTable.row(member).values()) {
                statusIndex.put(binding, true);
                affected.add(binding.getService());
                updateCaches();
            }
        }
        notifyChange(affected);
    }

    private void notifyChange(Collection<String> services) {
        try {
            listener.onRoutingTableChange(services);
        }
        catch(Exception ex) {
            LOG.error("Exception during listener execution", ex);
        }

        if (LOG.isDebugEnabled()) {
            DumpTable();
        }
    }

    private void updateCaches() {
        ArrayListMultimap<String, URL> tempServiceMap = ArrayListMultimap.create();
        for(Map.Entry<String, Map<Member, LocationBinding>> entry : serviceTable.columnMap().entrySet()) {
            for(LocationBinding binding : entry.getValue().values()) {
                if (statusIndex.get(binding)) {
                    tempServiceMap.put(entry.getKey(), binding.getAddress());
                }
            }
        }
        serviceMap = tempServiceMap;
    }

    private void DumpTable() {
        synchronized (lock) {
            for(Table.Cell<Member, String, LocationBinding> cell : serviceTable.cellSet()) {
                LOG.info("{} | {} | {} | {}", cell.getRowKey(), cell.getColumnKey(), cell.getValue(), statusIndex.get(cell.getValue()));
            }
        }
    }
}