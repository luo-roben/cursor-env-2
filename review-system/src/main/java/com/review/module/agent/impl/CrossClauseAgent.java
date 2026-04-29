package com.review.module.agent.impl;

import com.review.common.enums.CardCategory;
import com.review.module.agent.ReviewCardAgent;
import com.review.module.agent.dto.ClauseInfo;
import com.review.module.agent.dto.ReviewContext;
import com.review.module.agent.dto.ReviewIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@Order(2)
public class CrossClauseAgent implements ReviewCardAgent {

    private static final Pattern AMOUNT_PATTERN =
            Pattern.compile("([零壹贰叁肆伍陆柒捌玖拾佰仟万亿]+元|\\d+(?:\\.\\d+)?\\s*(?:元|万元|亿元))");

    private static final Pattern DATE_PATTERN =
            Pattern.compile("(\\d{4}年\\d{1,2}月\\d{1,2}日|\\d{4}-\\d{2}-\\d{2})");

    private static final Pattern PERIOD_PATTERN =
            Pattern.compile("(\\d+)\\s*(?:个?天|日|个?工作日|个?月|年)");

    @Override
    public CardCategory getCardCategory() {
        return CardCategory.LOGIC_CLAUSES;
    }

    @Override
    public boolean isApplicable(ReviewContext context) {
        return "CONTRACT".equalsIgnoreCase(context.getDocumentType())
                && context.getClauses() != null
                && !context.getClauses().isEmpty()
                && context.getContent() != null
                && context.getContent().length() > 200;
    }

    @Override
    public List<ReviewIssue> review(ReviewContext context) {
        List<ReviewIssue> issues = new ArrayList<>();

        issues.addAll(checkRightsObligationsSymmetry(context.getClauses()));
        issues.addAll(checkTimelineConsistency(context.getClauses()));
        issues.addAll(checkAmountConsistency(context.getClauses()));
        issues.addAll(checkDisputeResolutionCompleteness(context.getContent(), context.getClauses()));
        issues.addAll(checkTerminationClause(context.getContent(), context.getClauses()));

        return issues;
    }

    private List<ReviewIssue> checkRightsObligationsSymmetry(List<ClauseInfo> clauses) {
        List<ReviewIssue> issues = new ArrayList<>();

        Map<String, Long> typeCounts = new HashMap<>();
        for (ClauseInfo clause : clauses) {
            String type = clause.getClauseType() != null ? clause.getClauseType().toUpperCase() : "GENERAL";
            typeCounts.merge(type, 1L, Long::sum);
        }

        long rights = typeCounts.getOrDefault("RIGHT", 0L);
        long obligations = typeCounts.getOrDefault("OBLIGATION", 0L);

        if (rights > 0 && obligations > 0) {
            double ratio = (double) rights / obligations;
            if (ratio > 2.0) {
                issues.add(ReviewIssue.builder()
                        .cardCategory(CardCategory.LOGIC_CLAUSES.getCode())
                        .verdict("NEEDS_REVIEW")
                        .confidence(0.75)
                        .issueType("cross_clause_rights_obligations_asymmetry")
                        .severity("MAJOR")
                        .description("跨条款分析：权利条款(" + rights + "条)远多于义务条款(" + obligations + "条)，权责不对等")
                        .suggestion("建议审查合同整体权责平衡，确保双方权利义务对等")
                        .suggestionType("NEGOTIATION_POINT")
                        .build());
            } else if (ratio < 0.5) {
                issues.add(ReviewIssue.builder()
                        .cardCategory(CardCategory.LOGIC_CLAUSES.getCode())
                        .verdict("NEEDS_REVIEW")
                        .confidence(0.75)
                        .issueType("cross_clause_rights_obligations_asymmetry")
                        .severity("MAJOR")
                        .description("跨条款分析：义务条款(" + obligations + "条)远多于权利条款(" + rights + "条)，权责不对等")
                        .suggestion("建议审查合同整体权责平衡，确保双方权利义务对等")
                        .suggestionType("NEGOTIATION_POINT")
                        .build());
            }
        } else if (obligations > 0 && rights == 0) {
            issues.add(ReviewIssue.builder()
                    .cardCategory(CardCategory.LOGIC_CLAUSES.getCode())
                    .verdict("NEEDS_REVIEW")
                    .confidence(0.65)
                    .issueType("cross_clause_missing_rights")
                    .severity("MAJOR")
                    .description("跨条款分析：合同仅包含义务条款(" + obligations + "条)而无权利条款")
                    .suggestion("建议补充权利条款以确保权责对等")
                    .suggestionType("NEGOTIATION_POINT")
                    .build());
        }

        return issues;
    }

