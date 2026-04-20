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
public class ContractTemplateClauseRespVO {

    private Long id;
    private String templateId;
    private String clauseId;
    private String clauseTitle;
    private String clauseRole;
    private Integer clauseLevel;
    private String parentClauseId;
    private String clauseText;
    private Integer isRequired;
    private String riskLevel;
    private String annotations;
    private Integer charOffsetStart;
    private Integer charOffsetEnd;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
