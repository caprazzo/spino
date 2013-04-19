package spino.core;

import com.hazelcast.core.Member;

import java.io.Serializable;
import java.net.URL;

/**
 * Internal representation of a endpoint
 */
final class EndpointMember implements Serializable {

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
