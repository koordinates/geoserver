package org.geoserver.gsr.api;

import com.esri.arcgis.protobuf.FeatureCollection.FeatureCollectionPBuffer;
import com.esri.arcgis.protobuf.FeatureCollection.FeatureCollectionPBuffer.FeatureResult;
import com.esri.arcgis.protobuf.FeatureCollection.FeatureCollectionPBuffer.GeometryType;
import com.esri.arcgis.protobuf.FeatureCollection.FeatureCollectionPBuffer.Value;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.measure.Unit;
import org.geoserver.gsr.model.feature.FeatureList;
import org.geoserver.gsr.model.feature.Transform;
import org.geoserver.gsr.model.geometry.Geometry;
import org.geoserver.gsr.model.geometry.GeometryTypeEnum.*;
import org.geoserver.gsr.model.geometry.Multipoint;
import org.geoserver.gsr.model.geometry.Point;
import org.geoserver.gsr.model.geometry.Polygon;
import org.geoserver.gsr.model.geometry.Polyline;
import org.geoserver.gsr.model.geometry.SpatialReferenceWKID;
import org.geoserver.gsr.model.geometry.SpatialReferenceWKT;
import org.geoserver.gsr.translate.geometry.SpatialReferences;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.crs.DefaultProjectedCRS;
import org.geotools.referencing.wkt.Parser;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.api.referencing.crs.ProjectedCRS;
import si.uom.NonSI;
import si.uom.SI;

public class GSRFeatureResultPBF {

    // We need custom mappings, as the types defined in GSR differ to the GeometryTypes
    // defined in the protofile.
    private static final Map<String, String> GEOMETRY_TYPE_MAPPING;

    static {
        Map<String, String> geometryMap = new HashMap<>();
        geometryMap.put("esriGeometryPoint", "esriGeometryTypePoint");
        geometryMap.put("esriGeometryMultipoint", "esriGeometryTypeMultipoint");
        geometryMap.put("esriGeometryPolyline", "esriGeometryTypePolyline");
        geometryMap.put("esriGeometryPolygon", "esriGeometryTypePolygon");
        geometryMap.put("esriGeometryEnvelope", "esriGeometryTypeMultipatch");
        geometryMap.put("None", "esriGeometryTypeNone");
        GEOMETRY_TYPE_MAPPING = Collections.unmodifiableMap(geometryMap);
    }

    /**
     * FeatureResult consists of the following fields: objectIdFieldName, globalIdFieldName,
     * geometryType, exceededTransferLimit, spatialReference, transform, fields, features
     *
     * @param flist
     * @return FeatureResult
     */
    public static FeatureResult buildFeatureResult(FeatureList flist) {

        FeatureResult.Builder featureRestBuilder = FeatureResult.newBuilder();

        // OBJECTID Field and GlobalID Field
        featureRestBuilder.setObjectIdFieldName(flist.objectIdFieldName);
        featureRestBuilder.setGlobalIdFieldName(flist.globalIdFieldName);

        // exceededTransferLimit
        if (flist.exceededTransferLimit != null) {
            featureRestBuilder.setExceededTransferLimit(flist.exceededTransferLimit);
        }

        // Geometry Type
        if (flist.geometryType != null) {
            EnumDescriptor enumDescriptor = GeometryType.getDescriptor();

            String esriPbufferGeomType = GEOMETRY_TYPE_MAPPING.get(flist.geometryType);
            EnumValueDescriptor valueDescriptor =
                    enumDescriptor.findValueByName(esriPbufferGeomType);

            FeatureCollectionPBuffer.GeometryType geometryType =
                    FeatureCollectionPBuffer.GeometryType.valueOf(valueDescriptor);
            featureRestBuilder.setGeometryType(geometryType);
        }

        // Spatial Reference
        FeatureCollectionPBuffer.SpatialReference.Builder spatialReferenceBuilder =
                FeatureCollectionPBuffer.SpatialReference.newBuilder();
        if (flist.spatialReference instanceof SpatialReferenceWKID) {
            SpatialReferenceWKID spatialReference = (SpatialReferenceWKID) flist.spatialReference;
            spatialReferenceBuilder.setWkid(spatialReference.getWkid());
            spatialReferenceBuilder.setLastestWkid(spatialReference.getLatestWkid());
        } else if (flist.spatialReference instanceof SpatialReferenceWKT) {
            SpatialReferenceWKT spatialReference = (SpatialReferenceWKT) flist.spatialReference;
            spatialReferenceBuilder.setWkt(spatialReference.getWkt());
        }
        featureRestBuilder.setSpatialReference(spatialReferenceBuilder.build());

        // Transform
        CoordinateReferenceSystem crs;
        try {
            crs = SpatialReferences.fromSpatialReference(flist.spatialReference);
        } catch (FactoryException e) {
            throw new IllegalArgumentException("Unable to parse spatial reference", e);
        }
        FeatureCollectionPBuffer.Transform transformPBuffer = buildTransform(flist.transform, crs);
        featureRestBuilder.setTransform(transformPBuffer);

        // Fields
        flist.fields.forEach(
                field -> {
                    FeatureCollectionPBuffer.Field.Builder fieldBuilder =
                            FeatureCollectionPBuffer.Field.newBuilder();
                    FeatureCollectionPBuffer.FieldType fieldType =
                            FeatureCollectionPBuffer.FieldType.valueOf(field.getType());

                    fieldBuilder.setName(field.getName());
                    fieldBuilder.setFieldType(fieldType);
                    fieldBuilder.setAlias(field.getAlias());
                    featureRestBuilder.addFields(fieldBuilder.build());
                });

        // Features
        flist.features.forEach(
                feature -> {
                    FeatureCollectionPBuffer.Feature.Builder featureBuilder =
                            FeatureCollectionPBuffer.Feature.newBuilder();

                    // Add attributes
                    flist.fields.forEach(
                            field -> {
                                Object value = feature.getAttributes().get(field.getName());
                                Value.Builder valBuilder = Value.newBuilder();
                                if (value instanceof String) {
                                    valBuilder.setStringValue((String) value);
                                } else if (value instanceof Integer) {
                                    valBuilder.setUintValue((Integer) value);
                                } else if (value instanceof Double) {
                                    valBuilder.setDoubleValue((Double) value);
                                } else if (value instanceof Long) {
                                    valBuilder.setInt64Value((Long) value);
                                } else if (value instanceof Float) {
                                    valBuilder.setFloatValue((Float) value);
                                } else if (value instanceof Boolean) {
                                    valBuilder.setBoolValue((Boolean) value);
                                }
                                featureBuilder.addAttributes(valBuilder.build());
                            });

                    // Add geometry
                    if (feature.getGeometry() != null) {
                        FeatureCollectionPBuffer.Geometry geometry =
                                transformToPbfGeometry(feature.getGeometry(), transformPBuffer);
                        featureBuilder.setGeometry(geometry);
                    }
                    featureRestBuilder.addFeatures(featureBuilder.build());
                });
        return featureRestBuilder.build();
    }

