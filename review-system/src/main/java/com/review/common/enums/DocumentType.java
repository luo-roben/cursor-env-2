package com.review.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DocumentType {

    MARKETING("MARKETING", "营销物料"),
    CONTRACT("CONTRACT", "合同"),
    PROSPECTUS("PROSPECTUS", "招募说明书"),
    REPORT("REPORT", "报告"),
    OTHER("OTHER", "其他");

    private final String value;
    private final String description;

    public static DocumentType fromValue(String value) {
        for (DocumentType type : values()) {
            if (type.getValue().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown DocumentType value: " + value);
    }
}
