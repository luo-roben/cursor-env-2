package com.review.module.verification;

import com.review.module.agent.dto.ReviewIssue;
import com.review.module.verification.impl.RiskScoreCalculatorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiskScoreCalculatorTest {

    private RiskScoreCalculatorImpl calculator;

    @BeforeEach
    void setUp() {
        calculator = new RiskScoreCalculatorImpl();
    }

    @Test
    void testEmptyIssues() {
        RiskScoreCalculator.RiskScoreResult result = calculator.calculate(Collections.emptyList(), Collections.emptyList());
        assertEquals(0, result.score());
        assertEquals("low", result.level());
    }

    @Test
    void testCriticalIssue() {
        List<ReviewIssue> issues = List.of(
                ReviewIssue.builder()
                        .verdict("VIOLATION")
                        .severity("CRITICAL")
                        .citationStatus("VERIFIED")
                        .build()
        );
        RiskScoreCalculator.RiskScoreResult result = calculator.calculate(issues, Collections.emptyList());
        assertEquals(30, result.score());
    }

    @Test
    void testUnverifiedCitation() {
        List<ReviewIssue> issues = List.of(
                ReviewIssue.builder()
                        .verdict("VIOLATION")
                        .severity("CRITICAL")
                        .citationStatus("UNVERIFIED")
                        .build()
        );
        RiskScoreCalculator.RiskScoreResult result = calculator.calculate(issues, Collections.emptyList());
        assertEquals(15, result.score());
    }

    @Test
    void testMixedSeverities() {
        List<ReviewIssue> issues = List.of(
                ReviewIssue.builder()
                        .verdict("VIOLATION")
                        .severity("CRITICAL")
                        .citationStatus("VERIFIED")
                        .build(),
                ReviewIssue.builder()
                        .verdict("NEEDS_REVIEW")
                        .severity("MAJOR")
                        .citationStatus("VERIFIED")
                        .build(),
                ReviewIssue.builder()
                        .verdict("VIOLATION")
                        .severity("MINOR")
                        .citationStatus("PENDING")
                        .build()
        );
        RiskScoreCalculator.RiskScoreResult result = calculator.calculate(issues, Collections.emptyList());
        // CRITICAL*1.0=30, MAJOR*1.0=20, MINOR*0.5=5 => total=55
        assertEquals(55, result.score());
    }

    @Test
    void testScoreCapping() {
        List<ReviewIssue> issues = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            issues.add(ReviewIssue.builder()
                    .verdict("VIOLATION")
                    .severity("CRITICAL")
                    .citationStatus("VERIFIED")
                    .build());
        }
        RiskScoreCalculator.RiskScoreResult result = calculator.calculate(issues, Collections.emptyList());
        assertEquals(100, result.score());
    }

    @Test
    void testRiskLevels() {
        // low: 0-20
        RiskScoreCalculator.RiskScoreResult low = calculator.calculate(
                List.of(ReviewIssue.builder().verdict("VIOLATION").severity("INFO").citationStatus("VERIFIED").build()),
                Collections.emptyList());
        assertEquals("low", low.level());
        assertTrue(low.score() <= 20);

        // medium: 21-50
        List<ReviewIssue> mediumIssues = List.of(
                ReviewIssue.builder().verdict("VIOLATION").severity("CRITICAL").citationStatus("VERIFIED").build(),
                ReviewIssue.builder().verdict("VIOLATION").severity("MINOR").citationStatus("VERIFIED").build()
        );
        RiskScoreCalculator.RiskScoreResult medium = calculator.calculate(mediumIssues, Collections.emptyList());
        assertEquals("medium", medium.level());
        assertTrue(medium.score() > 20 && medium.score() <= 50);

        // high: 51-100
        List<ReviewIssue> highIssues = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            highIssues.add(ReviewIssue.builder().verdict("VIOLATION").severity("CRITICAL").citationStatus("VERIFIED").build());
        }
        RiskScoreCalculator.RiskScoreResult high = calculator.calculate(highIssues, Collections.emptyList());
        assertEquals("high", high.level());
        assertTrue(high.score() > 50);
    }

    @Test
    void testMissingElements() {
        RiskScoreCalculator.RiskScoreResult result = calculator.calculate(
                Collections.emptyList(),
                List.of("签章页", "生效日期")
        );
        // Each missing element adds MAJOR=10 score
        assertEquals(20, result.score());
        assertEquals("low", result.level());
    }

    @Test
    void testNullInputs() {
        RiskScoreCalculator.RiskScoreResult result = calculator.calculate(null, null);
        assertEquals(0, result.score());
        assertEquals("low", result.level());
    }

    @Test
    void testCompliantVerdictIgnored() {
        List<ReviewIssue> issues = List.of(
                ReviewIssue.builder()
                        .verdict("COMPLIANT")
                        .severity("CRITICAL")
                        .citationStatus("VERIFIED")
                        .build()
        );
        RiskScoreCalculator.RiskScoreResult result = calculator.calculate(issues, Collections.emptyList());
        assertEquals(0, result.score());
    }
}
