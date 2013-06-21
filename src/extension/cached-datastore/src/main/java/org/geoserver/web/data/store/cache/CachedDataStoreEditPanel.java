package org.geoserver.web.data.store.cache;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.store.ParamInfo;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.util.MapModel;
import org.geotools.data.DataAccessFactory;
import org.geotools.data.DataAccessFactory.Param;
import org.geotools.data.cache.datastore.CachedDataStoreFactory;
import org.geotools.data.cache.op.CacheStatus;
import org.geotools.data.cache.op.CachedOpSPI;
import org.geotools.data.cache.op.Operation;
import org.geotools.data.cache.op.SchemaOpSPI;
import org.geotools.data.cache.utils.CacheUtils;
import org.geotools.util.logging.Logging;

public class CachedDataStoreEditPanel extends StoreEditPanel {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.web.data.store.cache");

    private Form sourceForm;

    private Form cacheForm;

    private final DataStoreInfo storeInfo;

    final Map<String, Serializable> params;

    public CachedDataStoreEditPanel(final String componentId, final Form storeEditForm) {
        super(componentId, storeEditForm);
        setDefaultModel(this.getDefaultModel());
        this.setOutputMarkupId(true);

        storeInfo = (DataStoreInfo) storeEditForm.getModelObject();
        params = storeInfo.getConnectionParameters();
        
        appendCachedOpMenu(this);

        // ////////////////////////////////////////////////////////////////////////////
        // Adding the store property
        // ////////////////////////////////////////////////////////////////////////////
        final List<String> stores = new ArrayList<String>(CachedDataStoreFactory
                .getAvailableDataStores().keySet());
        if (params.get(CachedDataStoreFactory.SOURCE_TYPE_KEY) == null) {
            params.put(CachedDataStoreFactory.SOURCE_TYPE_KEY, stores.get(0));
        }

        final DropDownChoice<String> sourceDD = new DropDownChoice<String>(
                CachedDataStoreFactory.SOURCE_TYPE_KEY, new MapModel(params,
                        CachedDataStoreFactory.SOURCE_TYPE_KEY), new DetachableList(),
                new MapRenderer());

        final DataStoreInfo sourceDS = getDataStoreInfo(null,
                (String) params.get(CachedDataStoreFactory.SOURCE_TYPE_KEY), getCatalog());

        final Object sourceParamObj = params.get(CachedDataStoreFactory.SOURCE_PARAMS_KEY);
        Map<String, Serializable> sourceParams = null;
        if (sourceParamObj != null) {
            if (sourceParamObj instanceof Map) {
                sourceParams = (Map<String, Serializable>) sourceParamObj;
            } else /* if (paramObj instanceof String) */{
                sourceParams = parseMap((String) sourceParamObj, sourceDS);
            }
        }

        // sourceDS.getConnectionParameters().putAll(sourceParams);

        sourceForm = updateDataStorePanel(this, storeEditForm, sourceDS,
                (String) params.get(CachedDataStoreFactory.SOURCE_TYPE_KEY), "sourcePanel");
        sourceDD.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                sourceForm = updateDataStorePanel(CachedDataStoreEditPanel.this, storeEditForm,
                        null, (String) params.get(CachedDataStoreFactory.SOURCE_TYPE_KEY),
                        "sourcePanel");
                if (target != null) {
                    target.addComponent(CachedDataStoreEditPanel.this);
                }
            }
        });
        add(sourceDD);

        // ////////////////////////////////////////////////////////////////////////////
        // Adding the cache property
        // ////////////////////////////////////////////////////////////////////////////

        if (params.get(CachedDataStoreFactory.CACHE_TYPE_KEY) == null) {
            params.put(CachedDataStoreFactory.CACHE_TYPE_KEY, stores.get(0));
        }
        // final Map<String, Serializable> cacheParameters = CachedDataStoreFactory.extractParams(
        // storeInfo.getConnectionParameters(), CachedDataStoreFactory.CACHE_PREFIX);

        final DropDownChoice<String> cacheDD = new DropDownChoice<String>(
                CachedDataStoreFactory.CACHE_TYPE_KEY, new MapModel(params,
                        CachedDataStoreFactory.CACHE_TYPE_KEY), new DetachableList(),
                new MapRenderer());

        final DataStoreInfo cacheDS = getDataStoreInfo(null,
                (String) params.get(CachedDataStoreFactory.CACHE_TYPE_KEY), getCatalog());

        final Object cacheParamObj = params.get(CachedDataStoreFactory.CACHE_PARAMS_KEY);
        Map<String, Serializable> cacheParams = null;
        if (cacheParamObj != null) {
            if (cacheParamObj instanceof Map) {
                cacheParams = (Map<String, Serializable>) cacheParamObj;
            } else /* if (paramObj instanceof String) */{
                cacheParams = parseMap((String) cacheParamObj, cacheDS);
            }
        }
        // cacheDS.getConnectionParameters().putAll(cacheParams);

        cacheForm = updateDataStorePanel(this, storeEditForm, cacheDS,
                (String) params.get(CachedDataStoreFactory.CACHE_TYPE_KEY), "cachePanel");
        cacheDD.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                cacheForm = updateDataStorePanel(CachedDataStoreEditPanel.this, storeEditForm,
                        null, (String) params.get(CachedDataStoreFactory.CACHE_TYPE_KEY),
                        "cachePanel");
                if (target != null) {
                    target.addComponent(CachedDataStoreEditPanel.this);// cacheForm);//CachedDataStoreEditPanel.this.get("cachePanel"));
                }
            }
        });
        add(cacheDD);
        
    }
    
    private void appendCachedOpMenu(Panel parent){

        final Map<String, CachedOpSPI<?>> cacheStatusMap = new HashMap<String, CachedOpSPI<?>>();
        CacheStatus status = null;
        try {
            status = new CacheStatus(CachedDataStoreFactory.getDataStoreUID(params));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        Set<CachedOpSPI<?>> keys;
        if (status != null)
            keys = status.getCachedOpKeys();
        else
            keys = Collections.EMPTY_SET;

        for (Operation op : Operation.values()) {
            // CachedOp<>
            boolean found = false;
            for (CachedOpSPI<?> spi : keys) {
                if (spi.getOp().equals(op)) {
                    found = true;
                    cacheStatusMap.put(op.toString(), spi);
                }
            }
            if (!found) {
                if (op.equals(Operation.schema)) {
                    // special case
                    cacheStatusMap.put(op.toString(), new SchemaOpSPI());
                } else {
                    cacheStatusMap.put(op.toString(), null);// new NoCachedOpSPI());
                }
            }
        }

        final CacheUtils cu = CacheUtils.getCacheUtils();
        for (Operation op : Operation.values()) {
            List<String> listOfOp = new ArrayList<String>();
            listOfOp.add("NONE");
            for (CachedOpSPI<?> spi : cu.getCachedOps()) {
                if (op.equals(spi.getOp())) {
                    listOfOp.add(spi.getClass().toString());
                }
            }
            final DropDownChoice<String> cachedOpDD = new DropDownChoice<String>(op.toString(),
                    new MapModel(cacheStatusMap, op.toString()), listOfOp, new IChoiceRenderer() {

                        @Override
                        public Object getDisplayValue(Object object) {
                            return object;
                        }

                        @Override
                        public String getIdValue(Object object, int index) {
                            if (object != null)
                                return object.toString();
                            else
                                return "NONE";
                        }

                    });

            parent.add(cachedOpDD);
        }
    }

    private Map<String, Serializable> parseMap(String input, StoreInfo info) {
        // final Map<String, Serializable> map = new HashMap<String, Serializable>();
        input = input.substring(1, input.length() - 1);
        for (String pair : input.split(",")) {
            String[] kv = pair.split("=");
            ParamInfo pInfo = new ParamInfo(new Param(kv[0].trim()));
            pInfo.setValue(kv[1].trim());
            this.applyParamDefault(pInfo, info);
            // map.put(kv[0], kv[1]);
        }
        return info.getConnectionParameters();
    }

    private static DataStoreInfo getDataStoreInfo(
            final Map<String, Serializable> existingParameters, final String type,
            final Catalog catalog) {

        final DataStoreInfo ds = catalog.getFactory().createDataStore();
        if (type != null) {
            ds.setType(type);
        }
        if (existingParameters != null) {
            ds.getConnectionParameters().putAll(existingParameters);
        }
        return ds;
    }

    private static final class DetachableList extends LoadableDetachableModel<List<String>> {
        /** serialVersionUID */
        private static final long serialVersionUID = 1L;

        @Override
        protected List<String> load() {
            return new ArrayList<String>(CachedDataStoreFactory.getAvailableDataStores().keySet());
        }
    }

    private static final class MapRenderer implements IChoiceRenderer<Object> {
        /** serialVersionUID */
        private static final long serialVersionUID = 7513288691725336549L;

        @Override
        public Object getDisplayValue(Object object) {
            return object;
        }

        @Override
        public String getIdValue(Object object, int index) {
            return object.toString();
        }
    }

    private static Form updateDataStorePanel(final Panel parent, final Form storeEditForm,
            DataStoreInfo info, String store, String name) {

        GeoServerApplication app = (GeoServerApplication) parent.getApplication();
        final DataAccessFactory storeFactory = CachedDataStoreFactory.getAvailableDataStores().get(
                store);

        // if we are creating the datastore
        if (info == null) {
            info = app.getCatalog().getFactory().createDataStore();
        }

        // info.setWorkspace(defaultWs);
        // info.setEnabled(true);
        info.setType(store);
        Form storeForm = new Form(storeEditForm.getId(), new CompoundPropertyModel<DataStoreInfo>(
                info));
        storeForm.setOutputMarkupId(true);
        Panel storeEditPanel = DataStoreExtensionPoints.getStoreEditPanel(name, storeForm,
                storeFactory, app);
        parent.addOrReplace(storeEditPanel);
        storeEditPanel.setOutputMarkupId(true);
        storeEditPanel.setVisible(true);

        return storeForm;
    }

    /**
     * Gives an option to store panels to raise an opinion before saving
     * 
     * @return
     */
    @Override
    public boolean onSave() {

        // if ((sourceParameters = (Map<String, Serializable>) params
        // .get(CachedDataStoreFactory.SOURCE_PARAMS_KEY)) == null) {
        // sourceParameters = new HashMap<String, Serializable>();
        // params.put(CachedDataStoreFactory.SOURCE_PARAMS_KEY, (Serializable) sourceParameters);
        // }
        params.put(CachedDataStoreFactory.SOURCE_PARAMS_KEY,
                (Serializable) ((DataStoreInfo) sourceForm.getModelObject())
                        .getConnectionParameters());
        //
        // if ((cacheParameters = (Map<String, Serializable>) params
        // .get(CachedDataStoreFactory.CACHE_PARAMS_KEY)) == null) {
        // cacheParameters = new HashMap<String, Serializable>();
        // params.put(CachedDataStoreFactory.CACHE_PARAMS_KEY, (Serializable) cacheParameters);
        // }
        params.put(CachedDataStoreFactory.CACHE_PARAMS_KEY,
                (Serializable) ((DataStoreInfo) cacheForm.getModelObject())
                        .getConnectionParameters());

        params.put(CachedDataStoreFactory.NAME_KEY,
                (Serializable) ((String) this.storeInfo.getName()));

        params.put(CachedDataStoreFactory.NAMESPACE_KEY, (Serializable) ((String) this.storeInfo
                .getWorkspace().getName()));

        // cacheParameters
        return true;
    }
}
