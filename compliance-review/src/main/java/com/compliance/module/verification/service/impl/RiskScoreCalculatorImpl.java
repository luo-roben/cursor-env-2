package com.compliance.module.verification.service.impl;

import com.compliance.module.review.entity.ReviewMissingElementDO;
import com.compliance.module.review.entity.ReviewResultDO;
import com.compliance.module.verification.service.RiskScoreCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RiskScoreCalculatorImpl implements RiskScoreCalculator {

    private static final Map<String, Integer> SEVERITY_SCORES = Map.of(
            "critical", 30,
            "major", 20,
            "minor", 10,
            "info", 5
    );

    private static final Map<String, Integer> MISSING_ELEMENT_SCORES = Map.of(
            "critical", 25,
            "major", 10,
            "minor", 5
    );

    private static final Map<String, Double> CITATION_WEIGHT = Map.of(
            "verified", 1.0,
            "corrected", 0.8,
            "unverified", 0.5,
            "pending", 0.7
    );

    @Override
    public int calculateRiskScore(List<ReviewResultDO> results, List<ReviewMissingElementDO> missingElements) {
        double totalScore = 0;

        for (ReviewResultDO result : results) {
            if ("violation".equals(result.getVerdict()) && result.getSeverity() != null) {
                int baseScore = SEVERITY_SCORES.getOrDefault(result.getSeverity(), 5);
                double weight = CITATION_WEIGHT.getOrDefault(
                        result.getCitationStatus() != null ? result.getCitationStatus() : "pending", 0.7);
                totalScore += baseScore * weight;
            }
        }

        for (ReviewMissingElementDO missing : missingElements) {
            if (missing.getSeverity() != null) {
                totalScore += MISSING_ELEMENT_SCORES.getOrDefault(missing.getSeverity(), 10);
            }
        }

        int capped = Math.min((int) Math.round(totalScore), 100);
        log.debug("Risk score calculated: raw={}, capped={}, violations={}, missingElements={}",
                totalScore, capped, results.size(), missingElements.size());
        return capped;
    }

    @Override
    public String calculateRiskLevel(int riskScore) {
        if (riskScore > 50) return "high";
        if (riskScore >= 21) return "medium";
        return "low";
    }
}
