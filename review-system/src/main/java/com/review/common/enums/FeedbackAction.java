package com.review.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FeedbackAction {

    CONFIRMED("CONFIRMED", "确认"),
    REJECTED("REJECTED", "驳回"),
    MODIFIED("MODIFIED", "修改"),
    SUPPLEMENTED("SUPPLEMENTED", "补充");

    private final String value;
    private final String description;

    public static FeedbackAction fromValue(String value) {
        for (FeedbackAction action : values()) {
            if (action.getValue().equalsIgnoreCase(value)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown FeedbackAction value: " + value);
    }
}
