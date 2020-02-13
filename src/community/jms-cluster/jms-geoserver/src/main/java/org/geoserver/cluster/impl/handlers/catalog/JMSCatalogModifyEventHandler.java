/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.catalog;

import com.thoughtworks.xstream.XStream;
import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.impl.CatalogModifyEventImpl;
import org.geoserver.cluster.JMSEventHandlerSPI;
import org.geoserver.cluster.events.ToggleSwitch;
import org.geoserver.cluster.impl.utils.BeanUtils;
import org.geoserver.cluster.server.events.StyleModifyEvent;

/**
 * Handle modify events synchronizing catalog with serialized objects
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class JMSCatalogModifyEventHandler extends JMSCatalogEventHandler<CatalogModifyEvent> {

    private final Catalog catalog;
    private final ToggleSwitch producer;

    /**
     * @param catalog
     * @param xstream
     * @param generatorClass
     * @param producer
     */
    public JMSCatalogModifyEventHandler(
            Catalog catalog,
            XStream xstream,
            Class<? extends JMSEventHandlerSPI<String, CatalogModifyEvent>> generatorClass,
            ToggleSwitch producer) {
        super(xstream, generatorClass);
        this.catalog = catalog;
        this.producer = producer;
    }

    @Override
    public boolean synchronize(CatalogModifyEvent modifyEv) throws Exception {
        Objects.requireNonNull(modifyEv, "Incoming object is null");
        try {
            producer.disable();
            modify(catalog, modifyEv);
        } finally {
            // re enable the producer
            producer.enable();
        }
        return true;
    }

    @Override
    public String serialize(CatalogModifyEvent event) throws Exception {
        return super.serialize(removeCatalogProperties(event));
    }

    /** Make sure that properties of type catalog are not serialized for catalog modified events. */
    private CatalogModifyEvent removeCatalogProperties(CatalogModifyEvent event) {
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

    private <T extends CatalogInfo> T modifyLocalObject(
            CatalogModifyEvent modifyEv, Function<String, T> findByIdFunction)
            throws CatalogException, IllegalAccessException, InvocationTargetException,
                    NoSuchMethodException {

        final CatalogInfo eventSource = modifyEv.getSource();
        final List<String> propertyNames = modifyEv.getPropertyNames();
        final List<Object> newValues = modifyEv.getNewValues();

        T localObject = findByIdFunction.apply(eventSource.getId());
        if (localObject == null) {
            throw new CatalogException(String.format("Unable to locate %s locally", eventSource));
        }
        BeanUtils.smartUpdate(localObject, propertyNames, newValues);
        return localObject;
    }

    /**
     * simulate a catalog.save() rebuilding the EventModify proxy object locally {@link
     * org.geoserver.catalog.impl.DefaultCatalogFacade#saved(CatalogInfo)}
     *
     * @param catalog
     * @param event
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     *     <p>TODO synchronization on catalog object
     */
    protected void modify(final Catalog catalog, CatalogModifyEvent event)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        final CatalogInfo info = event.getSource();
        Objects.requireNonNull(info, "Event does not provide the modified CatalogInfo instance");

        if (info instanceof Catalog) {
            handleCatalogModification(catalog, event);
            return;
        }

        CatalogInfo localObject;

        if (info instanceof LayerGroupInfo) {
            localObject = modifyLocalObject(event, catalog::getLayerGroup);
            catalog.save((LayerGroupInfo) localObject);
        } else if (info instanceof LayerInfo) {
            localObject = modifyLocalObject(event, catalog::getLayer);
            catalog.save((LayerInfo) localObject);
        } else if (info instanceof MapInfo) {
            localObject = modifyLocalObject(event, catalog::getMap);
            catalog.save((MapInfo) localObject);
        } else if (info instanceof NamespaceInfo) {
            localObject = modifyLocalObject(event, catalog::getNamespace);
            catalog.save((NamespaceInfo) localObject);
        } else if (info instanceof StoreInfo) {
            localObject = modifyLocalObject(event, id -> catalog.getStore(id, StoreInfo.class));
            catalog.save((StoreInfo) localObject);
        } else if (info instanceof ResourceInfo) {
            localObject =
                    modifyLocalObject(event, id -> catalog.getResource(id, ResourceInfo.class));
            catalog.save((ResourceInfo) localObject);
        } else if (info instanceof StyleInfo) {
            localObject = modifyLocalObject(event, catalog::getStyle);
            catalog.save((StyleInfo) localObject);
            // let's if the style file was provided
            if (event instanceof StyleModifyEvent) {
                StyleModifyEvent styleModifyEvent = (StyleModifyEvent) event;
                byte[] fileContent = styleModifyEvent.getFile();
                if (fileContent != null && fileContent.length != 0) {
                    // update the style file using the old style, in case name changed
                    StyleInfo currentStyle = catalog.getStyle(info.getId());
                    try {
                        catalog.getResourcePool()
                                .writeStyle(currentStyle, new ByteArrayInputStream(fileContent));
                    } catch (Exception exception) {
                        throw new RuntimeException(
                                String.format(
                                        "Error writing style '%s' file.",
                                        ((StyleInfo) localObject).getName()),
                                exception);
                    }
                }
            }
        } else if (info instanceof WorkspaceInfo) {
            localObject = modifyLocalObject(event, catalog::getWorkspace);
            catalog.save((WorkspaceInfo) localObject);
        } else {
            final String stringRepresentation = info.toString();
            LOGGER.warning(
                    "Unknown CatalogInfo object type. ID: "
                            + info.getId()
                            + ": "
                            + stringRepresentation);
            throw new IllegalArgumentException("Bad incoming object: " + stringRepresentation);
        }
    }

    private void handleCatalogModification(final Catalog catalog, CatalogModifyEvent event) {
        final List<String> propertyNames = event.getPropertyNames();
        final List<Object> newValues = event.getNewValues();

        // change default workspace in the handled catalog
        /**
         * This piece of code was inspired on: {@link
         * org.geoserver.catalog.NamespaceWorkspaceConsistencyListener#handleModifyEvent(CatalogModifyEvent)}
         */
        final int defWsIndex;
        if ((defWsIndex = propertyNames.indexOf("defaultWorkspace")) > -1) {
            final WorkspaceInfo newDefaultWS = (WorkspaceInfo) newValues.get(defWsIndex);
            final WorkspaceInfo ws =
                    newDefaultWS == null
                            ? null
                            : catalog.getWorkspaceByName(newDefaultWS.getName());
            catalog.setDefaultWorkspace(ws);
        }
        final int defNsIndex;
        if ((defNsIndex = propertyNames.indexOf("defaultNamespace")) > -1) {
            final NamespaceInfo newDefaultNS = (NamespaceInfo) newValues.get(defNsIndex);
            final NamespaceInfo ns =
                    newDefaultNS == null
                            ? null
                            : catalog.getNamespaceByPrefix(newDefaultNS.getPrefix());
            catalog.setDefaultNamespace(ns);
        }
    }
}