    private static FeatureCollectionPBuffer.Transform buildTransform(
            Transform transform, CoordinateReferenceSystem crs) {
        FeatureCollectionPBuffer.Transform.Builder transformBuilder =
                FeatureCollectionPBuffer.Transform.newBuilder();

        FeatureCollectionPBuffer.Scale.Builder scaleBuilder =
                FeatureCollectionPBuffer.Scale.newBuilder();

        FeatureCollectionPBuffer.Translate.Builder translateBuilder =
                FeatureCollectionPBuffer.Translate.newBuilder();

        if (transform != null) {
            transformBuilder.setQuantizeOriginPostion(
                    FeatureCollectionPBuffer.QuantizeOriginPostion.valueOf(
                            transform.originPosition.toString()));
            scaleBuilder.setXScale(transform.scale[0]);
            scaleBuilder.setYScale(transform.scale[1]);
            translateBuilder.setXTranslate(transform.translate[0]);
            translateBuilder.setYTranslate(transform.translate[1]);
            transformBuilder.setScale(scaleBuilder.build());
            transformBuilder.setTranslate(translateBuilder.build());
        } else {
            Parser parser = new Parser();
            double xScale, yScale;

            if (crs instanceof GeographicCRS) {
                DefaultGeographicCRS geoCRS;
                try {
                    geoCRS = (DefaultGeographicCRS) parser.parseObject(crs.toWKT());
                } catch (ParseException e) {
                    throw new IllegalArgumentException("Unable to parse CRS", e);
                }
                Unit<?> unit = geoCRS.getCoordinateSystem().getAxis(0).getUnit();
                xScale = calculateScale(unit);
                yScale = xScale;
            } else if (crs instanceof ProjectedCRS) {
                DefaultProjectedCRS projCRS;
                try {
                    projCRS = (DefaultProjectedCRS) parser.parseObject(crs.toWKT());
                } catch (ParseException e) {
                    throw new IllegalArgumentException("Unable to parse CRS", e);
                }
                Unit<?> unit = projCRS.getCoordinateSystem().getAxis(0).getUnit();
                xScale = calculateScale(unit);
                yScale = xScale;
            } else {
                xScale = 1;
                yScale = 1;
            }

            scaleBuilder.setXScale(xScale);
            scaleBuilder.setYScale(yScale);
            translateBuilder.setXTranslate(0.0);
            translateBuilder.setYTranslate(0.0);
            transformBuilder.setScale(scaleBuilder.build());
            transformBuilder.setTranslate(translateBuilder.build());
        }
        return transformBuilder.build();
    }

