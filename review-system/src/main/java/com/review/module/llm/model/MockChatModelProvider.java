package com.review.module.llm.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class MockChatModelProvider implements ChatModelProvider {

    private static final List<String> MARKETING_KEYWORDS = List.of(
            "保本", "收益率", "稳赚", "零风险", "最高", "最佳", "暴涨"
    );

    private static final List<String> CONTRACT_KEYWORDS = List.of(
            "权责不对等", "违约金过高", "无上限赔偿", "单方解除", "霸王条款"
    );

    @Override
    public String generate(String modelName, String prompt) {
        log.debug("MockChatModelProvider generating response for model={}", modelName);

        List<String> detectedIssues = new ArrayList<>();

        for (String keyword : MARKETING_KEYWORDS) {
            if (prompt.contains(keyword)) {
                detectedIssues.add(buildMarketingIssue(keyword));
            }
        }

        for (String keyword : CONTRACT_KEYWORDS) {
            if (prompt.contains(keyword)) {
                detectedIssues.add(buildContractIssue(keyword));
            }
        }

        if (detectedIssues.isEmpty()) {
            return "{\"issues\":[],\"summary\":\"未发现明显合规问题\"}";
        }

        return "{\"issues\":[" + String.join(",", detectedIssues) + "],\"summary\":\"发现" + detectedIssues.size() + "个潜在合规问题\"}";
    }

    private String buildMarketingIssue(String keyword) {
        String severity = switch (keyword) {
            case "保本", "稳赚", "零风险" -> "CRITICAL";
            case "最高", "最佳", "暴涨" -> "MAJOR";
            default -> "MINOR";
        };

        return "{" +
                "\"verdict\":\"VIOLATION\"," +
                "\"confidence\":0.95," +
                "\"issueType\":\"marketing_compliance\"," +
                "\"severity\":\"" + severity + "\"," +
                "\"description\":\"包含违规营销用语: " + keyword + "\"," +
                "\"suggestion\":\"删除或替换违规用语'" + keyword + "'\"," +
                "\"suggestionType\":\"REVISION\"," +
                "\"citedLawName\":\"证券期货投资者适当性管理办法\"," +
                "\"citedArticleCode\":\"第24条\"" +
                "}";
    }

    private String buildContractIssue(String keyword) {
        String severity = switch (keyword) {
            case "无上限赔偿", "霸王条款" -> "CRITICAL";
            case "权责不对等", "违约金过高" -> "MAJOR";
            default -> "MINOR";
        };

        return "{" +
                "\"verdict\":\"NEEDS_REVIEW\"," +
                "\"confidence\":0.85," +
                "\"issueType\":\"contract_clause_issue\"," +
                "\"severity\":\"" + severity + "\"," +
                "\"description\":\"合同条款问题: " + keyword + "\"," +
                "\"suggestion\":\"建议修改相关条款以确保权责对等\"," +
                "\"suggestionType\":\"NEGOTIATION_POINT\"," +
                "\"citedLawName\":\"中华人民共和国民法典\"," +
                "\"citedArticleCode\":\"第496条\"" +
                "}";
    }
}
