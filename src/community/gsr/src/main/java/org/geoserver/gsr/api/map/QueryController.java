/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.api.map;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.gsr.api.AbstractGSRController;
import org.geoserver.gsr.api.GSRProtobufConverter;
import org.geoserver.gsr.model.GSRModel;
import org.geoserver.gsr.model.feature.FeatureList;
import org.geoserver.gsr.model.map.LayerOrTable;
import org.geoserver.gsr.model.map.LayersAndTables;
import org.geoserver.gsr.translate.feature.FeatureDAO;
import org.geoserver.gsr.translate.feature.FeatureEncoder;
import org.geoserver.gsr.translate.map.LayerDAO;
import org.geoserver.ogcapi.APIException;
import org.geoserver.wfs.json.JSONType;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.referencing.CRS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Controller for the Map Service query endpoint */
@RestController
@RequestMapping(path = "/gsr/rest/services/{workspaceName}/{layerName}/MapServer")
public class QueryController extends AbstractGSRController {

    @Autowired
    public QueryController(@Qualifier("geoServer") GeoServer geoServer) {
        super(geoServer);
    }

    @RequestMapping(
            path = "/{layerId}/query",
            method = {RequestMethod.GET, RequestMethod.POST},
            name = "MapServerQuery",
            produces = {MediaType.APPLICATION_JSON_VALUE, JSONType.jsonp, GSRProtobufConverter.PBF})
    public GSRModel query(
            @PathVariable String workspaceName,
            @PathVariable String layerName,
            @PathVariable Integer layerId,
            @RequestParam(name = "f", required = false, defaultValue = "json") String format,
            @RequestParam(
                            name = "geometryType",
                            required = false,
                            defaultValue = "esriGeometryEnvelope")
                    String geometryTypeName,
            @RequestParam(name = "geometry", required = false) String geometryText,
            @RequestParam(name = "inSR", required = false) String inSRText,
            @RequestParam(name = "outSR", required = false) String outSRText,
            @RequestParam(
                            name = "spatialRel",
                            required = false,
                            defaultValue = "esriSpatialRelIntersects")
                    String spatialRelText,
            @RequestParam(name = "objectIds", required = false) String objectIdsText,
            @RequestParam(name = "relationPattern", required = false) String relatePattern,
            @RequestParam(name = "time", required = false) String time,
            @RequestParam(name = "text", required = false) String text,
            @RequestParam(name = "maxAllowableOffsets", required = false)
                    String maxAllowableOffsets,
            @RequestParam(name = "where", required = false) String whereClause,
            @RequestParam(name = "returnGeometry", required = false, defaultValue = "true")
                    Boolean returnGeometry,
            @RequestParam(name = "outFields", required = false, defaultValue = "*")
                    String outFieldsText,
            @RequestParam(name = "returnIdsOnly", required = false, defaultValue = "false")
                    boolean returnIdsOnly,
            @RequestParam(name = "returnCountOnly", required = false, defaultValue = "false")
                    boolean returnCountOnly,
            @RequestParam(name = "quantizationParameters", required = false)
                    String quantizationParameters,
            @RequestParam(name = "resultRecordCount", required = false) Integer resultRecordCount,
            @RequestParam(name = "resultOffset", required = false, defaultValue = "0")
                    Integer resultOffset)
            throws IOException {

        LayersAndTables layersAndTables = LayerDAO.find(catalog, workspaceName, layerName);
        if (layersAndTables.layers.size() == 0 & layersAndTables.tables.size() == 0) {
            throw new APIException(
                    "InvalidLayerName",
                    layerName + " does not correspond to a layer in the workspace.",
                    HttpStatus.NOT_FOUND);
        }
        FeatureCollection<? extends FeatureType, ? extends Feature> features =
                FeatureDAO.getFeatureCollectionForLayerWithId(
                        workspaceName,
                        layerId,
                        geometryTypeName,
                        geometryText,
                        inSRText,
                        outSRText,
                        spatialRelText,
                        objectIdsText,
                        relatePattern,
                        time,
                        text,
                        maxAllowableOffsets,
                        whereClause,
                        returnGeometry,
                        outFieldsText,
                        resultOffset,
                        resultRecordCount,
                        layersAndTables);

        FeatureIterator<? extends Feature> iterator = null;
        Feature f = null;
        try {
            iterator = features.features();
            while (iterator.hasNext()) {
                f = iterator.next();
            }
        } catch (Exception e) {
            LayerInfo l = null;
            for (LayerOrTable layerOrTable : layersAndTables.layers) {
                if (Objects.equals(layerOrTable.getId(), layerId)) {
                    l = layerOrTable.layer;
                    break;
                }
            }

            if (l == null) {
                for (LayerOrTable layerOrTable : layersAndTables.tables) {
                    if (Objects.equals(layerOrTable.getId(), layerId)) {
                        l = layerOrTable.layer;
                        break;
                    }
                }
            }
            FeatureTypeInfo featureType = (FeatureTypeInfo) l.getResource();
            FeatureSource<? extends FeatureType, ? extends Feature> source =
                    featureType.getFeatureSource(null, null);
            throw new RuntimeException(
                    e.getMessage()
                            + "\n-----\n"
                            + iterator.getClass()
                            + ","
                            + features.getClass()
                            + ","
                            + source.getClass()
                            + ","
                            + featureType.getClass()
                            + "\n"
                            + iterator
                            + ","
                            + features
                            + ","
                            + source
                            + ","
                            + featureType
                            + "\n"
                            + "\n-----\n"
                            + toString((SimpleFeatureType) features.getSchema())
                            + "\n-----\n"
                            + f,
                    e);
        }

        features =
                FeatureDAO.getFeatureCollectionForLayerWithId(
                        workspaceName,
                        layerId,
                        geometryTypeName,
                        geometryText,
                        inSRText,
                        outSRText,
                        spatialRelText,
                        objectIdsText,
                        relatePattern,
                        time,
                        text,
                        maxAllowableOffsets,
                        whereClause,
                        returnGeometry,
                        outFieldsText,
                        resultOffset,
                        resultRecordCount,
                        layersAndTables);
        // returnCountOnly should take precedence over returnIdsOnly.
        // Sometimes both count and Ids are requested, but the count is expected in
        // some ArcGIS softwares (e.g. AGOL) to be returned when loading attribute tables.
        if (returnCountOnly) {
            return FeatureEncoder.count(features);
        } else if (returnIdsOnly) {
            return FeatureEncoder.objectIds(features);
        } else {
            FeatureList featureList =
                    new FeatureList(
                            features,
                            returnGeometry,
                            outSRText,
                            quantizationParameters,
                            format,
                            resultRecordCount);
            return featureList;
        }
    }

