package com.review.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Severity {

    CRITICAL("CRITICAL", "严重"),
    MAJOR("MAJOR", "重要"),
    MINOR("MINOR", "一般"),
    INFO("INFO", "提示");

    private final String value;
    private final String description;

    public static Severity fromValue(String value) {
        for (Severity severity : values()) {
            if (severity.getValue().equalsIgnoreCase(value)) {
                return severity;
            }
        }
        throw new IllegalArgumentException("Unknown Severity value: " + value);
    }
}
