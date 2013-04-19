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