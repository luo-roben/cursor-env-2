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
public class LawArticleCreateReqVO {

    @NotNull(message = "法规来源ID不能为空")
    private Long sourceId;

    @NotBlank(message = "法律名称不能为空")
    private String lawName;

    private String lawShortName;

    @NotBlank(message = "法条编号不能为空")
    private String articleId;

    @NotBlank(message = "法条原文不能为空")
    private String originalText;

    @NotBlank(message = "规范类型不能为空")
    private String normType;

    private String subject;
    private String behavior;
    private String objectDesc;
    private String applicableCondition;
    private String applicableScenarios;
    private String applicableContentTypes;
    private String applicableProductTypes;
    private String applicableDocTypes;
    private String contractClauseRole;
    private String contractTypes;
    private String keyPhrases;
    private String semanticExtensions;
    private String violationExamples;
    private String compliantExamples;
    private String penalty;
    private String relatedArticles;
    private Integer authorityLevel;
}
