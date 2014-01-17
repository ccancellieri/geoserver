package org.geoserver.web.data.store.cache;

import java.util.Map;

import org.apache.wicket.model.IModel;
import org.geotools.data.cache.op.CachedOpSPI;
import org.geotools.data.cache.utils.CacheUtils;

public  class MapModel implements IModel<Class<CachedOpSPI<?>>> {

    private final Map<String, CachedOpSPI<?>> cacheStatusMap;
    private final String opName;
    
    public MapModel(Map<String, CachedOpSPI<?>> cacheStatusMap, String opName) {
        this.opName=opName;
        this.cacheStatusMap=cacheStatusMap;
    }

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    @Override
    public void detach() {
        // TODO Auto-generated method stub
    }

    @Override
    public Class<CachedOpSPI<?>> getObject() {
        CachedOpSPI<?> obj=cacheStatusMap.get(opName);
        if (obj!=null)
            return (Class<CachedOpSPI<?>>) obj.getClass();
        else
            return null;
    }

    @Override
    public void setObject(Class<CachedOpSPI<?>> object) {
        if (object==null){
            cacheStatusMap.put(opName, null);
        } else {
            try {
                cacheStatusMap.put(opName, (CachedOpSPI<?>) Class.forName(object.getName()).newInstance());
            } catch (InstantiationException e) {
                throw new IllegalArgumentException(e);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

}
