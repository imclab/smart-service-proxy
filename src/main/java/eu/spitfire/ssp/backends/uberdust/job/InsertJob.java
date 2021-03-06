package eu.spitfire.ssp.backends.uberdust.job;

import com.hp.hpl.jena.rdf.model.Model;
import eu.spitfire.ssp.backends.uberdust.UberdustNodeHelper;
import eu.spitfire.ssp.backends.uberdust.UberdustObserver;
import eu.uberdust.communication.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 10/12/13
 * Time: 1:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class InsertJob implements Runnable {
    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private final String prefix;
    private final String testbed;
    private final UberdustObserver observer;
    private Message.NodeReadings.Reading reading;

    public InsertJob(UberdustObserver observer, Message.NodeReadings.Reading reading, String testbed, String prefix) {
        this.prefix = prefix;
        this.testbed = testbed;
        this.reading = reading;
        this.observer = observer;
    }

    @Override
    public void run() {
        long start = 0;
        log.warn("Received Insert with " + (System.currentTimeMillis() - reading.getTimestamp()) + " millis drift. " + Thread.activeCount() + " Threads Running.");
        try {
            start = System.currentTimeMillis();
            Model description = UberdustNodeHelper.generateDescription(reading.getNode(), testbed, prefix, reading.getCapability(), reading.getDoubleReading(), new Date(reading.getTimestamp()));
            String resourceURI = UberdustNodeHelper.getResourceURI(testbed, reading.getNode(), reading.getCapability());
            log.warn("uberdustCreate " + (System.currentTimeMillis() - start) + " millis " + resourceURI.hashCode());
            start = System.currentTimeMillis();
            observer.registerModel(description, resourceURI);
            observer.doCacheResourcesStates(description);
            log.warn("jenaInsert " + (System.currentTimeMillis() - start) + " millis " + resourceURI.hashCode());
        } catch (URISyntaxException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        log.warn("Processed Insert with " + (System.currentTimeMillis() - reading.getTimestamp()) + " millis drift. " + Thread.activeCount() + " Threads Running.");
    }
}
