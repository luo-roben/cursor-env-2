package com.review.module.agent;

import com.review.module.agent.dto.ReviewContext;
import com.review.module.agent.dto.ReviewIssue;
import com.review.module.agent.impl.TextBasicsAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextBasicsAgentTest {

    private TextBasicsAgent agent;

    @BeforeEach
    void setUp() {
        agent = new TextBasicsAgent();
    }

    @Test
    void testPairedPunctuationDetection() {
        ReviewContext context = ReviewContext.builder()
                .content("这是一个测试（缺少右括号的文本")
                .build();

        List<ReviewIssue> issues = agent.review(context);

        boolean hasPunctuationIssue = issues.stream()
                .anyMatch(i -> "paired_punctuation_mismatch".equals(i.getIssueType()));
        assertTrue(hasPunctuationIssue, "Should detect mismatched paired punctuation");
    }

    @Test
    void testNoIssuesCleanText() {
        ReviewContext context = ReviewContext.builder()
                .content("这是一段干净的文本，没有任何问题。")
                .build();

        List<ReviewIssue> issues = agent.review(context);

        boolean hasPunctuationIssue = issues.stream()
                .anyMatch(i -> "paired_punctuation_mismatch".equals(i.getIssueType()));
        assertFalse(hasPunctuationIssue, "Clean text should not have punctuation issues");
    }

    @Test
    void testApplicableAlwaysTrue() {
        ReviewContext context = ReviewContext.builder()
                .content("任何内容")
                .build();
        assertTrue(agent.isApplicable(context));

        ReviewContext emptyContext = ReviewContext.builder().build();
        assertTrue(agent.isApplicable(emptyContext));
    }

    @Test
    void testNullContent() {
        ReviewContext context = ReviewContext.builder()
                .content(null)
                .build();
        List<ReviewIssue> issues = agent.review(context);
        assertTrue(issues.isEmpty());
    }

    @Test
    void testEmptyContent() {
        ReviewContext context = ReviewContext.builder()
                .content("")
                .build();
        List<ReviewIssue> issues = agent.review(context);
        assertTrue(issues.isEmpty());
    }

    @Test
    void testMultipleMismatchedPairs() {
        ReviewContext context = ReviewContext.builder()
                .content("（左括号《左书名号")
                .build();

        List<ReviewIssue> issues = agent.review(context);

        long punctuationIssueCount = issues.stream()
                .filter(i -> "paired_punctuation_mismatch".equals(i.getIssueType()))
                .count();
        assertTrue(punctuationIssueCount >= 2, "Should detect multiple mismatched pairs");
    }
}
