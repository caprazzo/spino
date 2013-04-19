package spino.core;


import java.net.URL;
import java.util.List;

public class Spino {
    private static final SpinoImpl INSTANCE = new SpinoImpl();

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
    public static void start(String seeds) {
        INSTANCE.start(seeds);
    }

    /**
     * Leave the Spino cluster.
     */
    public static void shutdown() {
        INSTANCE.shutdown();
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
    public static void activateServiceEndpoint(String serviceName, URL address) {
        INSTANCE.activateServiceEndpoint(serviceName, address);
    }

    public static void activateServiceEndpoint(String serviceName, String address) {
        INSTANCE.activateServiceEndpoint(serviceName, address);
    }

    /**
     * Deactivate a endpoint endpoint. The caller
     * is declaring that the instance of `serviceName` will not
     * be available at this address.
     *
     * De-activating a deactivated or non-existing endpoint has no effect.
     *
     * @param serviceName - the name of the endpoint.
     * @param address URL where the endpoint can be reached.
     */
    public static void deactivateLocalService(String serviceName, URL address) {
        INSTANCE.deactivateLocalService(serviceName, address);
    }

    public static void deactivateLocalService(String serviceName, String address) {
        INSTANCE.deactivateLocalService(serviceName, address);
    }

    /**
     * List all active addresses for a service. An endpoint is active when it has been de
     * @param serviceName - the name of the endpoint
     * @return
     */
    public static List<URL> getServiceAddresses(String serviceName) {
        return INSTANCE.getServiceAddresses(serviceName);
    }
}
