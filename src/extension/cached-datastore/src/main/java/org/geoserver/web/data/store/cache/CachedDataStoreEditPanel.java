package org.geoserver.web.data.store.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.util.MapModel;
import org.geotools.data.DataAccessFactory;
import org.geotools.data.cache.datastore.CachedDataStoreFactory;
import org.geotools.util.logging.Logging;

public class CachedDataStoreEditPanel extends StoreEditPanel {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.web.data.store.cache");

    Form sourceForm;

    Form cacheForm;

    final Map<String, Serializable> params;

    public CachedDataStoreEditPanel(final String componentId, final Form storeEditForm) {
        super(componentId, storeEditForm);
        setDefaultModel(this.getDefaultModel());
        this.setOutputMarkupId(true);

        final DataStoreInfo storeInfo = (DataStoreInfo) storeEditForm.getModelObject();
        params = storeInfo.getConnectionParameters();

        final List<String> stores = new ArrayList<String>(CachedDataStoreFactory
                .getAvailableDataStores().keySet());

        if (params.get(CachedDataStoreFactory.SOURCE_TYPE_KEY) == null) {
            params.put(CachedDataStoreFactory.SOURCE_TYPE_KEY, stores.get(0));
        }
        if (params.get(CachedDataStoreFactory.CACHE_TYPE_KEY) == null) {
            params.put(CachedDataStoreFactory.CACHE_TYPE_KEY, stores.get(0));
        }

        // final IModel paramsModel = new PropertyModel(model, "connectionParameters");

        // ////////////////////////////////////////////////////////////////////////////
        // Adding the store property
        // ////////////////////////////////////////////////////////////////////////////

        // final Map<String, Serializable> sourceParameters = CachedDataStoreFactory.extractParams(
        // storeInfo.getConnectionParameters(), CachedDataStoreFactory.SOURCE_PREFIX);

        final DropDownChoice<String> sourceDD = new DropDownChoice<String>(
                CachedDataStoreFactory.SOURCE_TYPE_KEY, new MapModel(params,
                        CachedDataStoreFactory.SOURCE_TYPE_KEY), new DetachableList(),
                new MapRenderer());

        final DataStoreInfo sourceDS = getDataStoreInfo(
                (Map<String, Serializable>) params.get(CachedDataStoreFactory.SOURCE_PARAMS_KEY),
                (String) params.get(CachedDataStoreFactory.SOURCE_TYPE_KEY), getCatalog());

        sourceForm = updateDataStorePanel(this, storeEditForm, sourceDS,
                (String) params.get(CachedDataStoreFactory.SOURCE_TYPE_KEY), "sourcePanel");
        sourceDD.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                sourceForm = updateDataStorePanel(CachedDataStoreEditPanel.this, storeEditForm,
                        null, (String) params.get(CachedDataStoreFactory.SOURCE_TYPE_KEY),
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

        // final Map<String, Serializable> cacheParameters = CachedDataStoreFactory.extractParams(
        // storeInfo.getConnectionParameters(), CachedDataStoreFactory.CACHE_PREFIX);

        final DropDownChoice<String> cacheDD = new DropDownChoice<String>(
                CachedDataStoreFactory.CACHE_TYPE_KEY, new MapModel(params,
                        CachedDataStoreFactory.CACHE_TYPE_KEY), new DetachableList(),
                new MapRenderer());

        final DataStoreInfo cacheDS = getDataStoreInfo(
                (Map<String, Serializable>) params.get(CachedDataStoreFactory.CACHE_PARAMS_KEY),
                (String) params.get(CachedDataStoreFactory.CACHE_TYPE_KEY), getCatalog());

        cacheForm = updateDataStorePanel(this, storeEditForm, cacheDS,
                (String) params.get(CachedDataStoreFactory.CACHE_TYPE_KEY), "cachePanel");
        cacheDD.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                cacheForm = updateDataStorePanel(CachedDataStoreEditPanel.this, storeEditForm,
                        null, (String) params.get(CachedDataStoreFactory.CACHE_TYPE_KEY),
                        "cachePanel");
                if (target != null) {
                    target.addComponent(CachedDataStoreEditPanel.this);
                }
            }
        });
        add(cacheDD);

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

    private static final class StorePanel extends Panel {

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
        Panel storeEditPanel = DataStoreExtensionPoints.getStoreEditPanel(name, storeForm,
                storeFactory, app);
        parent.addOrReplace(storeEditPanel);
        storeEditPanel.setVisible(true);
        // storeEditForm.setModel(new CompoundPropertyModel<DataStoreInfo>(info));

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
                (Serializable) ((DataStoreInfo) sourceForm.getModelObject()).getConnectionParameters());
        //
        // if ((cacheParameters = (Map<String, Serializable>) params
        // .get(CachedDataStoreFactory.CACHE_PARAMS_KEY)) == null) {
        // cacheParameters = new HashMap<String, Serializable>();
        // params.put(CachedDataStoreFactory.CACHE_PARAMS_KEY, (Serializable) cacheParameters);
        // }
        params.put(CachedDataStoreFactory.CACHE_PARAMS_KEY,
                (Serializable) ((DataStoreInfo) cacheForm.getModelObject()).getConnectionParameters());

        // final DataStoreInfo storeInfo = (DataStoreInfo) storeEditForm.getModelObject();
        // params = storeInfo.getConnectionParameters();
        // cacheParameters
        return true;
    }

}
