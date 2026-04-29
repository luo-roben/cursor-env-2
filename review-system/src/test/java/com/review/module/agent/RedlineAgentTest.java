package com.review.module.agent;

import com.review.module.agent.dto.CustomRuleInfo;
import com.review.module.agent.dto.ReviewContext;
import com.review.module.agent.dto.ReviewIssue;
import com.review.module.agent.impl.RedlineAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RedlineAgentTest {

    private RedlineAgent agent;

    @BeforeEach
    void setUp() {
        agent = new RedlineAgent();
    }

    @Test
    void testBannedWordDetection() {
        List<CustomRuleInfo> rules = List.of(
                CustomRuleInfo.builder()
                        .ruleType("BANNED_WORD")
                        .content("保本")
                        .severity("CRITICAL")
                        .build()
        );

        ReviewContext context = ReviewContext.builder()
                .content("本产品保本保收益，请放心购买")
                .customRules(rules)
                .build();

        List<ReviewIssue> issues = agent.review(context);

        assertFalse(issues.isEmpty(), "Should detect banned word");
        assertTrue(issues.stream().anyMatch(i -> "banned_word".equals(i.getIssueType())));
        assertTrue(issues.stream().anyMatch(i -> "VIOLATION".equals(i.getVerdict())));
    }

    @Test
    void testNoBannedWords() {
        List<CustomRuleInfo> rules = List.of(
                CustomRuleInfo.builder()
                        .ruleType("BANNED_WORD")
                        .content("保本")
                        .severity("CRITICAL")
                        .build()
        );

        ReviewContext context = ReviewContext.builder()
                .content("这是一段正常的合同文本，不包含任何敏感词。")
                .customRules(rules)
                .build();

        List<ReviewIssue> issues = agent.review(context);

        boolean hasBannedWord = issues.stream()
                .anyMatch(i -> "banned_word".equals(i.getIssueType()));
        assertFalse(hasBannedWord, "Should not detect banned words in clean text");
    }

    @Test
    void testMultipleBannedWords() {
        List<CustomRuleInfo> rules = List.of(
                CustomRuleInfo.builder()
                        .ruleType("BANNED_WORD")
                        .content("保本")
                        .severity("CRITICAL")
                        .build(),
                CustomRuleInfo.builder()
                        .ruleType("BANNED_WORD")
                        .content("无风险")
                        .severity("MAJOR")
                        .build()
        );

        ReviewContext context = ReviewContext.builder()
                .content("本产品保本且无风险，适合所有投资者")
                .customRules(rules)
                .build();

        List<ReviewIssue> issues = agent.review(context);

        long bannedWordCount = issues.stream()
                .filter(i -> "banned_word".equals(i.getIssueType()))
                .count();
        assertTrue(bannedWordCount >= 2, "Should detect multiple banned words");
    }

    @Test
    void testNoRulesProvided() {
        ReviewContext context = ReviewContext.builder()
                .content("保本保收益")
                .customRules(null)
                .build();

        List<ReviewIssue> issues = agent.review(context);
        assertTrue(issues.isEmpty(), "No rules means no banned word issues");
    }

    @Test
    void testEmptyContent() {
        List<CustomRuleInfo> rules = List.of(
                CustomRuleInfo.builder()
                        .ruleType("BANNED_WORD")
                        .content("保本")
                        .severity("CRITICAL")
                        .build()
        );

        ReviewContext context = ReviewContext.builder()
                .content("")
                .customRules(rules)
                .build();

        List<ReviewIssue> issues = agent.review(context);
        assertTrue(issues.isEmpty());
    }
}
