package lab.spino.core;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import spino.core.Spino;
import spino.core.SpinoServiceListener;

import java.io.IOException;
import java.net.URL;
import java.util.Random;

public class Example {

    public static void main(String[] args) throws IOException {

        Random random = new Random();

        Logger root = (Logger) org.slf4j.LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);

        listLocations("some-service");

        Spino.addServiceListener("some-service", new SpinoServiceListener() {
            @Override
            public void onServiceChange(String service) {
                System.out.println("Service " + service + " changed");
                listLocations(service);
            }
        });

        Spino.start();

        Spino.activateLocation("some-service", new URL("http://db-01:" + random.nextInt(64000)));
        //Spino.activateLocation("some-service", "http://db-01:8003");
    }

    public static void listLocations(String service) {
        System.out.print("Locations for " + service + ": [");
        for (URL address : Spino.getLocations("some-service")) {
            System.out.print(" " + address);
        }
        System.out.println(" ]");
    }

}
