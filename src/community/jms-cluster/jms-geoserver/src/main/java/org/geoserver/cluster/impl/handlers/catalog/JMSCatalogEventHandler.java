/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.catalog;

import com.thoughtworks.xstream.XStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.jms.JMSException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.impl.CatalogModifyEventImpl;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.cluster.JMSEventHandler;
import org.geoserver.cluster.JMSEventHandlerSPI;

/**
 * Abstract class which use Xstream as message serializer/de-serializer. We extend this class to
 * implementing synchronize method.
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public abstract class JMSCatalogEventHandler<E extends CatalogEvent>
        extends JMSEventHandler<String, E> {
    public JMSCatalogEventHandler(
            final XStream xstream, Class<? extends JMSEventHandlerSPI<String, E>> generatorClass) {
        super(xstream, generatorClass);
        // omit not serializable fields
        omitFields();
    }

    /**
     * omit not serializable fields
     *
     * @see {@link XStream}
     */
    private void omitFields() {
        // omit not serializable fields
        xstream.omitField(CatalogImpl.class, "listeners");
        xstream.omitField(CatalogImpl.class, "facade");
        xstream.omitField(CatalogImpl.class, "resourcePool");
        xstream.omitField(CatalogImpl.class, "resourceLoader");
    }

    @Override
    public String serialize(CatalogEvent event) throws Exception {
        return xstream.toXML(removeCatalogProperties(event));
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

    /** Make sure that properties of type catalog are not serialized for catalog modified events. */
    private CatalogEvent removeCatalogProperties(CatalogEvent event) {
        if (!(event instanceof CatalogModifyEvent)) {
            // not a modify event so nothing to do
            return event;
        }
        final CatalogModifyEvent modifyEvent = (CatalogModifyEvent) event;
        final int propCount = modifyEvent.getPropertyNames().size();
        final Set<Integer> catalogIndexes =
                IntStream.range(0, propCount)
                        .filter(
                                i ->
                                        modifyEvent.getNewValues().get(i) instanceof Catalog
                                                || modifyEvent.getOldValues().get(i)
                                                        instanceof Catalog)
                        .boxed()
                        .collect(Collectors.toSet());
        if (catalogIndexes.isEmpty()) {
            return event;
        }
        final List<String> properties = new ArrayList<>(propCount);
        final List<Object> oldValues = new ArrayList<>(propCount);
        final List<Object> newValues = new ArrayList<>(propCount);
        IntStream.range(0, propCount)
                .filter(i -> !catalogIndexes.contains(Integer.valueOf(i)))
                .forEach(
                        index -> {
                            properties.add(modifyEvent.getPropertyNames().get(index));
                            oldValues.add(modifyEvent.getOldValues().get(index));
                            newValues.add(modifyEvent.getNewValues().get(index));
                        });
        // crete the new event
        CatalogModifyEventImpl newEvent = new CatalogModifyEventImpl();
        newEvent.setPropertyNames(properties);
        newEvent.setOldValues(oldValues);
        newEvent.setNewValues(newValues);
        newEvent.setSource(modifyEvent.getSource());
        return newEvent;
    }
}
