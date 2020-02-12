/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.configuration;

import com.thoughtworks.xstream.XStream;
import org.geoserver.cluster.JMSEventHandlerSPI;
import org.geoserver.cluster.impl.handlers.XStreamEventHandler;
import org.geoserver.config.GeoServer;

/**
 * Abstract class which use Xstream as message serializer/de-serializer.
 *
 * <p>You have to extend this class to implement synchronize method.
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public abstract class JMSConfigurationHandler<TYPE> extends XStreamEventHandler<TYPE> {
    public JMSConfigurationHandler(
            final XStream xstream,
            Class<? extends JMSEventHandlerSPI<String, TYPE>> generatorClass) {
        super(xstream, generatorClass);
    }

    @Override
    protected void configureXStream(final XStream xstream) {
        xstream.omitField(GeoServer.class, "geoServer");
    }
}
