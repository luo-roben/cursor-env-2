package com.compliance.module.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewVO {

    private long totalTasks;
    private long pendingTasks;
    private long completedTasks;
    private long humanReviewedTasks;
    private long totalCases;
    private long totalLawArticles;
    private long totalCustomRules;
}
