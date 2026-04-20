package com.review.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CardCategory {

    TEXT_BASICS(1, "文本基础核对"),
    REDLINES(2, "红线与敏感词"),
    FORMAT_ELEMENTS(3, "形式与要素审查"),
    SEMANTIC_COMPLIANCE(4, "语义与合规审查"),
    LOGIC_CLAUSES(5, "逻辑与条款审查");

    private final int code;
    private final String title;

    public String getTitle() {
        return title;
    }

    public static CardCategory fromValue(int code) {
        for (CardCategory category : values()) {
            if (category.getCode() == code) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown CardCategory code: " + code);
    }
}
