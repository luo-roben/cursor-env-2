package com.review.module.filter;

import com.review.module.agent.dto.CustomRuleInfo;
import com.review.module.filter.dto.QuickFilterResult;
import com.review.module.filter.impl.QuickFilterServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuickFilterTest {

    private QuickFilterServiceImpl filterService;

    @BeforeEach
    void setUp() {
        filterService = new QuickFilterServiceImpl();
    }

    @Test
    void testBannedWordFilter() {
        List<CustomRuleInfo> rules = List.of(
                CustomRuleInfo.builder()
                        .ruleType("BANNED_WORD")
                        .content("保本")
                        .severity("CRITICAL")
                        .build()
        );

        QuickFilterResult result = filterService.filter("本产品保本保收益", rules);

        assertFalse(result.isPassed());
        assertFalse(result.getBannedWordHits().isEmpty());
        assertEquals("保本", result.getBannedWordHits().get(0).getWord());
        assertEquals("CRITICAL", result.getBannedWordHits().get(0).getSeverity());
    }

    @Test
    void testRequiredStatementFilter() {
        List<CustomRuleInfo> rules = List.of(
                CustomRuleInfo.builder()
                        .ruleType("REQUIRED_STATEMENT")
                        .content("风险提示")
                        .severity("MAJOR")
                        .build()
        );

        QuickFilterResult result = filterService.filter("这是一段没有风险提示的文本", rules);
        assertTrue(result.isPassed(), "Content contains 'REQUIRED_STATEMENT', so it should pass");

        QuickFilterResult result2 = filterService.filter("这是一段普通文本", rules);
        assertFalse(result2.isPassed(), "Content missing 'REQUIRED_STATEMENT', should fail");
        assertTrue(result2.getMissingStatements().contains("风险提示"));
    }

    @Test
    void testNoIssues() {
        List<CustomRuleInfo> rules = List.of(
                CustomRuleInfo.builder()
                        .ruleType("BANNED_WORD")
                        .content("保本")
                        .severity("CRITICAL")
                        .build(),
                CustomRuleInfo.builder()
                        .ruleType("REQUIRED_STATEMENT")
                        .content("风险提示")
                        .severity("MAJOR")
                        .build()
        );

        QuickFilterResult result = filterService.filter("本文包含风险提示内容，请仔细阅读。", rules);
        assertTrue(result.isPassed());
        assertTrue(result.getBannedWordHits().isEmpty());
        assertTrue(result.getMissingStatements().isEmpty());
    }

    @Test
    void testNullContent() {
        List<CustomRuleInfo> rules = List.of(
                CustomRuleInfo.builder()
                        .ruleType("BANNED_WORD")
                        .content("保本")
                        .severity("CRITICAL")
                        .build()
        );

        QuickFilterResult result = filterService.filter(null, rules);
        assertTrue(result.isPassed());
    }

    @Test
    void testNullRules() {
        QuickFilterResult result = filterService.filter("任何内容", null);
        assertTrue(result.isPassed());
    }

    @Test
    void testMultipleBannedWordOccurrences() {
        List<CustomRuleInfo> rules = List.of(
                CustomRuleInfo.builder()
                        .ruleType("BANNED_WORD")
                        .content("保本")
                        .severity("CRITICAL")
                        .build()
        );

        QuickFilterResult result = filterService.filter("保本理财产品保本保收益", rules);
        assertFalse(result.isPassed());
        assertTrue(result.getBannedWordHits().size() >= 2, "Should find multiple occurrences");
    }
}
