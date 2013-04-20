import httpfailover.FailoverHttpClient;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import spino.SpinoHttpHostProvider;
import spino.core.Spino;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class Example {

    private static List<HttpHost> hosts = new ArrayList<HttpHost>();

    private static List<HttpHost> activeHosts(Collection<URL> addresses) {
        ArrayList<HttpHost> hosts = new ArrayList<HttpHost>();
        for(URL address : addresses) {
            hosts.add(new HttpHost(address.getHost(), address.getPort(), address.getProtocol()));
        }
        return hosts;
    }

    public static void main(String[] args) throws IOException {

        Spino.start();
        Iterable<HttpHost> provider = SpinoHttpHostProvider.ofService("my-service");

        FailoverHttpClient httpClient = new FailoverHttpClient();

        HttpGet request = new HttpGet("/index.html");

        // this will try the request on all hosts, until it succeeds
        HttpResponse httpResponse = httpClient.execute(provider, request);


        HttpClient client = new DefaultHttpClient();




        // crude example of retrying the same http call over multiple hosts
        // (if this is what you are after, see HttpClient-Failover
        Iterator<HttpHost> hostIterator = provider.iterator();
        while(hostIterator.hasNext()) {
            HttpHost target =  hostIterator.next();
            try {
                HttpResponse response = client.execute(target, request);
                // do something with your response
                break;
            }
            catch(IOException ex) {
                System.err.println("WARNING: Failed to execute " + request + " on " + target);
                if (hostIterator.hasNext()) {
                    System.err.println("WARNING: Trying on next host...");
                }
                else {
                    break;
                }
            }
        }
    }

}
