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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

final class RoutingTable {

    private volatile Multimap<String, URL> serviceMap;

    public interface RoutingTableListener {
        void onRoutingTableChange();
    }

    private final RoutingTableListener listener;

    private final Object lock = new Object();

    private final Logger LOG = LoggerFactory.getLogger(RoutingTable.class);

    // a table  | Member | Service | LocationBinding |
    // to maintain a double index on LocationBinding
    private HashBasedTable<Member, String, LocationBinding> serviceTable = HashBasedTable.create();

    // maintains enabled/disabled status for getServiceAddresses
    private HashMap<LocationBinding, Boolean> statusIndex = new HashMap<LocationBinding, Boolean>();

    RoutingTable(RoutingTableListener listener) {
        this.listener = listener;
    }

    /**
     * Retrieve all addresses by service
     * @param service
     * @return a Map LocationBinding -> boolean enabled/disabled status for getServiceAddresses
     */
     Collection<URL> getServiceAddresses(String service) {
        return serviceMap.get(service);
     }

    void putService(LocationBinding service) {
        LOG.info("put endpoint {}", service);
        synchronized (lock) {
            serviceTable.put(service.getMember(), service.getService(), service);
            statusIndex.put(service, true);
            updateCaches();
        }
        notifyChange();
    }

    void removeService(LocationBinding service) {
        LOG.info("remove endpoint {}", service);
        synchronized (lock) {
            serviceTable.remove(service.getMember(), service.getService());
            statusIndex.remove(service);
            updateCaches();
        }
        notifyChange();
    }

    void removeMember(Member member) {
        LOG.info("Disabling all entries for removed [{}]", member);
        synchronized (lock) {
            for (LocationBinding service: serviceTable.row(member).values()) {
                statusIndex.put(service, false);
                updateCaches();
            }
        }
        notifyChange();
    }

    void addMember(Member member) {
        LOG.info("Enabling all entries for added [{}]", member);
        synchronized (lock) {
            for (LocationBinding service: serviceTable.row(member).values()) {
                statusIndex.put(service, true);
                updateCaches();
            }
        }
        notifyChange();
    }

    private void notifyChange() {
        try {
            listener.onRoutingTableChange();
        }
        catch(Exception ex) {
            LOG.error("Exception during listener execution", ex);
        }

        if (LOG.isDebugEnabled()) {
            DumpTable();
        }
    }

    private HashMap<LocationBinding, Boolean> statusMap(Collection<LocationBinding> services) {
        HashMap<LocationBinding, Boolean> statusMap = new HashMap<LocationBinding, Boolean>();
        synchronized (lock) {
            for (LocationBinding service : services) {
                statusMap.put(service, statusIndex.get(service));
                updateCaches();
            }
        }
        return statusMap;
    }

    private void updateCaches() {
        ArrayListMultimap<String, URL> map = ArrayListMultimap.create();
        synchronized (lock) {
            for(Map.Entry<String, Map<Member, LocationBinding>> entry : serviceTable.columnMap().entrySet()) {
                for(LocationBinding binding : entry.getValue().values()) {
                    if (statusIndex.get(binding)) {
                        map.put(entry.getKey(), binding.getAddress());
                    }
                }
            }
        }
        serviceMap = map;
    }

    private void DumpTable() {
        synchronized (lock) {
            for(Table.Cell<Member, String, LocationBinding> cell : serviceTable.cellSet()) {
                LOG.info("{} | {} | {} | {}", cell.getRowKey(), cell.getColumnKey(), cell.getValue(), statusIndex.get(cell.getValue()));
            }
        }
    }
}