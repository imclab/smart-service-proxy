package eu.spitfire.ssp.backends.files;

import com.google.common.util.concurrent.SettableFuture;
import com.hp.hpl.jena.rdf.model.Model;
import eu.spitfire.ssp.backends.generic.BackendResourceManager;
import eu.spitfire.ssp.backends.generic.SemanticHttpRequestProcessor;
import eu.spitfire.ssp.backends.generic.exceptions.DataOriginAccessException;
import eu.spitfire.ssp.backends.generic.exceptions.SemanticResourceException;
import eu.spitfire.ssp.backends.generic.messages.InternalResourceStatusMessage;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;


public class HttpRequestProcessorForFiles implements SemanticHttpRequestProcessor {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private BackendResourceManager<Path> backendResourceManager;

    public HttpRequestProcessorForFiles(FilesBackendComponentFactory backendComponentFactory){
        this.backendResourceManager = backendComponentFactory.getBackendResourceManager();
    }

    @Override
    public void processHttpRequest(SettableFuture<InternalResourceStatusMessage> resourceStatusFuture,
                                   HttpRequest httpRequest) {

        log.info("Received request for resource {}", httpRequest.getUri());
        try{
            if(httpRequest.getMethod() == HttpMethod.GET){
                processGET(resourceStatusFuture, httpRequest);
            }
            else if (httpRequest.getMethod() == HttpMethod.POST){
                processPOST(resourceStatusFuture, httpRequest);
            }
            else if (httpRequest.getMethod() == HttpMethod.PUT){
                processPUT(resourceStatusFuture, httpRequest);
            }
        }
        catch (Exception e) {
            resourceStatusFuture.setException(e);
        }

    }

    public void processPOST(SettableFuture<InternalResourceStatusMessage> resourceStatusFuture,
                            HttpRequest httpRequest) throws Exception {

        URI resourceProxyUri = new URI(httpRequest.getUri());
        URI resourceUri = new URI(resourceProxyUri.getQuery().substring(4));

        SemanticResourceException exception = new SemanticResourceException(resourceUri,
                HttpResponseStatus.METHOD_NOT_ALLOWED, "Method POST is not supported for resource " + resourceUri);

        resourceStatusFuture.setException(exception);
    }

    private void processPUT(SettableFuture<InternalResourceStatusMessage> resourceStatusFuture,
                            HttpRequest httpRequest) throws Exception {

        resourceStatusFuture.setException(new DataOriginAccessException(HttpResponseStatus.METHOD_NOT_ALLOWED,
                "Resources from files only allow GET requests"));

//        URI resourceProxyUri = new URI(httpRequest.getUri());
//        URI resourceUri = new URI(resourceProxyUri.getQuery().substring(4));
//
//        //Read model from file
//        Path resourceFile = resources.get(resourceUri);
//
//        synchronized (resourceFile){
//            Model modelFromFile = FilesResourceToolBox.readModelFromFile(resourceFile);
//
//            //Delete the resource from the model
//            Resource resource = modelFromFile.getResource(resourceUri.toString());
//            StmtIterator iterator = resource.listProperties();
//            modelFromFile.remove(iterator);
//
//            //Add the new resource status into the model
//            Model newModel = modelFromFile.union(ResourceToolbox.getModelFromHttpMessage(httpRequest));
//
//            FilesResourceToolBox.writeModelToFile(resourceFile, newModel);
//
//            resourceStatusFuture.set(new InternalResourceStatusMessage(ModelFactory.createDefaultModel()));
//        }
    }

    private void processGET(SettableFuture<InternalResourceStatusMessage> resourceStatusFuture,
                            HttpRequest httpRequest){
        try{

            URI resourceProxyUri = new URI(httpRequest.getUri());
            URI resourceUri = new URI(resourceProxyUri.getQuery().substring(4));

            Path file = backendResourceManager.getDataOrigin(resourceUri);

            Model modelFromFile = FilesResourceToolBox.readModelFromFile(file);
            Map<URI, Model> models = FilesResourceToolBox.getModelsPerSubject(modelFromFile);

            Model model = models.get(resourceUri);

            if(model == null){
                resourceStatusFuture.setException(new DataOriginAccessException(HttpResponseStatus.NOT_FOUND,
                        "Could not find resource " + resourceUri + " in file " + file));
            }
            else{
                InternalResourceStatusMessage internalResourceStatusMessage = new InternalResourceStatusMessage(model);
                resourceStatusFuture.set(internalResourceStatusMessage);
            }
        }
        catch(Exception e){
            log.error("This should never happen!", e);
            resourceStatusFuture.setException(new Exception("Something went really wrong!"));
        }
    }
}
