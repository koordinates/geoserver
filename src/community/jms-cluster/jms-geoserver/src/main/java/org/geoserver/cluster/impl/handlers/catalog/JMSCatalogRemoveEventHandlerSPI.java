/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.catalog;

import com.thoughtworks.xstream.XStream;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.cluster.JMSEventHandler;
import org.geoserver.cluster.events.ToggleSwitch;

/** @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it */
public class JMSCatalogRemoveEventHandlerSPI extends JMSCatalogEventHandlerSPI<CatalogRemoveEvent> {

    public JMSCatalogRemoveEventHandlerSPI(
            final int priority, Catalog cat, XStream xstream, ToggleSwitch producer) {
        super(priority, cat, xstream, producer, CatalogRemoveEvent.class);
    }

    @Override
    public JMSEventHandler<String, CatalogRemoveEvent> createHandler() {
        return new JMSCatalogRemoveEventHandler(catalog, xstream, this.getClass(), producer);
    }
}
