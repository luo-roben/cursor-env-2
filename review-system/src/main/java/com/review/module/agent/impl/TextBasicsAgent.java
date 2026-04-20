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
public class TextBasicsAgent implements ReviewCardAgent {

    private static final Map<Character, Character> PAIRED_PUNCTUATION = Map.of(
            '（', '）',
            '(', ')',
            '【', '】',
            '[', ']',
            '{', '}',
            '"', '"',
            '《', '》',
            '「', '」'
    );

    private static final Map<String, String> COMMON_TYPOS = Map.ofEntries(
            Map.entry("的地得", "的/地/得使用混淆"),
            Map.entry("以至于", "检查是否应为'以致于'"),
            Map.entry("权利", "检查是否应为'权力'(公权力)"),
            Map.entry("制定", "检查是否应为'制订'")
    );

    @Override
    public CardCategory getCardCategory() {
        return CardCategory.TEXT_BASICS;
    }

    @Override
    public List<ReviewIssue> review(ReviewContext context) {
        List<ReviewIssue> issues = new ArrayList<>();
        String content = context.getContent();
        if (content == null || content.isEmpty()) {
            return issues;
        }

        issues.addAll(checkPairedPunctuation(content));
        issues.addAll(checkCommonTypos(content));
        issues.addAll(checkNumberFormatConsistency(content));

        return issues;
    }

    @Override
    public boolean isApplicable(ReviewContext context) {
        return true;
    }

    private List<ReviewIssue> checkPairedPunctuation(String content) {
        List<ReviewIssue> issues = new ArrayList<>();

        for (Map.Entry<Character, Character> pair : PAIRED_PUNCTUATION.entrySet()) {
            char open = pair.getKey();
            char close = pair.getValue();
            if (open == close) continue;

            int openCount = 0;
            int closeCount = 0;
            for (int i = 0; i < content.length(); i++) {
                if (content.charAt(i) == open) openCount++;
                else if (content.charAt(i) == close) closeCount++;
            }

            if (openCount != closeCount) {
                issues.add(ReviewIssue.builder()
                        .cardCategory(CardCategory.TEXT_BASICS.getCode())
                        .verdict("NEEDS_REVIEW")
                        .confidence(0.9)
                        .issueType("paired_punctuation_mismatch")
                        .severity("MINOR")
                        .description("配对标点符号不匹配: '" + open + "' 出现" + openCount + "次, '" + close + "' 出现" + closeCount + "次")
                        .suggestion("请检查并修正配对标点符号")
                        .suggestionType("REVISION")
                        .build());
            }
        }

        return issues;
    }

    private List<ReviewIssue> checkCommonTypos(String content) {
        List<ReviewIssue> issues = new ArrayList<>();

        for (Map.Entry<String, String> entry : COMMON_TYPOS.entrySet()) {
            String pattern = entry.getKey();
            int idx = content.indexOf(pattern);
            if (idx >= 0) {
                issues.add(ReviewIssue.builder()
                        .cardCategory(CardCategory.TEXT_BASICS.getCode())
                        .charOffset(idx)
                        .charLength(pattern.length())
                        .originalText(pattern)
                        .locationText(extractContext(content, idx, 20))
                        .verdict("NEEDS_REVIEW")
                        .confidence(0.6)
                        .issueType("possible_typo")
                        .severity("INFO")
                        .description(entry.getValue())
                        .suggestion("请核实此处用词是否正确")
                        .suggestionType("TIP")
                        .build());
            }
        }

        return issues;
    }

    private List<ReviewIssue> checkNumberFormatConsistency(String content) {
        List<ReviewIssue> issues = new ArrayList<>();

        boolean hasChineseNumbers = content.matches(".*[一二三四五六七八九十百千万亿]+.*");
        boolean hasArabicNumbers = content.matches(".*\\d+.*");
        boolean hasPercentSymbol = content.contains("%");
        boolean hasChinesePercent = content.contains("百分之");

        if (hasPercentSymbol && hasChinesePercent) {
            issues.add(ReviewIssue.builder()
                    .cardCategory(CardCategory.TEXT_BASICS.getCode())
                    .verdict("NEEDS_REVIEW")
                    .confidence(0.7)
                    .issueType("number_format_inconsistency")
                    .severity("MINOR")
                    .description("文中同时使用了'%'符号和'百分之'表述，建议统一数字格式")
                    .suggestion("统一使用'%'或'百分之'表示百分比")
                    .suggestionType("TIP")
                    .build());
        }

        if (hasChineseNumbers && hasArabicNumbers) {
            long chineseCount = content.chars()
                    .filter(c -> "一二三四五六七八九十百千万亿".indexOf(c) >= 0)
                    .count();
            long arabicCount = content.chars()
                    .filter(Character::isDigit)
                    .count();

            if (chineseCount > 5 && arabicCount > 5) {
                issues.add(ReviewIssue.builder()
                        .cardCategory(CardCategory.TEXT_BASICS.getCode())
                        .verdict("NEEDS_REVIEW")
                        .confidence(0.5)
                        .issueType("number_format_mixed")
                        .severity("INFO")
                        .description("文中混合使用了中文数字和阿拉伯数字，建议根据语境统一")
                        .suggestion("正文中建议统一使用阿拉伯数字或中文数字")
                        .suggestionType("TIP")
                        .build());
            }
        }

        return issues;
    }

    private String extractContext(String content, int offset, int radius) {
        int start = Math.max(0, offset - radius);
        int end = Math.min(content.length(), offset + radius);
        return content.substring(start, end);
    }
}
