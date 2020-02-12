/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.catalog;

import com.thoughtworks.xstream.XStream;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.cluster.events.ToggleSwitch;

/**
 * Handler for CatalogAddEvent.
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class JMSCatalogAddEventHandler extends JMSCatalogEventHandler {
    private final Catalog catalog;
    private final ToggleSwitch producer;
    private final CatalogUtils catalogUtils = CatalogUtils.creating();

    public JMSCatalogAddEventHandler(
            Catalog catalog, XStream xstream, Class clazz, ToggleSwitch producer) {
        super(xstream, clazz);
        this.catalog = catalog;
        this.producer = producer;
    }

    @Override
    public boolean synchronize(CatalogEvent event) throws Exception {
        if (event == null) {
            throw new IllegalArgumentException("Incoming object is null");
        }
        try {
            if (event instanceof CatalogAddEvent) {
                final CatalogAddEvent addEv = ((CatalogAddEvent) event);

                // get the source from the incoming event
                final CatalogInfo info = addEv.getSource();
                // disable the producer to avoid recursion
                producer.disable();

                // add the incoming CatalogInfo to the local catalog
                this.add(catalog, info);
            } else {
                // incoming object not recognized
                if (LOGGER.isLoggable(java.util.logging.Level.SEVERE))
                    LOGGER.severe("Unrecognized event type");
                return false;
            }
        } finally {
            // re enable the producer
            producer.enable();
        }
        return true;
    }

    private void add(final Catalog catalog, CatalogInfo info)
            throws IllegalAccessException, InvocationTargetException {

        if (info instanceof LayerGroupInfo) {

            final LayerGroupInfo deserObject =
                    catalogUtils.localizePublishedInfo((LayerGroupInfo) info, catalog);
            catalog.add(ModificationProxy.unwrap(deserObject));

        } else if (info instanceof LayerInfo) {

            final LayerInfo layer = catalogUtils.localizePublishedInfo((LayerInfo) info, catalog);
            catalog.add(ModificationProxy.unwrap(layer));

        } else if (info instanceof MapInfo) {

            final MapInfo localObject = catalogUtils.localizeMapInfo((MapInfo) info, catalog);
            catalog.add(ModificationProxy.unwrap(localObject));

        } else if (info instanceof NamespaceInfo) {

            final NamespaceInfo namespace =
                    catalogUtils.localizeNamespace((NamespaceInfo) info, catalog);
            catalog.add(ModificationProxy.unwrap(namespace));

        } else if (info instanceof StoreInfo) {

            StoreInfo store = catalogUtils.localizeStore((StoreInfo) info, catalog);
            catalog.add(ModificationProxy.unwrap(store));

        } else if (info instanceof ResourceInfo) {

            final ResourceInfo resource =
                    catalogUtils.localizeResource((ResourceInfo) info, catalog);
            catalog.add(ModificationProxy.unwrap(resource));

        } else if (info instanceof StyleInfo) {

            final StyleInfo deserializedObject =
                    catalogUtils.localizeStyle((StyleInfo) info, catalog);
            catalog.add(ModificationProxy.unwrap(deserializedObject));

        } else if (info instanceof WorkspaceInfo) {

            final WorkspaceInfo workspace =
                    catalogUtils.localizeWorkspace((WorkspaceInfo) info, catalog);
            catalog.add(ModificationProxy.unwrap(workspace));

        } else if (info instanceof Catalog) {
            // TODO may we don't want to send this empty message!
            // TODO check the producer
            // DO NOTHING
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.severe("info - ID: " + info.getId() + " toString: " + info.toString());
            }
        } else {
            throw new IllegalArgumentException("Bad incoming object: " + info.getClass());
        }
    }
}
