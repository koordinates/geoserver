package org.geoserver.gsr.controller.map;

import static org.junit.Assert.assertTrue;

import net.sf.json.JSONObject;
import org.geoserver.gsr.JsonSchemaTest;
import org.geoserver.gsr.controller.ControllerTest;
import org.junit.Test;

public class GSRFunctionFactoryTest extends ControllerTest {
    private String query(String service, int layerId, String params) {
        return getBaseURL() + service + "/Streams/FeatureServer/" + layerId + "/query" + params;
    }

    @Test
    public void testWhereLOWER() throws Exception {
        String result =
                getAsString(
                        query(
                                "cite",
                                0,
                                "?f=json&geometryType=esriGeometryEnvelope&geometry=-180,-90,180,90&where=LOWER(NAME)=\'cam+stream\'"));
        assertTrue(
                "Request with valid where clause; returned " + result,
                JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        JSONObject json = JSONObject.fromObject(result);
        assertTrue("Request with short envelope; returned " + result, json.containsKey("features"));
    }

    @Test
    public void testWhereUPPER() throws Exception {
        String result =
                getAsString(
                        query(
                                "cite",
                                0,
                                "?f=json&geometryType=esriGeometryEnvelope&geometry=-180,-90,180,90&where=UPPER(NAME)=\'CAM+STREAM\'"));
        assertTrue(
                "Request with valid where clause; returned " + result,
                JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        JSONObject json = JSONObject.fromObject(result);
        assertTrue("Request with short envelope; returned " + result, json.containsKey("features"));
    }

    @Test
    public void testWhereSUBSTRING() throws Exception {
        // Also tests POSITION and CHAR_LENGTH
        String result =
                getAsString(
                        query(
                                "cite",
                                0,
                                "?f=json&geometryType=esriGeometryEnvelope&geometry=-180,-90,180,90&where=NAME=SUBSTRING(\'testCam+Stream\', POSITION(\'Cam+Stream\', \'testCam+Stream\'), CHAR_LENGTH(\'Cam+Stream\'))"));
        assertTrue(
                "Request with valid where clause; returned " + result,
                JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        JSONObject json = JSONObject.fromObject(result);
        assertTrue("Request with short envelope; returned " + result, json.containsKey("features"));
    }

    @Test
    public void testWhereCONCAT() throws Exception {
        // Test LOWER()
        String result =
                getAsString(
                        query(
                                "cite",
                                0,
                                "?f=json&geometryType=esriGeometryEnvelope&geometry=-180,-90,180,90&where=NAME=CONCAT(\'Cam+\', \'Stream\')"));
        assertTrue(
                "Request with valid where clause; returned " + result,
                JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        JSONObject json = JSONObject.fromObject(result);
        assertTrue("Request with short envelope; returned " + result, json.containsKey("features"));
    }
}
