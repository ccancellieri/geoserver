/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.cache;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;

import org.apache.wicket.markup.html.form.Form;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.resource.DataStorePanelInfo;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.StoreExtensionPoints;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.data.DataAccessFactory;

/**
 * Personal implementation of the {@link StoreExtensionPoints} class
 * 
 * @author Carlo Cancellieri - GeoSolutions SAS
 * 
 */
public abstract class DataStoreExtensionPoints {

    private DataStoreExtensionPoints() {
        // do nothing
    }

    /**
     * Finds out the {@link StoreEditPanel} that provides the edit form components for the given store.
     * 
     * @param componentId the id for the returned panel
     * @param editForm the form that's going to contain the components in the returned panel
     * @param storeInfo the store being edited
     * @param app the {@link GeoServerApplication} where to look for registered {@link DataStorePanelInfo}s
     * @return a custom {@link StoreEditPanel} if there's one declared for the given store type, or a default one otherwise
     */
    public static StoreEditPanel getStoreEditPanel(final String componentId, final Form editForm,
            final DataAccessFactory storeFactory, final GeoServerApplication app) {

        if (storeFactory == null) {
            throw new NullPointerException("storeFactory param");
        }
        if (app == null) {
            throw new NullPointerException("GeoServerApplication param");
        }

        DataStorePanelInfo panelInfo = findPanelInfo(storeFactory, app);
        if (panelInfo == null || panelInfo.getComponentClass() == null) {
            // there's either no panel info specific for this kind of store, or it provides no
            // component class
            panelInfo = getDefaultPanelInfo(storeFactory, app);
        }
        final Class<StoreEditPanel> componentClass = panelInfo.getComponentClass();

        final Constructor<StoreEditPanel> constructor;
        try {
            constructor = componentClass.getConstructor(String.class, Form.class);
        } catch (SecurityException e) {
            throw e;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(componentClass.getName()
                    + " does not provide the required constructor");
        }

        final StoreEditPanel storeEditPanel;
        try {
            storeEditPanel = constructor.newInstance(componentId, editForm);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Cannot instantiate extension point contributor "
                    + componentClass.getName(), e);
        }

        return storeEditPanel;
    }

    private static DataStorePanelInfo getDefaultPanelInfo(final DataAccessFactory storeFactory,
            GeoServerApplication app) {

        final List<DataStorePanelInfo> providers = app.getBeansOfType(DataStorePanelInfo.class);

        DataStorePanelInfo panelInfo = null;

        for (DataStorePanelInfo provider : providers) {
            if ("defaultVector".equals(provider.getId())) {
                panelInfo = provider;
                break;
            }
        }

        if (panelInfo.getComponentClass() == null) {
            throw new IllegalStateException("Default DataStorePanelInfo '" + panelInfo.getId()
                    + "' does not define a componentClass property");
        }

        if (panelInfo.getIconBase() == null || panelInfo.getIcon() == null) {
            throw new IllegalStateException("Default DataStorePanelInfo '" + panelInfo.getId()
                    + "' does not define default icon");
        }

        return panelInfo;
    }

    /**
     * 
     * @param storeInfo
     * @param app
     * @return the extension point descriptor for the given storeInfo, or {@code null} if there's no contribution specific for the given storeInfo's
     *         type
     */
    private static DataStorePanelInfo findPanelInfo(final DataAccessFactory storeFactory,
            final GeoServerApplication app) {

        Class<?> factoryClass = null;

        if (storeFactory != null) {
            factoryClass = storeFactory.getClass();
        } else {
            throw new IllegalArgumentException("storeFactory is null");
        }

        if (factoryClass == null) {
            throw new IllegalArgumentException("Can't locate the factory for the store");
        }

        final List<DataStorePanelInfo> providers = app.getBeansOfType(DataStorePanelInfo.class);

        DataStorePanelInfo panelInfo = null;

        for (DataStorePanelInfo provider : providers) {
            Class<?> providerFactoryClass = provider.getFactoryClass();
            if (factoryClass.equals(providerFactoryClass)) {
                panelInfo = provider;
                break;
            }
        }

        return panelInfo;
    }

}
