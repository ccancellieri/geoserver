package org.geoserver.web.data.store.cache;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.DefaultCatalogFacade;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.store.ParamInfo;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.util.MapModel;
import org.geotools.data.DataAccessFactory;
import org.geotools.data.DataAccessFactory.Param;
import org.geotools.data.cache.datastore.CacheManager;
import org.geotools.data.cache.datastore.CachedDataStore;
import org.geotools.data.cache.datastore.CachedDataStoreFactory;
import org.geotools.data.cache.op.CachedOp;
import org.geotools.data.cache.op.CachedOpSPI;
import org.geotools.data.cache.op.CachedOpStatus;
import org.geotools.data.cache.op.Operation;
import org.geotools.data.cache.utils.CacheUtils;
import org.geotools.data.cache.utils.CachedOpSPIMapParam;
import org.geotools.data.cache.utils.MapParam;
import org.geotools.util.logging.Logging;

public class CachedDataStoreEditPanel extends StoreEditPanel {

    /** serialVersionUID */
    private static final long serialVersionUID = 14714866663069357L;

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.web.data.store.cache");

    private Map<String, CachedOpSPI<CachedOpStatus<Object>, CachedOp<Object, Object>, Object, Object>> cacheStatusMap;

    private Form<DataStoreInfo> sourceForm;

    private Form<DataStoreInfo> cacheForm;

    private final DataStoreInfo storeInfo;

    private final Map<String, Serializable> params;

    public CachedDataStoreEditPanel(final String componentId,
            final Form<DataStoreInfo> storeEditForm) throws InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        super(componentId, storeEditForm);
        setDefaultModel(this.getDefaultModel());
        this.setOutputMarkupId(true);

        storeInfo = (DataStoreInfo) storeEditForm.getModelObject();
        params = storeInfo.getConnectionParameters();

        final List<String> stores = new ArrayList<String>(CachedDataStoreFactory
                .getAvailableDataStores().keySet());

        // ////////////////////////////////////////////////////////////////////////////
        // Adding the source panel
        // ////////////////////////////////////////////////////////////////////////////
        sourceForm = appendStoreDDSelector("sourcePanel", CachedDataStoreFactory.SOURCE_TYPE_KEY,
                CachedDataStoreFactory.SOURCE_PARAMS_KEY, stores);

        // create the drop down to select the desired store
        final DropDownChoice<String> sourceDD = new DropDownChoice<String>(
                CachedDataStoreFactory.SOURCE_TYPE_KEY, new MapModel(params,
                        CachedDataStoreFactory.SOURCE_TYPE_KEY), new DetachableList(),
                new MapRenderer());
        sourceDD.add(new OnChangeAjaxBehavior() {
            /** serialVersionUID */
            private static final long serialVersionUID = 1323333739138238355L;

            /**
             * onUpdate:<br/>
             * load a new datastore<br/>
             * generate the panel<br/>
             * add this to redraw<br/>
             */
            @Override
            protected void onUpdate(AjaxRequestTarget target) {

                // clear any previous store status
                clear();

                sourceForm = updateDataStorePanel(CachedDataStoreEditPanel.this, storeEditForm,
                        null, (String) params.get(CachedDataStoreFactory.SOURCE_TYPE_KEY),
                        "sourcePanel");
                if (target != null) {
                    target.addComponent(CachedDataStoreEditPanel.this);
                }
            }
        });
        // add the storeDrop down to this panel
        add(sourceDD);

        // ////////////////////////////////////////////////////////////////////////////
        // Adding the cache panel
        // ////////////////////////////////////////////////////////////////////////////
        cacheForm = appendStoreDDSelector("cachePanel", CachedDataStoreFactory.CACHE_TYPE_KEY,
                CachedDataStoreFactory.CACHE_PARAMS_KEY, stores);

