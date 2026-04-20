package com.review.module.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewContext {

    private Long taskId;
    private Long tenantId;
    private String content;
    private String documentType;
    private String contractType;
    private String contentType;
    private String productType;
    private String channel;
    private List<String> segments;
    private List<ClauseInfo> clauses;
    private List<LawArticleInfo> lawArticles;
    private List<CustomRuleInfo> customRules;
    private List<CaseInfo> cases;
    private List<TemplateDiffResult> templateDiffResults;
}
