package com.review.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReviewStatus {

    PENDING("PENDING", "待审查"),
    REVIEWING("REVIEWING", "审查中"),
    COMPLETED("COMPLETED", "已完成"),
    HUMAN_REVIEWED("HUMAN_REVIEWED", "已人工复核");

    private final String value;
    private final String description;

    public static ReviewStatus fromValue(String value) {
        for (ReviewStatus status : values()) {
            if (status.getValue().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ReviewStatus value: " + value);
    }
}