    private List<ReviewIssue> checkTimelineConsistency(List<ClauseInfo> clauses) {
        List<ReviewIssue> issues = new ArrayList<>();
        List<String> allDates = new ArrayList<>();
        Map<String, List<Integer>> clausePeriods = new LinkedHashMap<>();

        for (ClauseInfo clause : clauses) {
            if (clause.getClauseText() == null) continue;

            Matcher dateMatcher = DATE_PATTERN.matcher(clause.getClauseText());
            while (dateMatcher.find()) {
                allDates.add(dateMatcher.group(1));
            }

            Matcher periodMatcher = PERIOD_PATTERN.matcher(clause.getClauseText());
            List<Integer> periods = new ArrayList<>();
            while (periodMatcher.find()) {
                try {
                    periods.add(Integer.parseInt(periodMatcher.group(1)));
                } catch (NumberFormatException ignored) {
                }
            }
            if (!periods.isEmpty()) {
                clausePeriods.put(
                        clause.getClauseNumber() != null ? clause.getClauseNumber() : "unknown",
                        periods);
            }
        }

        if (clausePeriods.size() >= 2) {
            List<Integer> allPeriods = clausePeriods.values().stream()
                    .flatMap(List::stream)
                    .sorted()
                    .toList();

            if (allPeriods.size() >= 2) {
                int min = allPeriods.getFirst();
                int max = allPeriods.getLast();
                if (max > 0 && min > 0 && max > min * 10) {
                    issues.add(ReviewIssue.builder()
                            .cardCategory(CardCategory.LOGIC_CLAUSES.getCode())
                            .verdict("NEEDS_REVIEW")
                            .confidence(0.7)
                            .issueType("cross_clause_timeline_conflict")
                            .severity("MAJOR")
                            .description("跨条款时间线冲突：各条款期限差异过大（最短" + min + "天/月 vs 最长" + max + "天/月），可能存在履行期限矛盾")
                            .suggestion("请核实各条款中的时间期限是否存在逻辑矛盾")
                            .suggestionType("TIP")
                            .build());
                }
            }
        }

        if (allDates.size() > 5) {
            Set<String> uniqueDates = new HashSet<>(allDates);
            if (uniqueDates.size() > 4) {
                issues.add(ReviewIssue.builder()
                        .cardCategory(CardCategory.LOGIC_CLAUSES.getCode())
                        .verdict("NEEDS_REVIEW")
                        .confidence(0.55)
                        .issueType("cross_clause_timeline_complexity")
                        .severity("MINOR")
                        .description("跨条款分析：合同涉及" + uniqueDates.size() + "个不同日期，时间线较复杂")
                        .suggestion("建议梳理合同时间线，确认各日期之间的逻辑关系")
                        .suggestionType("TIP")
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
            Set<String> amounts = new LinkedHashSet<>();
            while (matcher.find()) {
                amounts.add(matcher.group(1));
            }
            if (!amounts.isEmpty()) {
                clauseAmounts.put(
                        clause.getClauseNumber() != null ? clause.getClauseNumber() : "unknown",
                        amounts);
            }
        }

        if (clauseAmounts.size() > 1) {
            Set<String> allAmounts = new LinkedHashSet<>();
            clauseAmounts.values().forEach(allAmounts::addAll);

            if (allAmounts.size() > clauseAmounts.size() * 2) {
                issues.add(ReviewIssue.builder()
                        .cardCategory(CardCategory.LOGIC_CLAUSES.getCode())
                        .verdict("NEEDS_REVIEW")
                        .confidence(0.65)
                        .issueType("cross_clause_amount_inconsistency")
                        .severity("MAJOR")
                        .description("跨条款金额不一致：" + clauseAmounts.size() + "个条款中出现" + allAmounts.size() + "个不同金额，可能存在矛盾")
                        .suggestion("请核实各条款中的金额是否一致，避免合同金额矛盾")
                        .suggestionType("TIP")
                        .build());
            }
        }

        return issues;
    }

    private List<ReviewIssue> checkDisputeResolutionCompleteness(String content, List<ClauseInfo> clauses) {
        List<ReviewIssue> issues = new ArrayList<>();
        boolean hasJurisdiction = false;
        boolean hasArbitration = false;

        for (ClauseInfo clause : clauses) {
            if (clause.getClauseText() == null) continue;
            if (clause.getClauseText().contains("管辖") || clause.getClauseText().contains("诉讼")) {
                hasJurisdiction = true;
            }
            if (clause.getClauseText().contains("仲裁")) {
                hasArbitration = true;
            }
        }

        if (content != null) {
            if (content.contains("管辖") || content.contains("诉讼")) {
                hasJurisdiction = true;
            }
            if (content.contains("仲裁")) {
                hasArbitration = true;
            }
        }

        if (!hasJurisdiction && !hasArbitration) {
            issues.add(ReviewIssue.builder()
                    .cardCategory(CardCategory.LOGIC_CLAUSES.getCode())
                    .verdict("VIOLATION")
                    .confidence(0.85)
                    .issueType("cross_clause_missing_dispute_resolution")
                    .severity("CRITICAL")
                    .description("跨条款分析：合同缺少争议解决条款（管辖或仲裁）")
                    .suggestion("建议补充争议解决条款，明确管辖法院或仲裁机构")
                    .suggestionType("REVISION")
                    .build());
        }

        if (hasJurisdiction && hasArbitration) {
            issues.add(ReviewIssue.builder()
                    .cardCategory(CardCategory.LOGIC_CLAUSES.getCode())
                    .verdict("NEEDS_REVIEW")
                    .confidence(0.7)
                    .issueType("cross_clause_dispute_resolution_conflict")
                    .severity("MAJOR")
                    .description("跨条款分析：合同同时包含管辖和仲裁条款，可能存在冲突")
                    .suggestion("建议明确选择管辖或仲裁其中一种争议解决方式")
                    .suggestionType("REVISION")
                    .build());
        }

        return issues;
    }

    private List<ReviewIssue> checkTerminationClause(String content, List<ClauseInfo> clauses) {
        List<ReviewIssue> issues = new ArrayList<>();
        boolean hasTermination = false;

        for (ClauseInfo clause : clauses) {
            if (clause.getClauseText() == null) continue;
            String type = clause.getClauseType() != null ? clause.getClauseType().toUpperCase() : "";
            if ("TERMINATION".equals(type)
                    || clause.getClauseText().contains("解除")
                    || clause.getClauseText().contains("终止")) {
                hasTermination = true;
                break;
            }
        }

        if (!hasTermination && content != null) {
            if (content.contains("解除") || content.contains("终止")) {
                hasTermination = true;
            }
        }

        if (!hasTermination) {
            issues.add(ReviewIssue.builder()
                    .cardCategory(CardCategory.LOGIC_CLAUSES.getCode())
                    .verdict("NEEDS_REVIEW")
                    .confidence(0.8)
                    .issueType("cross_clause_missing_termination")
                    .severity("MAJOR")
                    .description("跨条款分析：合同缺少终止/解除条款")
                    .suggestion("建议补充合同终止条件及程序")
                    .suggestionType("REVISION")
                    .build());
        }

        return issues;
    }
}
