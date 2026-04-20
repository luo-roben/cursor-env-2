package com.review.module.context.dto;

import com.review.module.agent.dto.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssembledContext {

    private List<String> segments;
    private List<LawArticleInfo> relevantArticles;
    private List<CustomRuleInfo> customRules;
    private List<CaseInfo> cases;
    private List<TemplateDiffResult> templateDiffResults;
    private String assembledPrompt;
}
