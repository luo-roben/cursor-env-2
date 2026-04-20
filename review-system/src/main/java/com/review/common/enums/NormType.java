package com.review.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NormType {

    PROHIBITION("PROHIBITION", "禁止"),
    OBLIGATION("OBLIGATION", "义务"),
    RIGHT("RIGHT", "权利"),
    DEFINITION("DEFINITION", "定义"),
    PROCEDURE("PROCEDURE", "程序");

    private final String value;
    private final String description;

    public static NormType fromValue(String value) {
        for (NormType type : values()) {
            if (type.getValue().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown NormType value: " + value);
    }
}