        // create the drop down to select the desired store
        final DropDownChoice<String> cacheDD = new DropDownChoice<String>(
                CachedDataStoreFactory.CACHE_TYPE_KEY, new MapModel(params,
                        CachedDataStoreFactory.CACHE_TYPE_KEY), new DetachableList(),
                new MapRenderer());
        cacheDD.add(new OnChangeAjaxBehavior() {
            /** serialVersionUID */
            private static final long serialVersionUID = -7680695733844795529L;

            /**
             * onUpdate:<br/>
             * load a new datastore<br/>
             * generate the panel<br/>
             * add this to redraw<br/>
             */
            @Override
            protected void onUpdate(AjaxRequestTarget target) {

                // clear any previous store status
                clear();

                cacheForm = updateDataStorePanel(CachedDataStoreEditPanel.this, storeEditForm,
                        null, (String) params.get(CachedDataStoreFactory.CACHE_TYPE_KEY),
                        "cachePanel");
                if (target != null) {
                    target.addComponent(CachedDataStoreEditPanel.this);// cacheForm);//CachedDataStoreEditPanel.this.get("cachePanel"));
                }
            }
        });
        // add the storeDrop down to this panel
        add(cacheDD);

        // ////////////////////////////////////////////////////////////////////////////
        // Adding the CachedOpSPI form selector
        // ////////////////////////////////////////////////////////////////////////////
        appendCachedOpMenu(this);
    }

    private void clear() {

        // getCatalog().getStore(getId(), CachedDataStore.class);
//        DataStoreInfo info = getCatalog().getStore(getId(), DataStoreInfo.class);
        if (storeInfo != null) {
            CachedDataStore ds = null;
            try {
                // CachedDataStoreFactory f = new CachedDataStoreFactory();
                // ds = (CachedDataStore) f.createNewDataStore(params);
                ds = (CachedDataStore) storeInfo.getDataStore(null);

                if (ds != null) {
                    ds.clear();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            } finally {
                if (ds != null) {
                    ds.dispose();
                }
            }
        }
    }

    /**
     * @param panelName the name of the generated panel
     * @param argsStoreTypeKey the key to locate into the params map the type of the store
     * @param argsParamsNameKey the key to locate into the params map the map of parameters to configure the selected store
     * @param stores the list of available stores
     */
    private Form<DataStoreInfo> appendStoreDDSelector(final String panelName,
            final String argsStoreTypeKey, String argsParamsNameKey, final List<String> stores) {

        // get the firs store as default
        if (params.get(argsStoreTypeKey) == null) {
            params.put(argsStoreTypeKey, stores.get(0));
        }

        final Object cacheParamObj = params.get(argsParamsNameKey);
        Map<String, Serializable> cacheParams = null;
        if (cacheParamObj != null) {
            if (cacheParamObj instanceof Map) {
                cacheParams = (Map<String, Serializable>) cacheParamObj;
            } else /* if (paramObj instanceof String) */{
                cacheParams = MapParam.parseMap((String) cacheParamObj);
            }
        }
        // create a new datastore info using extracted params (or null if no one is found)
        final DataStoreInfo dataStore = getDataStoreInfo(cacheParams,
                (String) params.get(argsStoreTypeKey), getCatalog());

        // create a form using that store (it is embedded in a new panel which is added to this instance itself)
        return updateDataStorePanel(this, storeEditForm, dataStore,
                (String) params.get(argsStoreTypeKey), panelName);

    }

    private void appendCachedOpMenu(Panel parent) throws InstantiationException,
            IllegalAccessException, ClassNotFoundException {

        final Object cacheStatusMapObj = params.get(CachedDataStoreFactory.CACHEDOPSPI_PARAMS_KEY);
        if (cacheStatusMapObj != null) {
            if (cacheStatusMapObj instanceof Map) {
                cacheStatusMap = (Map<String, CachedOpSPI<CachedOpStatus<Object>, CachedOp<Object, Object>, Object, Object>>) cacheStatusMapObj;
            } else /* if (paramObj instanceof String) */{
                cacheStatusMap = CachedOpSPIMapParam.parseSPIMap((String) cacheStatusMapObj);
            }
        } else {
            cacheStatusMap = new HashMap<String, CachedOpSPI<CachedOpStatus<Object>, CachedOp<Object, Object>, Object, Object>>();
        }

        // append clear button
        parent.add(new AjaxButton("clear") {

            /** serialVersionUID */
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                clear();
            }
        });

        // append operations
        final CacheUtils cu = CacheUtils.getCacheUtils();
        for (Operation op : Operation.values()) {
            final List<Class<CachedOpSPI<CachedOpStatus<?>, CachedOp<?, ?>, ?, ?>>> listOfOp = new ArrayList<Class<CachedOpSPI<CachedOpStatus<?>, CachedOp<?, ?>, ?, ?>>>();
            for (CachedOpSPI<?, ?, ?, ?> spi : cu.getCachedOps()) {
                if (op.equals(spi.getOp())) {
                    listOfOp.add((Class<CachedOpSPI<CachedOpStatus<?>, CachedOp<?, ?>, ?, ?>>) spi
                            .getClass());
                }
            }
            final DropDownChoice<Class<CachedOpSPI<CachedOpStatus<?>, CachedOp<?, ?>, ?, ?>>> cachedOpDD = new DropDownChoice<Class<CachedOpSPI<CachedOpStatus<?>, CachedOp<?, ?>, ?, ?>>>(
                    op.toString(),
                    new org.geoserver.web.data.store.cache.MapModel(cacheStatusMap, op.toString()),
                    listOfOp,
                    new IChoiceRenderer<Class<CachedOpSPI<CachedOpStatus<?>, CachedOp<?, ?>, ?, ?>>>() {

                        /** serialVersionUID */
                        private static final long serialVersionUID = 1164860377053981031L;

                        @Override
                        public Object getDisplayValue(
                                Class<CachedOpSPI<CachedOpStatus<?>, CachedOp<?, ?>, ?, ?>> object) {
                            return object;
                        }

                        @Override
                        public String getIdValue(
                                Class<CachedOpSPI<CachedOpStatus<?>, CachedOp<?, ?>, ?, ?>> object,
                                int index) {
                            return object.getName();
                        }

                    });
            cachedOpDD.setNullValid(true);
            cachedOpDD.add(new OnChangeAjaxBehavior() {
                /** serialVersionUID */
                private static final long serialVersionUID = -4965892267714452997L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    // clear any previous store status
                    clear();
                }
            });
            parent.add(cachedOpDD);
        }
    }

    /**
     * note that some parts of this implementation is taken from: {@link StoreEditPanel#applyDataStoreParamsDefaults(StoreInfo)}
     * 
     * the namespace part is needed to avoid the following exception on workspace/namespace Caused by: java.lang.NullPointerException at
     * {@link DefaultCatalogFacade#getNamespaceByURI(DefaultCatalogFacade)}
     * 
     * @param existingParameters
     * @param type
     * @param catalog
     * @return
     */
    private static DataStoreInfo getDataStoreInfo(
            final Map<String, Serializable> existingParameters, final String type,
            final Catalog catalog) {

        final CatalogFactory factory = catalog.getFactory();

        final DataStoreInfo ds = factory.createDataStore();

        final DataStoreInfoImpl dsInfo = (DataStoreInfoImpl) ds;
        if (dsInfo.getId() == null) {
            dsInfo.setId("id");
        }
        if (type != null) {
            dsInfo.setType(type);
        }
        if (existingParameters != null) {
            dsInfo.getConnectionParameters().putAll(existingParameters);
        } else {
            final DataAccessFactory dsFactory;
            try {
                dsFactory = catalog.getResourcePool().getDataStoreFactory(dsInfo);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            final Param[] dsParams = dsFactory.getParametersInfo();
            for (Param p : dsParams) {
                ParamInfo paramInfo = new ParamInfo(p);
                applyParamDefault(catalog, paramInfo, dsInfo);
            }
        }
        return ds;
    }

    /**
     * {@link StoreEditPanel#applyDataStoreParamsDefaults(StoreInfo)}
     * 
     * @param catalog
     * @param paramInfo
     * @param info
     */
    private static void applyParamDefault(Catalog catalog, ParamInfo paramInfo, StoreInfo info) {
        Serializable defValue = paramInfo.getValue();
        if ("namespace".equals(paramInfo.getName())) {
            defValue = catalog.getDefaultNamespace().getURI();
        } else if (URL.class == paramInfo.getBinding() && null == defValue) {
            defValue = "file:data/example.extension";
        } else {
            defValue = paramInfo.getValue();
        }
        info.getConnectionParameters().put(paramInfo.getName(), defValue);
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

    private Form<DataStoreInfo> updateDataStorePanel(final Panel parent,
            final Form<?> storeEditForm, DataStoreInfo info, String store, String panelName) {

        GeoServerApplication app = (GeoServerApplication) parent.getApplication();
        final DataAccessFactory storeFactory = CachedDataStoreFactory.getAvailableDataStores().get(
                store);

        // if we are creating the datastore
        if (info == null) {
            // storeFactory.createDataStore(params);
            // info = new DataStoreInfoImpl(app.getCatalog(), null);
            // we have to override the normal factory mechanism to use the 2 arguments constructor
            // this is done to initialize the info ID to avoid overwrite passed params with the default ones.
            info = getDataStoreInfo(null, store, app.getCatalog());
            // app.getCatalog().getFactory().createDataStore();
            // look at
            // org.geoserver.web.data.store.DefaultDataStoreEditPanel.DefaultDataStoreEditPanel(String, Form)
            // set the type of the desired store
            // info.setType(store);
            // ((DataStoreInfoImpl) info).setId("id");
            // info.setWorkspace(storeInfo.getWorkspace());
        }

        // info.setEnabled(true);
        // Form storeForm = new Form(storeEditForm.getId(), new CompoundPropertyModel<DataStoreInfo>(
        // info));

        final Form<DataStoreInfo> storeForm = new Form<DataStoreInfo>(store,
                new CompoundPropertyModel<DataStoreInfo>(info));
        storeForm.setOutputMarkupId(true);
        final Panel storeEditPanel = DataStoreExtensionPoints.getStoreEditPanel(panelName,
                storeForm, storeFactory, app);
        parent.addOrReplace(storeEditPanel);
        storeEditPanel.setOutputMarkupId(true);
        // storeEditPanel.setVisible(true);

        return storeForm;
    }

    /**
     * Gives an option to store panels to raise an opinion before saving
     * 
     * @return
     */
    @Override
    public boolean onSave() {

        params.put(CachedDataStoreFactory.SOURCE_PARAMS_KEY,
                CacheUtils.toText(sourceForm.getModelObject().getConnectionParameters()));

        params.put(CachedDataStoreFactory.CACHE_PARAMS_KEY,
                CacheUtils.toText(cacheForm.getModelObject().getConnectionParameters()));

        // String oldName = (String) params.get(CachedDataStoreFactory.NAME_KEY);
        String name = (String) this.storeInfo.getName();
        // if (!oldName.isEmpty() && !oldName.equals(name)) {
        // clear();
        // }
        params.put(CachedDataStoreFactory.NAME_KEY, (Serializable) name);

        // String oldNameSpace = (String) params.get(CachedDataStoreFactory.NAMESPACE_KEY);
        String nameSpace = (String) this.storeInfo.getWorkspace().getName();
        // if (!oldNameSpace.isEmpty() && !oldNameSpace.equals(nameSpace)) {
        // clear();
        // }
        params.put(CachedDataStoreFactory.NAMESPACE_KEY, (Serializable) nameSpace);

        params.put(CachedDataStoreFactory.CACHEDOPSPI_PARAMS_KEY,
                (Serializable) CachedOpSPIMapParam.toText(cacheStatusMap));

        // cacheParameters
        return true;
    }
}
