package com.compliance.module.checklist.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChecklistRespVO {

    private Long id;
    private String contentType;
    private String productType;
    private String checkItem;
    private String checkMethod;
    private Long lawArticleId;
    private String conditionDesc;
    private String severityIfMissing;
    private Long tenantId;
    private Boolean enabled;
    private LocalDateTime createdAt;
}
