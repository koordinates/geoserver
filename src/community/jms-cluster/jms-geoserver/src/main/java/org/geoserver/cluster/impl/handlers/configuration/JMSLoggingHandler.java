/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.configuration;

import com.thoughtworks.xstream.XStream;
import java.util.Objects;
import org.apache.commons.beanutils.BeanUtils;
import org.geoserver.cluster.JMSEventHandlerSPI;
import org.geoserver.cluster.events.ToggleSwitch;
import org.geoserver.config.GeoServer;
import org.geoserver.config.LoggingInfo;

/**
 * JMS Handler is used to synchronize
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class JMSLoggingHandler extends JMSConfigurationHandler<LoggingInfo> {
    private final GeoServer geoServer;

    private final ToggleSwitch producer;

    public JMSLoggingHandler(
            GeoServer geo,
            XStream xstream,
            Class<? extends JMSEventHandlerSPI<String, LoggingInfo>> generatorClass,
            ToggleSwitch producer) {
        super(xstream, generatorClass);
        this.geoServer = geo;
        this.producer = producer;
    }

    @Override
    public boolean synchronize(LoggingInfo info) throws Exception {
        Objects.requireNonNull(info, "Incoming object is null");
        try {
            // LOCALIZE service
            final LoggingInfo localObject = geoServer.getLogging();
            // overwrite local object members with new incoming settings
            BeanUtils.copyProperties(localObject, info);

            // disable the message producer to avoid recursion
            producer.disable();

            // save the localized object
            geoServer.save(localObject);
        } finally {
            // enable message the producer
            producer.enable();
        }
        return true;
    }
}
