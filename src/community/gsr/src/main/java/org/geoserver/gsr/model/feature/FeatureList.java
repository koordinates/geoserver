/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr.model.feature;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.lang.StringUtils;
import org.geoserver.gsr.Utils;
import org.geoserver.gsr.model.GSRModel;
import org.geoserver.gsr.model.geometry.*;
import org.geoserver.gsr.translate.feature.FeatureEncoder;
import org.geoserver.gsr.translate.geometry.AbstractGeometryEncoder;
import org.geoserver.gsr.translate.geometry.GeometryEncoder;
import org.geoserver.gsr.translate.geometry.QuantizedGeometryEncoder;
import org.geoserver.gsr.translate.geometry.SpatialReferenceEncoder;
import org.geoserver.gsr.translate.geometry.SpatialReferences;
import org.geoserver.ogcapi.APIException;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.visitor.UniqueVisitor;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Envelope;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.springframework.http.HttpStatus;

/**
 * List of {@link Feature}, that can be serialized as JSON
 *
 * <p>See https://developers.arcgis.com/documentation/common-data-types/featureset-object.htm
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeatureList implements GSRModel {

    public final String objectIdFieldName = FeatureEncoder.OBJECTID_FIELD_NAME;

    public final String globalIdFieldName = "";

    public final String geometryType;

    public final SpatialReference spatialReference;

    public final Transform transform;

    public final Boolean exceededTransferLimit;

    public final ArrayList<Field> fields = new ArrayList<>();

    public final ArrayList<Feature> features = new ArrayList<>();

    public <T extends FeatureType, F extends org.opengis.feature.Feature> FeatureList(
            FeatureCollection<T, F> collection, boolean returnGeometry) throws IOException {
        this(collection, returnGeometry, null);
    }

    public <T extends FeatureType, F extends org.opengis.feature.Feature> FeatureList(
            FeatureCollection<T, F> collection, boolean returnGeometry, String outputSR)
            throws IOException {
        this(collection, returnGeometry, false, true, "none", null, outputSR, null, null, 0, null);
    }

    public <T extends FeatureType, F extends org.opengis.feature.Feature> FeatureList(
            FeatureCollection<T, F> collection,
            boolean returnGeometry,
            boolean returnDistinctValues,
            boolean returnExceededLimitFeatures,
            String resultType,
            String outFieldsText,
            String outputSR,
            String quantizationParameters,
            String format,
            Integer resultOffset,
            Integer resultRecordCount)
            throws IOException {

        T schema = collection.getSchema();

        // determine geometry type
        if (returnGeometry) {
            GeometryDescriptor geometryDescriptor = schema.getGeometryDescriptor();
            if (geometryDescriptor == null) {
                throw new RuntimeException(
                        "No geometry descriptor for type "
                                + schema
                                + "; "
                                + schema.getDescriptors());
            }
            GeometryType geometryType = geometryDescriptor.getType();
            if (geometryType == null) {
                throw new RuntimeException("No geometry type for type " + schema);
            }
            Class<?> binding = geometryType.getBinding();
            if (binding == null) {
                throw new RuntimeException("No binding for geometry type " + schema);
            }
            GeometryTypeEnum geometryTypeEnum = GeometryTypeEnum.forJTSClass(binding);
            this.geometryType = geometryTypeEnum.getGeometryType();
        } else {
            this.geometryType = null;
        }

        // determine crs
        CoordinateReferenceSystem outCrs = null;
        if (StringUtils.isNotEmpty(outputSR)) {
            outCrs = Utils.parseSpatialReference(outputSR);
        } else if (schema.getCoordinateReferenceSystem() != null) {
            outCrs = schema.getCoordinateReferenceSystem();
        }
        if (outCrs == null) {
            spatialReference = null;
        } else {
            try {
                spatialReference = SpatialReferences.fromCRS(outCrs);
            } catch (FactoryException e) {
                throw new RuntimeException(e);
            }
        }

        AbstractGeometryEncoder geometryEncoder;
        // Parse quantizationParameters
        if (null == quantizationParameters || quantizationParameters.isEmpty()) {
            transform = null;
            geometryEncoder = new GeometryEncoder();
        } else if (!quantizationParameters.isEmpty() && format.equals("pbf")) {
            JSONObject json = (JSONObject) JSONSerializer.toJSON(quantizationParameters);
            QuantizedGeometryEncoder.OriginPosition originPosition =
                    QuantizedGeometryEncoder.OriginPosition.valueOf(
                            json.getString("originPosition"));
            Double tolerance = json.getDouble("tolerance");
            Envelope extent = GeometryEncoder.jsonToEnvelope(json.getJSONObject("extent"));
            double[] translate;
            if (extent != null) {
                translate = new double[] {extent.getMinX(), extent.getMaxY()};
            } else {
                translate = new double[] {0, 0};
            }
            geometryEncoder = new GeometryEncoder();
            transform =
                    new Transform(originPosition, new double[] {tolerance, tolerance}, translate);
        } else {
            JSONObject json = (JSONObject) JSONSerializer.toJSON(quantizationParameters);

            QuantizedGeometryEncoder.Mode mode =
                    QuantizedGeometryEncoder.Mode.valueOf(json.getString("mode"));
            QuantizedGeometryEncoder.OriginPosition originPosition =
                    QuantizedGeometryEncoder.OriginPosition.valueOf(
                            json.getString("originPosition"));
            Double tolerance = json.getDouble("tolerance");
            Envelope extent = GeometryEncoder.jsonToEnvelope(json.getJSONObject("extent"));
            CoordinateReferenceSystem envelopeCrs =
                    SpatialReferenceEncoder.coordinateReferenceSystemFromJSON(
                            json.getJSONObject("extent").getJSONObject("spatialReference"));

            MathTransform mathTx;
            try {
                mathTx = CRS.findMathTransform(envelopeCrs, outCrs, true);
            } catch (FactoryException e) {
                throw new IllegalArgumentException(
                        "Unable to translate between input and native coordinate reference systems",
                        e);
            }
            Envelope transformedExtent;
            try {
                transformedExtent = JTS.transform(extent, mathTx);
            } catch (TransformException e) {
                throw new IllegalArgumentException(
                        "Error while converting envelope from input to native coordinate system",
                        e);
            }

            // TODO: Transform extent to outSR before determining translate
            // default to upperLeft
            double[] translate =
                    new double[] {transformedExtent.getMinX(), transformedExtent.getMaxY()};
            if (originPosition == QuantizedGeometryEncoder.OriginPosition.lowerLeft) {
                translate = new double[] {transformedExtent.getMinX(), transformedExtent.getMinY()};
            }
            transform =
                    new Transform(originPosition, new double[] {tolerance, tolerance}, translate);

            geometryEncoder =
                    new QuantizedGeometryEncoder(
                            mode, originPosition, tolerance, transformedExtent);
        }

        for (PropertyDescriptor desc : schema.getDescriptors()) {
            if (schema.getGeometryDescriptor() != null
                    && !desc.getName().equals(schema.getGeometryDescriptor().getName())) {
                fields.add(FeatureEncoder.field(desc, null));
            }
        }

        fields.add(FeatureEncoder.syntheticObjectIdField(objectIdFieldName));

        if (returnDistinctValues
                && outFieldsText != null
                && !outFieldsText.contains(FeatureEncoder.OBJECTID_FIELD_NAME)) {
            String field;
            String[] outFields = outFieldsText.split(",");
            if (outFields.length > 1) {
                // TODO: support multiple outFields once rebased on geoserver 2.25
                throw new APIException(
                        "InvalidParameter",
                        "Only one field can be specified when returnDistinctValues is true",
                        HttpStatus.BAD_REQUEST);
            }
            field = outFields[0];
            UniqueVisitor visitor = new UniqueVisitor(field);
            if (resultRecordCount != null) {
                visitor.setStartIndex(resultOffset);
                visitor.setMaxFeatures(resultRecordCount);
            }
            collection.accepts(visitor, null);

            Set uniqueValues = visitor.getUnique();
            Map<String, Object> attributes;

            for (Object value : uniqueValues) {
                attributes = new HashMap<>();
                attributes.put(field, value);
                features.add(new Feature(null, attributes));
            }
        } else {
            try (FeatureIterator<F> iterator = collection.features()) {
                while (iterator.hasNext()) {
                    org.opengis.feature.Feature feature = iterator.next();
                    features.add(
                            FeatureEncoder.feature(
                                    feature,
                                    returnGeometry,
                                    spatialReference,
                                    objectIdFieldName,
                                    geometryEncoder));
                }
            }
        }
        if ((resultRecordCount == null)
                || (resultRecordCount != null && features.size() < resultRecordCount)) {
            exceededTransferLimit = false;
        } else {
            exceededTransferLimit = true;
        }

        if (!returnExceededLimitFeatures && resultType.equals("tile")) {
            features.removeIf(feature -> exceededTransferLimit);
        }
    }
}
