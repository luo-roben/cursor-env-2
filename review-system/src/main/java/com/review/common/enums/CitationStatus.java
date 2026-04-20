package com.review.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CitationStatus {

    VERIFIED("VERIFIED", "已验证"),
    CORRECTED("CORRECTED", "已修正"),
    UNVERIFIED("UNVERIFIED", "未验证"),
    PENDING("PENDING", "待验证");

    private final String value;
    private final String description;

    public static CitationStatus fromValue(String value) {
        for (CitationStatus status : values()) {
            if (status.getValue().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown CitationStatus value: " + value);
    }
}
