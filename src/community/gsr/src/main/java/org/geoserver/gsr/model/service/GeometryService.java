/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.model.service;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.geoserver.platform.GeoServerExtensions;

/**
 * Simple model of a geometry service, for use in the list of services published by {@link
 * CatalogService}
 *
 * @author Juan Marin, OpenGeo
 */
@XStreamAlias("GeometryService")
public class GeometryService implements AbstractService {

    private String name;

    private ServiceType type;

    /*
     * The key of the property to disable the GeometryServer. This property is set default to false.
     */
    public static final String DISABLE_GSR_GEOMETRY_SERVICE_KEY = "DISABLE_GSR_GEOMETRY_SERVICE";

    private static boolean geometryServiceDisabled = isGeometryServicePropertyDisabled();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ServiceType getType() {
        return type;
    }

    public void setType(ServiceType serviceType) {
        this.type = serviceType;
    }

    public GeometryService(String name) {
        this.name = name;
        this.type = ServiceType.GeometryServer;
    }

    private static boolean isGeometryServicePropertyDisabled() {
        String geometryService = GeoServerExtensions.getProperty(DISABLE_GSR_GEOMETRY_SERVICE_KEY);
        return Boolean.parseBoolean(geometryService);
    }

    private static ReadWriteLock lock = new ReentrantReadWriteLock(true);

    /**
     * @return The boolean returned represents the value of the geometryService disable toggle (if
     *     true geometryService is disabled)
     */
    public static boolean isGeometryServiceDisabled() {
        lock.readLock().lock();
        try {
            return geometryServiceDisabled;
        } finally {
            lock.readLock().unlock();
        }
    }
}
