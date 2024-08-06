package org.geoserver.gsr.api;

import com.esri.arcgis.protobuf.FeatureCollection.FeatureCollectionPBuffer;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.logging.Logger;
import org.geoserver.gsr.model.GSRModel;
import org.geoserver.gsr.model.feature.FeatureCount;
import org.geoserver.gsr.model.feature.FeatureIdSet;
import org.geoserver.gsr.model.feature.FeatureList;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

@Component
public class GSRProtobufConverter extends BaseMessageConverter<GSRModel> {

    public static final String PBF = "application/x-protobuf";
    public static final MediaType PBF_MEDIA_TYPE = MediaType.parseMediaType(PBF);

    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(GSRProtobufConverter.class);

    public GSRProtobufConverter() {
        super(PBF_MEDIA_TYPE);
    }

    @Override
    public int getPriority() {
        return super.getPriority() - 5;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return GSRModel.class.isAssignableFrom(clazz);
    }

    @Override
    protected boolean canWrite(MediaType mediaType) {
        return super.canWrite(mediaType);
    }

    protected void writeInternal(GSRModel model, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        FeatureCollectionPBuffer.Builder fcBuilder = FeatureCollectionPBuffer.newBuilder();

        FeatureCollectionPBuffer.QueryResult.Builder fcQueryBuilder =
                FeatureCollectionPBuffer.QueryResult.newBuilder();

        if (model instanceof FeatureCount) {
            FeatureCount fc = (FeatureCount) model;
            FeatureCollectionPBuffer.CountResult countResult = buildCountResult(fc);

            fcQueryBuilder.setCountResult(countResult);
        } else if (model instanceof FeatureIdSet) {
            FeatureIdSet fids = (FeatureIdSet) model;
            FeatureCollectionPBuffer.ObjectIdsResult objectIdsResult = buildIdResult(fids);

            fcQueryBuilder.setIdsResult(objectIdsResult);
        } else if (model instanceof FeatureList) {
            FeatureList fl = (FeatureList) model;
            FeatureCollectionPBuffer.FeatureResult featureResult = buildFeatureResult(fl);

            fcQueryBuilder.setFeatureResult(featureResult);
        } else {
            LOGGER.warning("GSRModel is not a valid model and thus cannot build PBF QueryResult");
            throw new HttpMessageNotWritableException("Error encoding PBF FeatureCollection");
        }

        fcBuilder.setQueryResult(fcQueryBuilder);
        fcBuilder.setVersion("11.2");

        // Check if all required fields are populated
        if (!fcBuilder.isInitialized()) {
            LOGGER.warning("PBF FeatureCollection not initialized properly.");
            throw new HttpMessageNotWritableException("Error encoding PBF FeatureCollection");
        }

        outputMessage.getHeaders().setContentType(PBF_MEDIA_TYPE);
        OutputStream os = outputMessage.getBody();
        fcBuilder.build().writeTo(os);
        os.close();
    }

    private FeatureCollectionPBuffer.CountResult buildCountResult(FeatureCount fc) {
        FeatureCollectionPBuffer.CountResult.Builder countResultBuilder =
                FeatureCollectionPBuffer.CountResult.newBuilder();
        countResultBuilder.setCount(fc.getCount());
        return countResultBuilder.build();
    }

    private FeatureCollectionPBuffer.ObjectIdsResult buildIdResult(FeatureIdSet fids) {
        FeatureCollectionPBuffer.ObjectIdsResult.Builder objectIdsResultBuilder =
                FeatureCollectionPBuffer.ObjectIdsResult.newBuilder();
        objectIdsResultBuilder.setObjectIdFieldName(fids.getObjectIdFieldName());
        Arrays.stream(fids.getObjectIds()).forEach(objectIdsResultBuilder::addObjectIds);
        return objectIdsResultBuilder.build();
    }

    private FeatureCollectionPBuffer.FeatureResult buildFeatureResult(FeatureList flist) {
        return GSRFeatureResultPBF.buildFeatureResult(flist);
    }
}
