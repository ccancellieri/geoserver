package org.geoserver.web.data.store.cache;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.Transformer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geotools.data.DataAccess;
import org.geotools.data.DataAccessFactory;
import org.geotools.data.cache.datastore.CachedDataStore;
import org.geotools.data.cache.datastore.CachedDataStoreFactory;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.vfny.geoserver.util.DataStoreUtils;

public class CachedDataStoreEditPanel extends StoreEditPanel {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.web.data.store.cache");

    // this object is a PropertyModel

    // source attribute to track the source datastore name
    private String source;

    // cache attribute to track the cache datastore name
    private String cache;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCache() {
        return cache;
    }

    public void setCache(String cache) {
        this.cache = cache;
    }

    public CachedDataStoreEditPanel(final String componentId, final Form storeEditForm) {
        super(componentId, storeEditForm);
        setDefaultModel(this.getDefaultModel());
        this.setOutputMarkupId(true);

        final List<String> stores = new ArrayList<String>(CachedDataStoreFactory.getAvailableDataStores().keySet());
        source = cache = stores.get(0);

        final DataStoreInfo storeInfo = (DataStoreInfo) storeEditForm.getModelObject();
        
        final Map<String, Serializable> existingParameters = storeInfo.getConnectionParameters();

        final Map<String, Serializable> sourceParameters = CachedDataStoreFactory.extractParams(
                storeInfo.getConnectionParameters(), CachedDataStoreFactory.SOURCE_PREFIX);

//        DataStore sourceDS = (DataStore) getStore(sourceParameters, source);
        DataStoreInfo sourceDS = getDataStoreInfo(sourceParameters, getCatalog());

        final Map<String, Serializable> cacheParameters = CachedDataStoreFactory.extractParams(
                storeInfo.getConnectionParameters(), CachedDataStoreFactory.CACHE_PREFIX);

        DataStoreInfo cacheDS = getDataStoreInfo(cacheParameters, getCatalog());

        // final IModel paramsModel = new PropertyModel(model, "connectionParameters");

        // ////////////////////////////////////////////////////////////////////////////
        // Adding the store property
        // ////////////////////////////////////////////////////////////////////////////
        // final Panel sourcePanel = new StorePanel("sourcePanel");
        // sourcePanel.setOutputMarkupId(true);
        // add(sourcePanel);
        final DropDownChoice<String> sourceDD = new DropDownChoice<String>("sourceDD",
                new PropertyModel<String>(this, "source"), new DetachableList(), new MapRenderer());
        updateDataStorePanel(this, storeEditForm, sourceDS, source, "sourcePanel");
        sourceDD.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateDataStorePanel(CachedDataStoreEditPanel.this, storeEditForm, null, source,
                        "sourcePanel");
                if (target != null) {
                    target.addComponent(storeEditForm);
                }
            }
        });
        add(sourceDD);

        // ////////////////////////////////////////////////////////////////////////////
        // Adding the cache property
        // ////////////////////////////////////////////////////////////////////////////
        // final Panel cachePanel = new StorePanel("cachePanel");
        // cachePanel.setOutputMarkupId(true);
        // add(cachePanel);
        final DropDownChoice<String> cacheDD = new DropDownChoice<String>("cacheDD",
                new PropertyModel<String>(this, "cache"), new DetachableList(), new MapRenderer());
        updateDataStorePanel(this, storeEditForm, cacheDS, cache, "cachePanel");
        cacheDD.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateDataStorePanel(CachedDataStoreEditPanel.this, storeEditForm, null, cache,
                        "cachePanel");
                if (target != null) {
                    target.addComponent(CachedDataStoreEditPanel.this);
                }
            }
        });
        add(cacheDD);

    }

    private static DataStoreInfo getDataStoreInfo(
            final Map<String, Serializable> existingParameters, final Catalog catalog) {

        DataStoreInfo ds = catalog.getFactory().createDataStore();
        ds.getConnectionParameters().putAll(existingParameters);

        return ds;
    }

    private static final class StorePanel extends Panel {
        // private StorePanel() {
        // super();
        // }
        public StorePanel(String id) {
            super(id);
        }

        /** serialVersionUID */
        private static final long serialVersionUID = -1102744424603396285L;
    }

    private static final class DetachableList extends LoadableDetachableModel<List<String>> {
        /** serialVersionUID */
        private static final long serialVersionUID = 1L;

        @Override
        protected List<String> load() {
            return new ArrayList<String>(CachedDataStoreFactory.getAvailableDataStores().keySet());
        }
    }

    private static final class MapRenderer implements IChoiceRenderer<String> {
        /** serialVersionUID */
        private static final long serialVersionUID = 7513288691725336549L;

        @Override
        public Object getDisplayValue(String object) {
            return object;
        }

        @Override
        public String getIdValue(String object, int index) {
            return object;
        }
    }

    private static void updateDataStorePanel(final Panel parent, final Form storeEditForm,
            DataStoreInfo info, String store, String name) {

        GeoServerApplication app = (GeoServerApplication) parent.getApplication();
        final DataAccessFactory storeFactory = CachedDataStoreFactory.getAvailableDataStores().get(store);

        // if we are creating the datastore
        if (info == null) {
            info = app.getCatalog().getFactory().createDataStore();
        }
        info.getConnectionParameters();
        // info.setWorkspace(defaultWs);
        // info.setEnabled(true);
        info.setType(store);
        Panel storeEditPanel = DataStoreExtensionPoints.getStoreEditPanel(name, new Form(
                storeEditForm.getId(), new CompoundPropertyModel<DataStoreInfo>(info)),
                storeFactory, app);
        parent.addOrReplace(storeEditPanel);
        storeEditPanel.setVisible(true);
    }

    

}
