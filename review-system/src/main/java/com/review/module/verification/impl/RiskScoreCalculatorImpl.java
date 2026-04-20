package com.review.module.verification.impl;

import com.review.module.agent.dto.ReviewIssue;
import com.review.module.verification.RiskScoreCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RiskScoreCalculatorImpl implements RiskScoreCalculator {

    private static final Map<String, Integer> SEVERITY_SCORES = Map.of(
            "CRITICAL", 30,
            "MAJOR", 20,
            "MINOR", 10,
            "INFO", 5
    );

    private static final Map<String, Integer> MISSING_ELEMENT_SCORES = Map.of(
            "CRITICAL", 25,
            "MAJOR", 10
    );

    private static final Map<String, Double> CITATION_WEIGHT = Map.of(
            "VERIFIED", 1.0,
            "CORRECTED", 0.8,
            "UNVERIFIED", 0.5,
            "PENDING", 0.5
    );

    @Override
    public RiskScoreResult calculate(List<ReviewIssue> issues, List<String> missingElements) {
        double totalScore = 0;

        if (issues != null) {
            for (ReviewIssue issue : issues) {
                if (!"VIOLATION".equalsIgnoreCase(issue.getVerdict())
                        && !"NEEDS_REVIEW".equalsIgnoreCase(issue.getVerdict())) {
                    continue;
                }

                int baseScore = SEVERITY_SCORES.getOrDefault(
                        issue.getSeverity() != null ? issue.getSeverity().toUpperCase() : "MINOR", 10);

                double citationWeight = CITATION_WEIGHT.getOrDefault(
                        issue.getCitationStatus() != null ? issue.getCitationStatus().toUpperCase() : "PENDING", 0.5);

                totalScore += baseScore * citationWeight;
            }
        }

        if (missingElements != null) {
            for (String element : missingElements) {
                totalScore += MISSING_ELEMENT_SCORES.getOrDefault("MAJOR", 10);
            }
        }

        int score = Math.min(100, (int) Math.round(totalScore));
        String level = determineLevel(score);

        return new RiskScoreResult(score, level);
    }

    private String determineLevel(int score) {
        if (score <= 20) return "low";
        if (score <= 50) return "medium";
        return "high";
    }
}
