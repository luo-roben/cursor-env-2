package com.review.module.agent.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.common.enums.CardCategory;
import com.review.module.agent.ReviewCardAgent;
import com.review.module.agent.dto.*;
import com.review.module.llm.model.ModelRouter;
import com.review.module.llm.prompt.PromptTemplateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SemanticComplianceAgent implements ReviewCardAgent {

    private final ModelRouter modelRouter;
    private final PromptTemplateManager promptTemplateManager;
    private final ObjectMapper objectMapper;

    @Override
    public CardCategory getCardCategory() {
        return CardCategory.SEMANTIC_COMPLIANCE;
    }

    @Override
    public List<ReviewIssue> review(ReviewContext context) {
        List<ReviewIssue> issues = new ArrayList<>();

        try {
            String lawArticlesStr = formatLawArticles(context.getLawArticles());
            String customRulesStr = formatCustomRules(context.getCustomRules());
            String casesStr = formatCases(context.getCases());

            Map<String, String> variables = new HashMap<>();
            variables.put("content", context.getContent() != null ? context.getContent() : "");
            variables.put("lawArticles", lawArticlesStr);
            variables.put("customRules", customRulesStr);
            variables.put("cases", casesStr);
            variables.put("documentType", context.getDocumentType() != null ? context.getDocumentType() : "OTHER");
            variables.put("contentType", context.getContentType() != null ? context.getContentType() : "");

            String prompt = promptTemplateManager.buildReviewPrompt(variables);
            String response = modelRouter.generate("mock", prompt);

            issues.addAll(parseResponse(response));
        } catch (Exception e) {
            log.error("SemanticComplianceAgent review failed", e);
        }

        return issues;
    }

    @Override
    public boolean isApplicable(ReviewContext context) {
        return true;
    }

    private List<ReviewIssue> parseResponse(String response) {
        List<ReviewIssue> issues = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode issuesNode = root.get("issues");
            if (issuesNode != null && issuesNode.isArray()) {
                for (JsonNode node : issuesNode) {
                    issues.add(ReviewIssue.builder()
                            .cardCategory(CardCategory.SEMANTIC_COMPLIANCE.getCode())
                            .verdict(getTextOrDefault(node, "verdict", "NEEDS_REVIEW"))
                            .confidence(node.has("confidence") ? node.get("confidence").asDouble() : 0.5)
                            .issueType(getTextOrDefault(node, "issueType", "semantic_compliance"))
                            .severity(getTextOrDefault(node, "severity", "MAJOR"))
                            .description(getTextOrDefault(node, "description", ""))
                            .suggestion(getTextOrDefault(node, "suggestion", ""))
                            .suggestionType(getTextOrDefault(node, "suggestionType", "TIP"))
                            .citedLawName(getTextOrDefault(node, "citedLawName", null))
                            .citedArticleCode(getTextOrDefault(node, "citedArticleCode", null))
                            .citationStatus("PENDING")
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse LLM response", e);
        }
        return issues;
    }

    private String getTextOrDefault(JsonNode node, String field, String defaultValue) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asText();
        }
        return defaultValue;
    }

    private String formatLawArticles(List<LawArticleInfo> articles) {
        if (articles == null || articles.isEmpty()) return "无相关法规";
        return articles.stream()
                .map(a -> "- " + a.getLawName() + " " + a.getArticleId() + ": " + a.getOriginalText())
                .collect(Collectors.joining("\n"));
    }

    private String formatCustomRules(List<CustomRuleInfo> rules) {
        if (rules == null || rules.isEmpty()) return "无自定义规则";
        return rules.stream()
                .map(r -> "- [" + r.getRuleType() + "] " + r.getContent())
                .collect(Collectors.joining("\n"));
    }

    private String formatCases(List<CaseInfo> cases) {
        if (cases == null || cases.isEmpty()) return "无参考案例";
        return cases.stream()
                .map(c -> "- 判定:" + c.getVerdict() + " 内容:" + c.getReviewedContent() + " 理由:" + c.getReason())
                .collect(Collectors.joining("\n"));
    }
}
