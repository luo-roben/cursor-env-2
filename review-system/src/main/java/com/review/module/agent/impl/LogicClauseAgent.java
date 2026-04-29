package com.review.module.agent.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.common.enums.CardCategory;
import com.review.module.agent.ReviewCardAgent;
import com.review.module.agent.dto.ClauseInfo;
import com.review.module.agent.dto.ReviewContext;
import com.review.module.agent.dto.ReviewIssue;
import com.review.module.llm.cache.PromptCacheService;
import com.review.module.llm.model.ModelRouter;
import com.review.module.llm.prompt.PromptTemplateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class LogicClauseAgent implements ReviewCardAgent {

    private final ModelRouter modelRouter;
    private final PromptTemplateManager promptTemplateManager;
    private final ObjectMapper objectMapper;
    private final PromptCacheService promptCacheService;

    @Value("${review.llm.default-model:mock}")
    private String defaultModel;

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("([零壹贰叁肆伍陆柒捌玖拾佰仟万亿]+元|\\d+(?:\\.\\d+)?\\s*(?:元|万元|亿元))");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4}年\\d{1,2}月\\d{1,2}日|\\d{4}-\\d{2}-\\d{2})");

    @Override
    public CardCategory getCardCategory() {
        return CardCategory.LOGIC_CLAUSES;
    }

    @Override
    public List<ReviewIssue> review(ReviewContext context) {
        List<ReviewIssue> issues = new ArrayList<>();

        if (context.getClauses() == null || context.getClauses().isEmpty()) {
            return issues;
        }

        issues.addAll(checkRightsObligationsSymmetry(context.getClauses()));
        issues.addAll(checkAmountConsistency(context.getClauses()));
        issues.addAll(checkTimelineConsistency(context.getClauses()));

        try {
            issues.addAll(llmClauseAnalysis(context));
        } catch (Exception e) {
            log.error("LLM clause analysis failed", e);
        }

        return issues;
    }

    @Override
    public boolean isApplicable(ReviewContext context) {
        return "CONTRACT".equalsIgnoreCase(context.getDocumentType())
                && context.getClauses() != null
                && !context.getClauses().isEmpty();
    }

    private List<ReviewIssue> checkRightsObligationsSymmetry(List<ClauseInfo> clauses) {
        List<ReviewIssue> issues = new ArrayList<>();

        long rightsCount = clauses.stream()
                .filter(c -> "RIGHT".equalsIgnoreCase(c.getClauseType()))
                .count();
        long obligationsCount = clauses.stream()
                .filter(c -> "OBLIGATION".equalsIgnoreCase(c.getClauseType()))
                .count();

        if (rightsCount > 0 && obligationsCount > 0) {
            double ratio = (double) rightsCount / obligationsCount;
            if (ratio > 3.0 || ratio < 0.33) {
                issues.add(ReviewIssue.builder()
                        .cardCategory(CardCategory.LOGIC_CLAUSES.getCode())
                        .verdict("NEEDS_REVIEW")
                        .confidence(0.7)
                        .issueType("rights_obligations_asymmetry")
                        .severity("MAJOR")
                        .description("权利条款(" + rightsCount + "条)与义务条款(" + obligationsCount + "条)比例严重失衡")
                        .suggestion("建议检查合同权责对等性")
                        .suggestionType("NEGOTIATION_POINT")
                        .build());
            }
        }

        return issues;
    }

    private List<ReviewIssue> checkAmountConsistency(List<ClauseInfo> clauses) {
        List<ReviewIssue> issues = new ArrayList<>();
        Map<String, Set<String>> clauseAmounts = new LinkedHashMap<>();

        for (ClauseInfo clause : clauses) {
            if (clause.getClauseText() == null) continue;
            Matcher matcher = AMOUNT_PATTERN.matcher(clause.getClauseText());
            Set<String> amounts = new HashSet<>();
            while (matcher.find()) {
                amounts.add(matcher.group(1));
            }
            if (!amounts.isEmpty()) {
                clauseAmounts.put(clause.getClauseNumber(), amounts);
            }
        }

        if (clauseAmounts.size() > 1) {
            Set<String> allAmounts = clauseAmounts.values().stream()
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());
            if (allAmounts.size() > clauseAmounts.size() * 2) {
                issues.add(ReviewIssue.builder()
                        .cardCategory(CardCategory.LOGIC_CLAUSES.getCode())
                        .verdict("NEEDS_REVIEW")
                        .confidence(0.6)
                        .issueType("amount_inconsistency")
                        .severity("MINOR")
                        .description("多个条款中涉及金额数量较多，请确认各条款金额一致性")
                        .suggestion("请核实合同中各处金额是否一致")
                        .suggestionType("TIP")
                        .build());
            }
        }

        return issues;
    }

    private List<ReviewIssue> checkTimelineConsistency(List<ClauseInfo> clauses) {
        List<ReviewIssue> issues = new ArrayList<>();
        List<String> allDates = new ArrayList<>();

        for (ClauseInfo clause : clauses) {
            if (clause.getClauseText() == null) continue;
            Matcher matcher = DATE_PATTERN.matcher(clause.getClauseText());
            while (matcher.find()) {
                allDates.add(matcher.group(1));
            }
        }

        if (allDates.size() > 3) {
            issues.add(ReviewIssue.builder()
                    .cardCategory(CardCategory.LOGIC_CLAUSES.getCode())
                    .verdict("NEEDS_REVIEW")
                    .confidence(0.5)
                    .issueType("timeline_review")
                    .severity("INFO")
                    .description("合同包含" + allDates.size() + "处日期，请确认时间线的逻辑一致性")
                    .suggestion("请确认各条款中的日期符合业务时间线逻辑")
                    .suggestionType("TIP")
                    .build());
        }

        return issues;
    }

    private List<ReviewIssue> llmClauseAnalysis(ReviewContext context) {
        List<ReviewIssue> issues = new ArrayList<>();

        String clauseText = context.getClauses().stream()
                .map(c -> c.getClauseNumber() + " " + (c.getClauseTitle() != null ? c.getClauseTitle() : "") + ": " + c.getClauseText())
                .collect(Collectors.joining("\n"));

        Map<String, String> variables = new HashMap<>();
        variables.put("clauseText", clauseText);
        variables.put("contractType", context.getContractType() != null ? context.getContractType() : "GENERAL");
        variables.put("lawArticles", "");
        variables.put("customRules", "");

        String prompt = promptTemplateManager.buildContractReviewPrompt(variables);
        String promptHash = promptCacheService.computeHash(prompt);
        String cacheKey = "prompt:clause:" + promptHash;
        String cachedResponse = promptCacheService.getCachedResponse(cacheKey);
        String response;
        if (cachedResponse != null) {
            log.debug("Cache hit for LogicClauseAgent prompt");
            response = cachedResponse;
        } else {
            response = modelRouter.generate(defaultModel, prompt);
            promptCacheService.cacheResponse(cacheKey, response, promptCacheService.getDefaultTtl());
        }

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode issuesNode = root.get("issues");
            if (issuesNode != null && issuesNode.isArray()) {
                for (JsonNode node : issuesNode) {
                    issues.add(ReviewIssue.builder()
                            .cardCategory(CardCategory.LOGIC_CLAUSES.getCode())
                            .verdict(node.has("verdict") ? node.get("verdict").asText() : "NEEDS_REVIEW")
                            .confidence(node.has("confidence") ? node.get("confidence").asDouble() : 0.5)
                            .issueType(node.has("issueType") ? node.get("issueType").asText() : "clause_logic")
                            .severity(node.has("severity") ? node.get("severity").asText() : "MAJOR")
                            .description(node.has("description") ? node.get("description").asText() : "")
                            .suggestion(node.has("suggestion") ? node.get("suggestion").asText() : "")
                            .suggestionType(node.has("suggestionType") ? node.get("suggestionType").asText() : "TIP")
                            .citedLawName(node.has("citedLawName") ? node.get("citedLawName").asText() : null)
                            .citedArticleCode(node.has("citedArticleCode") ? node.get("citedArticleCode").asText() : null)
                            .citationStatus("PENDING")
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse LLM clause response", e);
        }

        return issues;
    }
}
