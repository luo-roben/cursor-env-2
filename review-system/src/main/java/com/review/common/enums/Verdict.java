package com.review.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Verdict {

    VIOLATION("VIOLATION", "违规"),
    COMPLIANT("COMPLIANT", "合规"),
    NEEDS_REVIEW("NEEDS_REVIEW", "待复核");

    private final String value;
    private final String description;

    public static Verdict fromValue(String value) {
        for (Verdict verdict : values()) {
            if (verdict.getValue().equalsIgnoreCase(value)) {
                return verdict;
            }
        }
        throw new IllegalArgumentException("Unknown Verdict value: " + value);
    }
}
