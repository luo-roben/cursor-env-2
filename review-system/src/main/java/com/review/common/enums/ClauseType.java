package com.review.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ClauseType {

    DEFINITION("DEFINITION", "定义条款"),
    OBLIGATION("OBLIGATION", "义务条款"),
    RIGHT("RIGHT", "权利条款"),
    LIABILITY("LIABILITY", "违约条款"),
    TERMINATION("TERMINATION", "终止条款"),
    DISPUTE("DISPUTE", "争议解决条款"),
    GENERAL("GENERAL", "一般条款");

    private final String value;
    private final String description;

    public static ClauseType fromValue(String value) {
        for (ClauseType type : values()) {
            if (type.getValue().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ClauseType value: " + value);
    }
}