    private static FeatureCollectionPBuffer.Geometry transformToPbfGeometry(
            Geometry geometry, FeatureCollectionPBuffer.Transform transform) {
        if (geometry == null) {
            return null;
        }

        switch (geometry.getGeometryType()) {
            case POINT:
                return transformPoint(geometry, transform);
            default:
                return transformGeometry(geometry, transform);
        }
    }

    private static FeatureCollectionPBuffer.Geometry transformPoint(
            Geometry geometry, FeatureCollectionPBuffer.Transform transform) {
        FeatureCollectionPBuffer.Geometry.Builder geometryBuilder =
                FeatureCollectionPBuffer.Geometry.newBuilder();

        Point point = (Point) geometry;
        List<Long> coords;
        List<Integer> lengths;

        double scaleX = transform.getScale().getXScale();
        double scaleY = transform.getScale().getYScale();
        double translateX = transform.getTranslate().getXTranslate();
        double translateY = transform.getTranslate().getYTranslate();
        long x = quantizeX(point.getX(), scaleX, translateX);
        long y = quantizeY(point.getY(), scaleY, translateY);
        coords = Arrays.asList(x, y);
        lengths = Arrays.asList(coords.size());

        geometryBuilder.addAllCoords(coords);
        geometryBuilder.addAllLengths(lengths);

        return geometryBuilder.build();
    }

    private static FeatureCollectionPBuffer.Geometry transformGeometry(
            Geometry geometry, FeatureCollectionPBuffer.Transform transform) {

        FeatureCollectionPBuffer.Geometry.Builder geometryBuilder =
                FeatureCollectionPBuffer.Geometry.newBuilder();
        Number[][][] paths;

        if (geometry instanceof Polyline) {
            paths = ((Polyline) geometry).getPaths();
        } else if (geometry instanceof Polygon) {
            paths = ((Polygon) geometry).getRings();
        } else if (geometry instanceof Multipoint) {
            paths = new Number[][][] {((Multipoint) geometry).getPoints()};
        } else {
            return geometryBuilder.build();
        }

        ArrayList<ArrayList<ArrayList<Long>>> finalResult = quantizePaths(paths, transform);

        List<Integer> lengths =
                finalResult.stream().map(arr -> arr.size()).collect(Collectors.toList());

        List<Long> finalResultFlattened =
                finalResult.stream()
                        .flatMap(Collection::stream)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());

        geometryBuilder.addAllCoords(finalResultFlattened);
        geometryBuilder.addAllLengths(lengths);

        return geometryBuilder.build();
    }

    private static ArrayList<ArrayList<ArrayList<Long>>> quantizePaths(
            Number[][][] paths, FeatureCollectionPBuffer.Transform transform) {
        ArrayList<ArrayList<ArrayList<Long>>> finalResult = new ArrayList<>();

        double scaleX = transform.getScale().getXScale();
        double scaleY = transform.getScale().getYScale();
        double translateX = transform.getTranslate().getXTranslate();
        double translateY = transform.getTranslate().getYTranslate();

        for (int i = 0; i < paths.length; i++) {
            ArrayList<ArrayList<Long>> result = new ArrayList<>();
            Number[][] path = paths[i];
            long x;
            long y;
            long prevx = 0;
            long prevy = 0;
            for (int j = 0; j < path.length; j++) {
                Number[] coords = path[j];
                if (j == 0) {
                    prevx = quantizeX(coords[0], scaleX, translateX);
                    prevy = quantizeY(coords[1], scaleY, translateY);
                    ArrayList<Long> newcoords = new ArrayList<>();
                    newcoords.add(prevx);
                    newcoords.add(prevy);
                    result.add(newcoords);
                } else {
                    x = quantizeX(coords[0], scaleX, translateX);
                    y = quantizeY(coords[1], scaleY, translateY);
                    if (x != prevx || y != prevy) {
                        ArrayList<Long> newcoords = new ArrayList<>();
                        newcoords.add(x - prevx);
                        newcoords.add(y - prevy);
                        result.add(newcoords);
                        prevx = x;
                        prevy = y;
                    }
                }
            }
            if (result.size() > 1) {
                finalResult.add(result);
            }
        }
        return finalResult;
    }

    private static long quantizeX(Number value, double scale, double translate) {
        return (long) Math.round((value.doubleValue() - translate) / scale);
    }

    private static long quantizeY(Number value, double scale, double translate) {
        return (long) Math.round((translate - value.doubleValue()) / scale);
    }

    private static double calculateScale(Unit<?> unit) {
        if (NonSI.DEGREE_ANGLE.equals(unit)) {
            return 1e-9;
        } else if (SI.METRE.equals(unit)) {
            return 1 / 10000;
        } else {
            return 1;
        }
    }
}
