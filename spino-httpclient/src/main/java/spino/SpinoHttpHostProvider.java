package spino;

import org.apache.http.HttpHost;
import spino.core.Spino;
import spino.core.SpinoServiceListener;

import java.net.URL;
import java.util.*;

/**
 * Provides an always up-to-date view of available
 * HttpHosts for a given service.
 *
 * This is useful when using Spino with Apache HttpClient
 * or Httpclient-Failover
 */
public class SpinoHttpHostProvider implements Iterable<HttpHost> {

    private ArrayList<HttpHost> hosts = new ArrayList<HttpHost>();

    private final String service;

    public static Iterable<HttpHost> ofService(String service) {
        return new SpinoHttpHostProvider(service);
    }

    private SpinoHttpHostProvider(String service) {
        this.service = service;
        Spino.addServiceListener(service, new SpinoServiceListener() {
            @Override
            public void onServiceChange(String service) {
                buildHostsList(Spino.getServiceAddresses(service));
            }
        });
        buildHostsList(Spino.getServiceAddresses(service));
    }

    private synchronized void buildHostsList(Collection<URL> addresses) {
        HashSet<URL> URLs = new HashSet<URL>(addresses);
        URLs.removeAll(hosts);

        ArrayList<HttpHost> newHosts = new ArrayList<HttpHost>();
        // add any new host
        for(URL address : URLs) {
            newHosts.add(new HttpHost(address.getHost(), address.getPort(), address.getProtocol()));
        }

        hosts = newHosts;
    }

    @Override
    public synchronized Iterator<HttpHost> iterator() {
        return hosts.iterator();

    }
}
