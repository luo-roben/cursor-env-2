package com.compliance.module.verification;

import com.compliance.module.review.entity.ReviewMissingElementDO;
import com.compliance.module.review.entity.ReviewResultDO;
import com.compliance.module.verification.service.impl.RiskScoreCalculatorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RiskScoreCalculatorTest {

    private RiskScoreCalculatorImpl calculator;

    @BeforeEach
    void setUp() {
        calculator = new RiskScoreCalculatorImpl();
    }

    @Test
    void testEmptyResults() {
        int score = calculator.calculateRiskScore(Collections.emptyList(), Collections.emptyList());
        assertEquals(0, score);
        assertEquals("low", calculator.calculateRiskLevel(score));
    }

    @Test
    void testCriticalViolationWithVerifiedCitation() {
        List<ReviewResultDO> results = new ArrayList<>();
        ReviewResultDO result = new ReviewResultDO();
        result.setVerdict("violation");
        result.setSeverity("critical");
        result.setCitationStatus("verified");
        results.add(result);

        int score = calculator.calculateRiskScore(results, Collections.emptyList());
        assertEquals(30, score);
        assertEquals("medium", calculator.calculateRiskLevel(score));
    }

    @Test
    void testCriticalViolationWithUnverifiedCitation() {
        List<ReviewResultDO> results = new ArrayList<>();
        ReviewResultDO result = new ReviewResultDO();
        result.setVerdict("violation");
        result.setSeverity("critical");
        result.setCitationStatus("unverified");
        results.add(result);

        int score = calculator.calculateRiskScore(results, Collections.emptyList());
        assertEquals(15, score);
        assertEquals("low", calculator.calculateRiskLevel(score));
    }

    @Test
    void testMixedSeveritiesVerified() {
        List<ReviewResultDO> results = new ArrayList<>();

        ReviewResultDO r1 = new ReviewResultDO();
        r1.setVerdict("violation");
        r1.setSeverity("critical");
        r1.setCitationStatus("verified");
        results.add(r1);

        ReviewResultDO r2 = new ReviewResultDO();
        r2.setVerdict("violation");
        r2.setSeverity("major");
        r2.setCitationStatus("verified");
        results.add(r2);

        int score = calculator.calculateRiskScore(results, Collections.emptyList());
        assertEquals(50, score);
        assertEquals("medium", calculator.calculateRiskLevel(score));
    }

    @Test
    void testScoreCapping() {
        List<ReviewResultDO> results = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ReviewResultDO r = new ReviewResultDO();
            r.setVerdict("violation");
            r.setSeverity("critical");
            r.setCitationStatus("verified");
            results.add(r);
        }

        int score = calculator.calculateRiskScore(results, Collections.emptyList());
        assertEquals(100, score);
    }

    @Test
    void testMissingElementsContribution() {
        List<ReviewMissingElementDO> missingElements = new ArrayList<>();
        ReviewMissingElementDO me = new ReviewMissingElementDO();
        me.setSeverity("critical");
        missingElements.add(me);

        int score = calculator.calculateRiskScore(Collections.emptyList(), missingElements);
        assertEquals(25, score);
        assertEquals("medium", calculator.calculateRiskLevel(score));
    }

    @Test
    void testCompliantResultsNoScore() {
        List<ReviewResultDO> results = new ArrayList<>();
        ReviewResultDO r = new ReviewResultDO();
        r.setVerdict("compliant");
        r.setSeverity("info");
        results.add(r);

        int score = calculator.calculateRiskScore(results, Collections.emptyList());
        assertEquals(0, score);
    }

    @Test
    void testRiskLevels() {
        assertEquals("low", calculator.calculateRiskLevel(0));
        assertEquals("low", calculator.calculateRiskLevel(20));
        assertEquals("medium", calculator.calculateRiskLevel(21));
        assertEquals("medium", calculator.calculateRiskLevel(50));
        assertEquals("high", calculator.calculateRiskLevel(51));
        assertEquals("high", calculator.calculateRiskLevel(100));
    }

    @Test
    void testCitationWeightFactors() {
        ReviewResultDO verified = new ReviewResultDO();
        verified.setVerdict("violation");
        verified.setSeverity("major");
        verified.setCitationStatus("verified");

        ReviewResultDO corrected = new ReviewResultDO();
        corrected.setVerdict("violation");
        corrected.setSeverity("major");
        corrected.setCitationStatus("corrected");

        int verifiedScore = calculator.calculateRiskScore(List.of(verified), Collections.emptyList());
        int correctedScore = calculator.calculateRiskScore(List.of(corrected), Collections.emptyList());

        assertEquals(20, verifiedScore);
        assertEquals(16, correctedScore);
    }
}
