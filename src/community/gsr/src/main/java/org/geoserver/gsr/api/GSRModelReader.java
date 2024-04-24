/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.api;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import net.sf.json.*;
import org.apache.commons.io.IOUtils;
import org.geoserver.gsr.model.GSRModel;
import org.geoserver.gsr.model.feature.Feature;
import org.geoserver.gsr.model.feature.FeatureArray;
import org.geoserver.gsr.translate.feature.FeatureEncoder;
import org.geoserver.ows.Dispatcher;
import org.geoserver.rest.catalog.AvailableResources;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.geoserver.wfs.json.JSONType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

@Component
public class GSRModelReader extends BaseMessageConverter<GSRModel> {

    @Autowired GeoServicesJacksonJsonConverter converter;

    public static MediaType JSONP_MEDIA_TYPE = MediaType.parseMediaType(JSONType.jsonp);

    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(GSRModelReader.class);

    public GSRModelReader() {
        super(MediaType.APPLICATION_JSON, JSONP_MEDIA_TYPE);
    }

    @Override
    public int getPriority() {
        return super.getPriority() - 5;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return GSRModel.class.isAssignableFrom(clazz);
    }

    //
    // Reading
    //
    @Override
    protected boolean canWrite(MediaType mediaType) {
        if (mediaType.equals(JSONP_MEDIA_TYPE)) {
            return true;
        }
        return false;
    }

    protected void writeInternal(GSRModel model, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        MediaType contentType = outputMessage.getHeaders().getContentType();

        if (contentType.equals(JSONP_MEDIA_TYPE)) {
            OutputStream os = outputMessage.getBody();
            OutputStreamWriter outWriter = new OutputStreamWriter(os);

            final String callback = (String) Dispatcher.REQUEST.get().getKvp().get("CALLBACK");

            outWriter.write(callback + "(");
            outWriter.flush();
            converter.writeToOutputStream(os, model);
            outWriter.write(");");
            outWriter.close();
        }
    }

    //
    // writing
    //
    @Override
    protected GSRModel readInternal(Class<? extends GSRModel> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        IOUtils.copy(inputMessage.getBody(), bout);
        JSON json = JSONSerializer.toJSON(new String(bout.toByteArray()));
        if (FeatureArray.class.isAssignableFrom(clazz)) {
            if (json instanceof JSONArray) {
                List<Feature> features = new ArrayList<>();
                JSONArray jsonArray = (JSONArray) json;
                for (Object o : jsonArray) {
                    try {
                        features.add(FeatureEncoder.fromJson((JSONObject) o));
                    } catch (JSONException e) {
                        features.add(null);
                        LOGGER.log(
                                java.util.logging.Level.WARNING, "Error parsing json feature", e);
                    }
                }
                return new FeatureArray(features);
            }
        }
        throw new HttpMessageNotReadableException(
                this.getClass().getName() + " does not support deserialization", inputMessage);
    }
}
