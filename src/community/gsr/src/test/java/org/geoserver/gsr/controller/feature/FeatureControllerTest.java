/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.controller.feature;

import static org.junit.Assert.assertTrue;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.geoserver.gsr.controller.ControllerTest;

public class FeatureControllerTest extends ControllerTest {
    private String query(String service, String layer, String feature, String params) {
        return getBaseURL()
                + service
                + "/layer2/FeatureServer/"
                + layer
                + "/"
                // + feature
                + params;
    }
    // TODO: Need to figure out the feature ID for the layernames
    // @Test
    // public void testBasicQuery() throws Exception {
    //     String q = query("cite", "0", "1107531599613", "query?f=json");
    //     JSON result = getAsJSON(q);
    //     checkResult(result);
    // }

    // @Test
    // public void testBasicQueryPJson() throws Exception {
    //     String q = query("cite", "0", "1107531599613", "?f=pjson");
    //     MockHttpServletResponse response = getAsServletResponse(q);
    //     assertEquals(response.getContentType(), "application/json");
    //     JSON result = json(response);
    //     checkResult(result);
    // }

    private void checkResult(JSON result) {
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);
        // result should have a field named 'feature' containing an object with 'geometry' and
        // 'attributes' fields.
        JSONObject json = (JSONObject) result;
        Object featureObject = ((JSONObject) result).get("feature");
        System.out.println(featureObject);
        assertTrue(
                "feature field should contain an object" + json,
                featureObject instanceof JSONObject);
        JSONObject feature = (JSONObject) featureObject;
        assertTrue("feature should contain attributes" + json, feature.containsKey("attributes"));
        assertTrue("feature should contain geometry" + json, feature.containsKey("geometry"));
    }
}
