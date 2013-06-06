package org.geoserver.web.data.store.cache;

import org.geoserver.data.DataStoreFactoryInitializer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.data.cache.datastore.CachedDataStoreFactory;

/**
 * Initializes an H2 data store factory setting its location to the geoserver
 *  data directory.
 *
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public class CachedDataStoreFactoryInitializer extends 
    DataStoreFactoryInitializer<CachedDataStoreFactory> {

    GeoServerResourceLoader resourceLoader;
    
    public CachedDataStoreFactoryInitializer() {
        super( CachedDataStoreFactory.class );
    }
    
    public void setResourceLoader(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    
    public void initialize(CachedDataStoreFactory factory) {
        
//        factory.setBaseDirectory( resourceLoader.getBaseDirectory() );
    }
}
