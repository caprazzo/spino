package lab.spino.core;

import ch.qos.logback.classic.Level;
import spino.core.SpinoImpl;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class Example {

    public static void main(String[] args) throws IOException {
        SpinoImpl spino = new SpinoImpl();

        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);

        spino.start("auth-host-0", "auth-host-1");
        spino.activateServiceEndpoint("some-service", new URL("http://db-01:8001"));
        spino.activateServiceEndpoint("some-service", "http://db-01:8001");

        List<SpinoImpl.Endpoint> serviceEndpoints = spino.getServiceEndpoints("some-service");
        for (SpinoImpl.Endpoint endpoint : serviceEndpoints) {
            System.out.println(endpoint.getName());
            System.out.println(endpoint.getAddress());
        }

        spino.shutdown();

    }

}
