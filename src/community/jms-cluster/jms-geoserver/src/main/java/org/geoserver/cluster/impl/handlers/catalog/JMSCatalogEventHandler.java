/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.catalog;

import com.thoughtworks.xstream.XStream;
import java.util.logging.Level;
import javax.jms.JMSException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.cluster.JMSEventHandlerSPI;
import org.geoserver.cluster.impl.handlers.XStreamEventHandler;

/**
 * Abstract class which use Xstream as message serializer/de-serializer. We extend this class to
 * implementing synchronize method.
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public abstract class JMSCatalogEventHandler<E extends CatalogEvent>
        extends XStreamEventHandler<E> {
    public JMSCatalogEventHandler(
            final XStream xstream, Class<? extends JMSEventHandlerSPI<String, E>> generatorClass) {
        super(xstream, generatorClass);
    }

    @Override
    protected void configureXStream(XStream xstream) {
        // omit not serializable fields
        xstream.omitField(CatalogImpl.class, "listeners");
        xstream.omitField(CatalogImpl.class, "facade");
        xstream.omitField(CatalogImpl.class, "resourcePool");
        xstream.omitField(CatalogImpl.class, "resourceLoader");
    }

    @Override
    public E deserialize(String s) throws Exception {
        final Object source = xstream.fromXML(s);
        if (source instanceof CatalogEvent) {
            @SuppressWarnings("unchecked")
            final E ev = (E) source;
            if (LOGGER.isLoggable(Level.FINE)) {
                final CatalogInfo info = ev.getSource();
                LOGGER.fine("Incoming message event of type CatalogEvent: " + info.getId());
            }
            return ev;
        } else {
            throw new JMSException("Unable to deserialize the following object:\n" + s);
        }
    }
}
