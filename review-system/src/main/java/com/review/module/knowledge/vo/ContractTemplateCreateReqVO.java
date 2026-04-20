package com.review.module.knowledge.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractTemplateCreateReqVO {

    @NotBlank(message = "模板ID不能为空")
    private String templateId;

    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @NotBlank(message = "合同类型不能为空")
    private String contractType;

    private String industry;

    @NotBlank(message = "来源不能为空")
    private String source;

    private String sourceName;
    private String version;
    private Long tenantId;
    private Integer totalClauses;
    private Integer totalChars;
    private String parties;
    private String requiredClauseRoles;
    private String applicableScenarios;
}
