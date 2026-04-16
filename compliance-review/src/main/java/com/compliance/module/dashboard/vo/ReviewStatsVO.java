package com.compliance.module.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatsVO {

    private long totalReviews;
    private long violationCount;
    private long compliantCount;
    private long needsReviewCount;
    private long highRiskCount;
    private long mediumRiskCount;
    private long lowRiskCount;
    private double avgRiskScore;
}
