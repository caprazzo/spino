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

import java.io.Serializable;
import java.net.URL;

/**
 * An instance of a service at an address
 */
final class ServiceLocation implements Serializable {
    private final String service;
    private final URL address;

    ServiceLocation(String service, URL address) {
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

        ServiceLocation serviceInstance = (ServiceLocation) o;

        if (!address.equals(serviceInstance.address)) return false;
        if (!service.equals(serviceInstance.service)) return false;

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
        return String.format("ServiceLocation(service=%s, address=%s)", service, address);
    }
}