    private String toString(Object[] attributes) {
        String result = "[";
        for (Object attr : attributes) {
            result += attr + ",";
        }
        return result.substring(0, result.length() - 1) + "]";
    }

    private static String toString(SimpleFeatureType schema) {
        return String.join("\n", toStringArray(schema.getAttributeDescriptors()));
    }

    private static String[] toStringArray(List<AttributeDescriptor> descriptors) {
        String[] result = new String[descriptors.size()];
        for (int i = 0; i < descriptors.size(); i++) {
            result[i] = toString(descriptors.get(i));
        }
        return result;
    }

    private static String toString(AttributeDescriptor descriptor) {
        if (descriptor instanceof GeometryDescriptor) {
            return toString((GeometryDescriptor) descriptor);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("AttributeDescriptor(");
        sb.append(descriptor.getName() + ", ");
        sb.append(getOccursStr(descriptor) + ", ");
        sb.append("binding=" + descriptor.getType().getBinding().getSimpleName() + ")");
        return sb.toString();
    }

    private static String toString(GeometryDescriptor descriptor) {
        StringBuilder sb = new StringBuilder("GeometryDescriptor(");
        sb.append(descriptor.getName() + ", ");
        sb.append(getOccursStr(descriptor) + ", ");
        sb.append("binding=" + descriptor.getType().getBinding().getSimpleName() + ", ");
        sb.append("CRS=" + CRS.toSRS(descriptor.getCoordinateReferenceSystem()) + ")");
        return sb.toString();
    }

    private static String getOccursStr(AttributeDescriptor d) {
        return String.format(
                "(%d,%d%s)", d.getMinOccurs(), d.getMaxOccurs(), (d.isNillable() ? ",nil" : ""));
    }
}
