package spino.core;

import java.io.Serializable;
import java.net.URL;

/**
 * An endpoint is the location where an instance of a service
 * is available.
 */
public final class Endpoint implements Serializable {
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
