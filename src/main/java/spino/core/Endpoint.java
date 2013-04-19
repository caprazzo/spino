package spino.core;

import java.io.Serializable;
import java.net.URL;

/**
 * An endpoint is an instance of a Service
 */
final class Endpoint implements Serializable {
    private final String service;
    private final URL address;

    Endpoint(String service, URL address) {
        this.service = service;
        this.address = address;
    }

    public String getService() {
        return service;
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
        if (!service.equals(endpoint.service)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = service.hashCode();
        result = 31 * result + address.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("Endpoint(service=%s, address=%s)", service, address);
    }
}
