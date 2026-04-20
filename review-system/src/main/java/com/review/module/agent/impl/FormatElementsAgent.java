package com.review.module.agent.impl;

import com.review.common.enums.CardCategory;
import com.review.module.agent.ReviewCardAgent;
import com.review.module.agent.dto.ReviewContext;
import com.review.module.agent.dto.ReviewIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class FormatElementsAgent implements ReviewCardAgent {

    private static final Map<String, List<String>> REQUIRED_ELEMENTS = new LinkedHashMap<>();

    static {
        REQUIRED_ELEMENTS.put("MARKETING", List.of(
                "风险提示", "免责声明", "过往业绩不代表未来表现",
                "投资有风险", "产品名称", "管理人"
        ));
        REQUIRED_ELEMENTS.put("CONTRACT", List.of(
                "合同编号", "甲方", "乙方", "签署日期",
                "争议解决", "违约责任", "生效条件"
        ));
        REQUIRED_ELEMENTS.put("PROSPECTUS", List.of(
                "风险揭示", "投资策略", "费率说明",
                "基金管理人", "基金托管人", "基金份额"
        ));
    }

    @Override
    public CardCategory getCardCategory() {
        return CardCategory.FORMAT_ELEMENTS;
    }

    @Override
    public List<ReviewIssue> review(ReviewContext context) {
        List<ReviewIssue> issues = new ArrayList<>();
        String content = context.getContent();
        if (content == null || content.isEmpty()) {
            return issues;
        }

        String docType = context.getDocumentType();
        List<String> requiredElements = REQUIRED_ELEMENTS.getOrDefault(
                docType != null ? docType.toUpperCase() : "", Collections.emptyList());

        for (String element : requiredElements) {
            if (!content.contains(element)) {
                String severity = determineSeverity(element);
                issues.add(ReviewIssue.builder()
                        .cardCategory(CardCategory.FORMAT_ELEMENTS.getCode())
                        .verdict("VIOLATION")
                        .confidence(0.85)
                        .issueType("missing_required_element")
                        .severity(severity)
                        .description("缺少必要要素: " + element)
                        .suggestion("请补充'" + element + "'相关内容")
                        .suggestionType("REVISION")
                        .build());
            }
        }

        return issues;
    }

    @Override
    public boolean isApplicable(ReviewContext context) {
        return true;
    }

    private String determineSeverity(String element) {
        Set<String> criticalElements = Set.of("风险提示", "免责声明", "风险揭示", "争议解决", "违约责任");
        Set<String> majorElements = Set.of("产品名称", "管理人", "甲方", "乙方", "基金管理人");
        if (criticalElements.contains(element)) return "CRITICAL";
        if (majorElements.contains(element)) return "MAJOR";
        return "MINOR";
    }
}
