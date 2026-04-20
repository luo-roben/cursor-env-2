package com.review.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SuggestionType {

    TIP("TIP", "纯提示"),
    REVISION("REVISION", "可采纳修订"),
    NEGOTIATION_POINT("NEGOTIATION_POINT", "协商点建议");

    private final String value;
    private final String description;

    public static SuggestionType fromValue(String value) {
        for (SuggestionType type : values()) {
            if (type.getValue().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown SuggestionType value: " + value);
    }
}
