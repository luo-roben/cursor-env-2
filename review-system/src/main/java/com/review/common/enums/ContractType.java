package com.review.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ContractType {

    PURCHASE("PURCHASE", "采购合同"),
    SALES("SALES", "销售合同"),
    SERVICE("SERVICE", "服务合同"),
    NDA("NDA", "保密协议"),
    EMPLOYMENT("EMPLOYMENT", "劳动合同"),
    EQUITY("EQUITY", "股权合同"),
    LEASE("LEASE", "租赁合同"),
    LOAN("LOAN", "借款合同"),
    GENERAL("GENERAL", "通用合同");

    private final String value;
    private final String description;

    public static ContractType fromValue(String value) {
        for (ContractType type : values()) {
            if (type.getValue().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ContractType value: " + value);
    }
}
