package eu.spitfire.ssp.backends.coap.registry;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.ncoap.application.server.webservice.NotObservableWebService;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.header.Code;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * This is the WebService for new sensor nodes to register. It's path is <code>/here_i_am</code>. It only accepts
 * {@link CoapRequest}s with code {@link Code#POST}. Any contained payload is ignored.
 *
 * Upon reception of such a request the service sends a {@link CoapRequest} with {@link Code#GET} to the
 * <code>/.well-known/core</code> resource of the sensor node to discover the services available on the new node.
 *
 * Upon discovery of the available services it responds to the original registration request with a proper response
 * code.
 *
 * @author Oliver Kleine
 */
public class CoapRegistrationWebservice extends NotObservableWebService<Boolean>{

    private static Logger log = LoggerFactory.getLogger(CoapRegistrationWebservice.class.getName());

    private CoapWebserviceRegistry coapWebserviceRegistry;
    private ExecutorService executorService;

    public CoapRegistrationWebservice(CoapWebserviceRegistry coapWebserviceRegistry, ExecutorService executorService){
        super("/here_i_am", Boolean.TRUE);
        this.coapWebserviceRegistry = coapWebserviceRegistry;
        this.executorService = executorService;
    }

    @Override
    public void processCoapRequest(final SettableFuture<CoapResponse> registrationResponseFuture,
                                   CoapRequest coapRequest, InetSocketAddress remoteAddress) {

        log.info("Received CoAP registration message from {}: {}", remoteAddress.getAddress(), coapRequest);

        //Only POST messages are allowed
        if(coapRequest.getCode() != Code.POST){
            CoapResponse coapResponse = new CoapResponse(Code.METHOD_NOT_ALLOWED_405);
            registrationResponseFuture.set(coapResponse);
            return;
        }

        //Request was POST, so go ahead
        final ListenableFuture<Set<URI>> registeredResourcesFuture =
                coapWebserviceRegistry.processRegistration(remoteAddress.getAddress());

        registeredResourcesFuture.addListener(new Runnable(){
            @Override
            public void run() {
                try{
                    Set<URI> registeredResources = registeredResourcesFuture.get();
                    if(log.isInfoEnabled()){
                        for(URI resourceUri : registeredResources)
                            log.info("Succesfully registered resource {}", resourceUri);
                    }
                    CoapResponse coapResponse = new CoapResponse(Code.CREATED_201);
                    registrationResponseFuture.set(coapResponse);
                }
                catch(Exception e){
                    CoapResponse coapResponse = new CoapResponse(Code.INTERNAL_SERVER_ERROR_500);
                    registrationResponseFuture.set(coapResponse);
                }
            }
        }, executorService);
    }

    @Override
    public void shutdown() {
        //Nothing to do
    }
}
