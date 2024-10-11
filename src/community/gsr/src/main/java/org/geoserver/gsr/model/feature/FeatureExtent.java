package org.geoserver.gsr.model.feature;

import org.geoserver.gsr.model.GSRModel;
import org.geoserver.gsr.model.geometry.Envelope;

public class FeatureExtent implements GSRModel {

    private Envelope extent;
    private Integer count = null;

    public Envelope getExtent() {
        return extent;
    }

    public void setExtent(Envelope extent) {
        this.extent = extent;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public FeatureExtent(Envelope extent, Integer count) {
        this.extent = extent;
        this.count = count;
    }

    public FeatureExtent(Envelope extent) {
        this(extent, null);
    }
}
