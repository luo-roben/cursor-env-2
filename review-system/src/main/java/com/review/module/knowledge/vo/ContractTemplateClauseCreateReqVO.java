package com.review.module.knowledge.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractTemplateClauseCreateReqVO {

    @NotBlank(message = "条款编号不能为空")
    private String clauseId;

    private String clauseTitle;

    @NotBlank(message = "条款角色不能为空")
    private String clauseRole;

    private Integer clauseLevel;

    private String parentClauseId;

    @NotBlank(message = "条款原文不能为空")
    private String clauseText;

    private Integer isRequired;
    private String riskLevel;
    private String annotations;
    private Integer charOffsetStart;
    private Integer charOffsetEnd;
}
