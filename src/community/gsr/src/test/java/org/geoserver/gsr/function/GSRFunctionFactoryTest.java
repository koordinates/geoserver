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
    public void testBasicQuery() throws Exception {
        String result = getAsString(query("cite", 0, "?f=json"));
        System.out.println("GSRFunctionFactoryTest.testBasicQuery() result = " + result);
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

    @Test
    public void testWhereNUMERIC() throws Exception {
        // ABS
        String result =
                getAsString(query("cite", 0, "?f=json&&where=CHAR_LENGTH(NAME)=ABS(-10.0)"));
        assertTrue(
                "Request with valid where clause; returned " + result,
                JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        JSONObject json = JSONObject.fromObject(result);
        assertTrue("Request with short envelope; returned " + result, json.containsKey("features"));

        // CEILING
        result = getAsString(query("cite", 0, "?f=json&&where=CHAR_LENGTH(NAME)=CEILING(9.7892)"));
        assertTrue(
                "Request with valid where clause; returned " + result,
                JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        assertTrue("Request with short envelope; returned " + result, json.containsKey("features"));

        // LOG10 (Custom) \ POWER
        result =
                getAsString(
                        query("cite", 0, "?f=json&&where=CHAR_LENGTH(NAME)=LOG10(POWER(10, 10))"));
        assertTrue(
                "Request with valid where clause; returned " + result,
                JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        assertTrue("Request with short envelope; returned " + result, json.containsKey("features"));

        // COS \ SIN \ TAN
        result =
                getAsString(
                        query("cite", 0, "?f=json&&where=CHAR_LENGTH(NAME)=COS(SIN(TAN(3.141)))"));
        assertTrue(
                "Request with valid where clause; returned " + result,
                JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        assertTrue("Request with short envelope; returned " + result, json.containsKey("features"));

        // MOD
        result = getAsString(query("cite", 0, "?f=json&&where=CHAR_LENGTH(NAME)=MOD(42, 16)"));
        assertTrue(
                "Request with valid where clause; returned " + result,
                JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        assertTrue("Request with short envelope; returned " + result, json.containsKey("features"));

        // FLOOR
        result = getAsString(query("cite", 0, "?f=json&&where=CHAR_LENGTH(NAME)=FLOOR(10.3232)"));
        assertTrue(
                "Request with valid where clause; returned " + result,
                JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        assertTrue("Request with short envelope; returned " + result, json.containsKey("features"));
    }

    @Test
    public void testWhereDATE() throws Exception {
        String result = getAsString(query("cite", 0, "?f=json&&where=NAME=CURRENT_TIME()"));
        assertTrue(
                "Request with valid where clause; returned " + result,
                JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        JSONObject json = JSONObject.fromObject(result);
        assertTrue("Request with short envelope; returned " + result, json.containsKey("features"));
    }
}
