package lab.spino.core;

import ch.qos.logback.classic.Level;
import spino.core.Spino;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class Example {

    public static void main(String[] args) throws IOException {
        Spino spino = new Spino();

        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);

        spino.start("auth-host-0", "auth-host-1");
        spino.activateServiceEndpoint("some-service", new URL("http://db-01:8001"));
        spino.activateServiceEndpoint("some-service", "http://db-01:8001");

        List<Spino.Endpoint> serviceEndpoints = spino.getServiceEndpoints("some-service");
        for (Spino.Endpoint endpoint : serviceEndpoints) {
            System.out.println(endpoint.getName());
            System.out.println(endpoint.getAddress());
        }

        spino.shutdown();

    }

}
