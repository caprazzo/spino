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

import java.net.URL;
import java.util.Collection;

public class Spino {
    private static final SpinoHazelcastImpl INSTANCE = new SpinoHazelcastImpl();

    /**
     * Join the Spino cluster using Multicast.
     */
    public static void start() {
        INSTANCE.start();
    }

    /**
     * Join the Spino cluster using other known nodes as a starting point.
     * @param seeds - other known nodes of the cluster
     */
    public static void start(String... seeds) {
        INSTANCE.start(seeds);
    }

    /**
     * Leave the Spino cluster.
     */
    public static void shutdown() {
        INSTANCE.shutdown();
    }

    /**
     * Activate a Location for a service. The caller
     * is declaring that an instance of service `serviceName` is now available
     * to accept connections at `address`. All nodes in the cluster will be notified.
     *
     * @param service - the service name.
     * @param address URL of the service instance
     */
    public static void activateLocation(String service, URL address) {
        INSTANCE.activateServiceLocation(service, address);
    }

    public static void activateLocation(String service, String address) {
        INSTANCE.activateServiceLocation(service, address);
    }

    /**
     * Activate a Location for a service. The caller
     * is declaring that an instance of service `serviceName` is now available
     * to accept connections at `address`. All nodes in the cluster will be notified.
     *
     * @param service - the service name.
     * @param address URL of the service instance
     */
    public static void deactivateLocation(String service, URL address) {
        INSTANCE.deactivateServiceLocation(service, address);
    }

    public static void deactivateLocation(String service, String address) {
        INSTANCE.deactivateServiceLocation(service, address);
    }

    /**
     * List all active location for a service
     * @param service - the service name
     * @return
     */
    public static Collection<URL> getLocations(String service) {
        return INSTANCE.getServiceAddresses(service);
    }

    /**
     * Add a service listener.
     * The listener is notified whenever locations are added or removed for this service
     * @param service - the service name
     * @param listener
     */
    public static void addServiceListener(String service, SpinoServiceListener listener) {
        INSTANCE.addServiceListener(service, listener);
    }

    /**
     * Remove a service listener.
     * @param service - the service name
     * @param listener
     */
    public static void removeServiceListener(String service, SpinoServiceListener listener) {
        INSTANCE.removeServiceListener(service, listener);
    }
}
