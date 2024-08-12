package org.geoserver.gsr.model.feature;

import com.fasterxml.jackson.annotation.JsonValue;

public enum StatisticTypeEnum {
    COUNT("count"),
    SUM("sum"),
    MIN("min"),
    MAX("max"),
    AVERAGE("avg"),
    STD_DEV("stddev");

    private final String statisticType;

    public String getStatisticType() {
        return statisticType;
    }

    StatisticTypeEnum(String statType) {
        this.statisticType = statType;
    }

    public static StatisticTypeEnum fromValue(String value) {
        for (StatisticTypeEnum type : StatisticTypeEnum.values()) {
            if (type.statisticType.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown statistic type: " + value);
    }

    @JsonValue
    public String value() {
        return this.statisticType;
    }
}
