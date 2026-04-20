package com.review.module.verification;

import com.review.module.agent.dto.ReviewIssue;

import java.util.List;

public interface RiskScoreCalculator {

    RiskScoreResult calculate(List<ReviewIssue> issues, List<String> missingElements);

    record RiskScoreResult(int score, String level) {}
}
