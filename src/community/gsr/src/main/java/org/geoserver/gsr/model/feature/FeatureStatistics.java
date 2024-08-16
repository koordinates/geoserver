package org.geoserver.gsr.model.feature;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.json.*;
import org.geoserver.gsr.model.GSRModel;
import org.geoserver.gsr.model.geometry.*;
import org.geoserver.gsr.translate.feature.FeatureEncoder;
import org.geoserver.ogcapi.APIException;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.visitor.Aggregate;
import org.geotools.feature.visitor.GroupByVisitor;
import org.geotools.feature.visitor.GroupByVisitorBuilder;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.PropertyName;
import org.springframework.http.HttpStatus;

/**
 * Statistics List of {@link org.geoserver.gsr.model.feature.Feature}, that can be serialized as
 * JSON
 *
 * <p>See https://developers.arcgis.com/documentation/common-data-types/featureset-object.htm
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeatureStatistics implements GSRModel {

    public final String displayFieldName = "";

    public final Map<String, String> fieldAliases = new HashMap<>();

    public final ArrayList<Field> fields = new ArrayList<>();

    public final ArrayList<Feature> features = new ArrayList<>();

    public <T extends FeatureType, F extends org.opengis.feature.Feature> FeatureStatistics(
            FeatureCollection<T, F> collection,
            String groupByFieldsForStatistics,
            String outStatistics)
            throws IOException {

        T schema = collection.getSchema();

        // Parse the string into a JSON array
        JSON json = JSONSerializer.toJSON(outStatistics);
        JSONArray jsonArray = (JSONArray) json;
        String onStatisticField = jsonArray.getJSONObject(0).getString("onStatisticField");
        String outStatisticFieldName =
                jsonArray.getJSONObject(0).getString("outStatisticFieldName");
        StatisticTypeEnum statisticType =
                StatisticTypeEnum.fromValue(jsonArray.getJSONObject(0).getString("statisticType"));

        if (!onStatisticField.equals(groupByFieldsForStatistics)) {
            throw new APIException(
                    "InvalidOperation",
                    "Cannot perform operation on the given feature collection. "
                            + "groupByFieldsForStatistics must be the same as onStatisticField.",
                    HttpStatus.BAD_REQUEST);
        }

        // Add the fields to the response
        for (PropertyDescriptor desc : schema.getDescriptors()) {
            if (schema.getGeometryDescriptor() != null
                    && !desc.getName().equals(schema.getGeometryDescriptor().getName())
                    && desc.getName().toString().equals(onStatisticField)) {
                fields.add(FeatureEncoder.field(desc, null));
            }
        }
        fields.add(new Field(outStatisticFieldName, FieldTypeEnum.DOUBLE, outStatisticFieldName));

        fieldAliases.put(onStatisticField, onStatisticField);
        fieldAliases.put(outStatisticFieldName, outStatisticFieldName);

        FilterFactory factory = CommonFactoryFinder.getFilterFactory(null);
        PropertyName expr = factory.property(onStatisticField);
        GroupByVisitor visitor;
        switch (statisticType) {
            case COUNT:
                visitor = groupVisitor(expr, Aggregate.COUNT);
                break;
            case SUM:
                visitor = groupVisitor(expr, Aggregate.SUM);
                break;
            case MIN:
                visitor = groupVisitor(expr, Aggregate.MIN);
                break;
            case MAX:
                visitor = groupVisitor(expr, Aggregate.MAX);
                break;
            case AVERAGE:
                visitor = groupVisitor(expr, Aggregate.AVERAGE);
                break;
            case STD_DEV:
                visitor = groupVisitor(expr, Aggregate.STD_DEV);
                break;
            default:
                throw new APIException(
                        "InvalidStatisticType",
                        "Statistic type not supported: " + statisticType.value(),
                        HttpStatus.BAD_REQUEST);
        }
        try {
            collection.accepts(visitor, null);
        } catch (IOException e) {
            throw new APIException(
                    "InvalidOperation",
                    "Cannot perform operation on the given feature collection. " + e.getMessage(),
                    HttpStatus.BAD_REQUEST);
        }
        Map<String, Object> attributes;
        Map<List<Object>, Object> groupResult = visitor.getResult().toMap();

        for (Map.Entry<List<Object>, Object> entry : groupResult.entrySet()) {
            List<Object> key = entry.getKey();
            Object value = entry.getValue();

            attributes = new HashMap<>();
            attributes.put(onStatisticField, key.get(0));
            attributes.put(outStatisticFieldName, value);

            features.add(new Feature(null, attributes));
        }
    }

    private GroupByVisitor groupVisitor(PropertyName propName, Aggregate agg) throws IOException {
        GroupByVisitorBuilder builder = new GroupByVisitorBuilder();
        builder.withAggregateVisitor(agg);
        builder.withGroupByAttribute(propName);
        builder.withAggregateAttribute(propName);
        return builder.build();
    }
}
