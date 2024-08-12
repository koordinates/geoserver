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
import org.geoserver.config.GeoServer;
import org.geoserver.gsr.api.AbstractGSRController;
import org.geoserver.gsr.api.GSRProtobufConverter;
import org.geoserver.gsr.model.GSRModel;
import org.geoserver.gsr.model.feature.FeatureList;
import org.geoserver.gsr.model.feature.FeatureStatistics;
import org.geoserver.gsr.model.map.LayersAndTables;
import org.geoserver.gsr.translate.feature.FeatureDAO;
import org.geoserver.gsr.translate.feature.FeatureEncoder;
import org.geoserver.gsr.translate.map.LayerDAO;
import org.geoserver.ogcapi.APIException;
import org.geoserver.wfs.json.JSONType;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
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
            @RequestParam(name = "returnDistinctValues", required = false, defaultValue = "false")
                    boolean returnDistinctValues,
            @RequestParam(name = "quantizationParameters", required = false)
                    String quantizationParameters,
            @RequestParam(name = "resultRecordCount", required = false) Integer resultRecordCount,
            @RequestParam(name = "resultOffset", required = false, defaultValue = "0")
                    Integer resultOffset,
            @RequestParam(name = "orderByFields", required = false) String orderByFieldsText,
            @RequestParam(name = "groupByFieldsForStatistics", required = false)
                    String groupByFieldsForStatistics,
            @RequestParam(name = "outStatistics", required = false) String outStatistics)
            throws IOException {

        if (returnDistinctValues && returnGeometry) {
            throw new APIException(
                    "InvalidParameter",
                    "returnDistinctValues cannot be true when returnGeometry is true.",
                    HttpStatus.BAD_REQUEST);
        }

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
                        returnDistinctValues,
                        outFieldsText,
                        resultOffset,
                        resultRecordCount,
                        orderByFieldsText,
                        layersAndTables);

        if (groupByFieldsForStatistics != null && outStatistics != null) {
            return new FeatureStatistics(features, groupByFieldsForStatistics, outStatistics);
        }

        FeatureList featureList =
                new FeatureList(
                        features,
                        returnGeometry,
                        returnDistinctValues,
                        outFieldsText,
                        outSRText,
                        quantizationParameters,
                        format,
                        resultOffset,
                        resultRecordCount);

        // returnCountOnly should take precedence over returnIdsOnly.
        // Sometimes both count and Ids are requested, but the count is expected in
        // some ArcGIS softwares (e.g. AGOL) to be returned when loading attribute tables.
        if (returnCountOnly) {
            return FeatureEncoder.count(featureList);
        } else if (returnIdsOnly) {
            return FeatureEncoder.objectIds(featureList);
        } else {
            return featureList;
        }
    }
}
