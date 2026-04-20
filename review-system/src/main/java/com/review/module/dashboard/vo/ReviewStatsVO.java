package com.review.module.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatsVO {

    private long totalTasks;
    private long completedTasks;
    private long pendingTasks;
    private long violationCount;
    private long compliantCount;
    private long needsReviewCount;
    private double avgRiskScore;
    private long highRiskCount;
    private long mediumRiskCount;
    private long lowRiskCount;
}
