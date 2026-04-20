package com.review.module.knowledge.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractTemplateRespVO {

    private Long id;
    private String templateId;
    private String templateName;
    private String contractType;
    private String industry;
    private String source;
    private String sourceName;
    private String version;
    private String status;
    private Long tenantId;
    private Integer totalClauses;
    private Integer totalChars;
    private String parties;
    private String requiredClauseRoles;
    private String applicableScenarios;
    private Long confirmedBy;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
