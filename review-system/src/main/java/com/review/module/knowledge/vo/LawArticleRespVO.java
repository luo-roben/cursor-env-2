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
public class LawArticleRespVO {

    private Long id;
    private Long sourceId;
    private String lawName;
    private String lawShortName;
    private String articleId;
    private String originalText;
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
    private String status;
    private Long confirmedBy;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
