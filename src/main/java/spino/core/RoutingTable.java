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
import com.hazelcast.core.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

final class RoutingTable {
    private final Logger LOG = LoggerFactory.getLogger(RoutingTable.class);

    // a table  | Member | Service | LocationBinding |
    // to maintain a double index on LocationBinding
    private HashBasedTable<Member, String, LocationBinding> serviceTable = HashBasedTable.create();

    // maintains enabled/disabled status for services
    private HashMap<LocationBinding, Boolean> statusIndex = new HashMap<LocationBinding, Boolean>();

    synchronized void putService(LocationBinding service) {
        LOG.info("put endpoint {}", service);
        serviceTable.put(service.getMember(), service.getService(), service);
        statusIndex.put(service, true);
        DumpTable();
    }

    synchronized void removeService(LocationBinding service) {
        LOG.info("remove endpoint {}", service);
        serviceTable.remove(service.getMember(), service.getService());
        statusIndex.remove(service);
        DumpTable();
    }

    /**
     * Retrieve all services offered by a member.
     * @param member
     * @return a Map LocationBinding -> boolean enabled/disabled status for services
     */
    synchronized Map<LocationBinding, Boolean> services(Member member) {
        return statusMap(serviceTable.row(member).values());
    }

    /**
     * Retrieve all endpoints by service
     * @param service
     * @return a Map LocationBinding -> boolean enabled/disabled status for services
     */
    synchronized Map<LocationBinding, Boolean> services(String service) {
        return statusMap(serviceTable.column(service).values());
    }

    synchronized void removeMember(Member member) {
        LOG.info("Disabling all entries for removed [{}]", member);
        for (LocationBinding service: serviceTable.row(member).values()) {
            statusIndex.put(service, false);
        }
        DumpTable();
    }

    synchronized void addMember(Member member) {
        LOG.info("Enabling all entries for added [{}]", member);
        for (LocationBinding service: serviceTable.row(member).values()) {
            statusIndex.put(service, true);
        }
        DumpTable();
    }

    synchronized private HashMap<LocationBinding, Boolean> statusMap(Collection<LocationBinding> services) {
        HashMap<LocationBinding, Boolean> statusMap = new HashMap<LocationBinding, Boolean>();
        for (LocationBinding service : services) {
            statusMap.put(service, statusIndex.get(service));
        }
        return statusMap;
    }

    synchronized private void DumpTable() {
        for(Table.Cell<Member, String, LocationBinding> cell : serviceTable.cellSet()) {
            LOG.info("{} | {} | {} | {}", cell.getRowKey(), cell.getColumnKey(), cell.getValue(), statusIndex.get(cell.getValue()));
        }
    }
}