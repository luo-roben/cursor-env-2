package com.review.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RuleType {

    BANNED_WORD("BANNED_WORD", "禁用词"),
    REQUIRED_STATEMENT("REQUIRED_STATEMENT", "必备声明"),
    PRODUCT_INFO("PRODUCT_INFO", "产品信息"),
    NATURAL_LANGUAGE("NATURAL_LANGUAGE", "自然语言规则"),
    CONTRACT_CLAUSE_TEMPLATE("CONTRACT_CLAUSE_TEMPLATE", "合同条款模板");

    private final String value;
    private final String description;

    public static RuleType fromValue(String value) {
        for (RuleType type : values()) {
            if (type.getValue().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown RuleType value: " + value);
    }
}
