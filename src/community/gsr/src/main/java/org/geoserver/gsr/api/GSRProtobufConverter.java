/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.api;

import com.esri.arcgis.protobuf.FeatureCollection;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import org.geoserver.gsr.model.GSRModel;
import org.geoserver.gsr.model.feature.FeatureCount;
import org.geoserver.gsr.model.feature.FeatureIdSet;
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

        outputMessage.getHeaders().setContentType(PBF_MEDIA_TYPE);

        FeatureCollection.FeatureCollectionPBuffer.Builder fcBuilder =
                FeatureCollection.FeatureCollectionPBuffer.newBuilder();

        FeatureCollection.FeatureCollectionPBuffer.QueryResult.Builder fcQueryBuilder =
                FeatureCollection.FeatureCollectionPBuffer.QueryResult.newBuilder();

        if (model instanceof FeatureCount) {
            LOGGER.debug("GSRModel is FeatureCount, building count result");
            FeatureCount fc = (FeatureCount) model;
            FeatureCollection.FeatureCollectionPBuffer.CountResult countResult =
                    buildCountResult(fc);

            fcQueryBuilder.setCountResult(countResult);
        } else if (model instanceof FeatureIdSet) {
            // TODO: Implement building of idsResult
            LOGGER.warning("IdResult not implemented yet.");
        } else {
            // TODO: Implement building of featureResult
            LOGGER.warning("FeatureResult not implemented yet.");
        }

        fcBuilder.setQueryResult(fcQueryBuilder);
        fcBuilder.setVersion("1.2.3");

        // Check if all required fields are populated
        if (!fcBuilder.isInitialized()) {
            LOGGER.warning("PBF FeatureCollection not initialized properly.");
            throw new HttpMessageNotWritableException("Error encoding PBF FeatureCollection");
        }

        OutputStream os = outputMessage.getBody();
        fcBuilder.build().writeTo(os);
        LOGGER.info("PBF Output Message (Human Readable): " + fcBuilder.build().toString());
        os.close();
    }

    private FeatureCollection.FeatureCollectionPBuffer.CountResult buildCountResult(
            FeatureCount fc) {
        FeatureCollection.FeatureCollectionPBuffer.CountResult.Builder countResultBuilder =
                FeatureCollection.FeatureCollectionPBuffer.CountResult.newBuilder();
        countResultBuilder.setCount(fc.getCount());
        return countResultBuilder.build();
    }
}
