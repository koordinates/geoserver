/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

 package org.geoserver.gsr.model.feature;

 import org.geoserver.gsr.model.GSRModel;
 
 public class FeatureCount implements GSRModel {

    private int count;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public FeatureCount(int count) {
        super();
        this.count = count;
    }
}
