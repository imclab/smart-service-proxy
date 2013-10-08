package eu.spitfire.ssp.backends.uberdust;

import com.hp.hpl.jena.rdf.model.Model;
import eu.spitfire.ssp.backends.generic.BackendComponentFactory;
import eu.spitfire.ssp.backends.generic.DataOriginRegistry;

import java.net.URI;
import java.nio.file.Path;

/**
 * Created by amaxilatis on 10/8/13.
 */
public class UberdustDataOriginRegistry extends DataOriginRegistry<URI> {

    protected UberdustDataOriginRegistry(BackendComponentFactory<URI> backendComponentFactory) {
        super(backendComponentFactory);
    }


    public void registerResource(Model model, URI uri) {
        super.registerResource(uri, model);
    }
}
