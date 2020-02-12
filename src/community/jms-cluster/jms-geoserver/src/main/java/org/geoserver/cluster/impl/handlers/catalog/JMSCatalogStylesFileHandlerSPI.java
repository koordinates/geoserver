/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.catalog;

import com.thoughtworks.xstream.XStream;
import org.geoserver.cluster.JMSEventHandler;
import org.geoserver.cluster.configuration.JMSConfiguration;
import org.geoserver.cluster.impl.handlers.DocumentFile;
import org.geoserver.cluster.impl.handlers.DocumentFileHandlerSPI;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;

/** @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it */
public class JMSCatalogStylesFileHandlerSPI extends DocumentFileHandlerSPI {

    final XStream xstream;
    private final GeoServerResourceLoader loader;

    @Autowired public JMSConfiguration config;

    public JMSCatalogStylesFileHandlerSPI(
            final int priority, XStream xstream, GeoServerResourceLoader loader) {
        super(priority, xstream);
        this.xstream = xstream;
        this.loader = loader;
    }

    @Override
    public JMSEventHandler<String, DocumentFile> createHandler() {
        JMSCatalogStylesFileHandler styleHandler =
                new JMSCatalogStylesFileHandler(
                        xstream, JMSCatalogStylesFileHandlerSPI.class, loader);
        styleHandler.setConfig(config);
        return styleHandler;
    }
}
