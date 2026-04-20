package com.review.module.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewVO {

    private ReviewStatsVO stats;
    private Map<String, Long> issuesByCategory;
    private Map<String, Long> issuesBySeverity;
    private Map<String, Long> tasksByDocumentType;
    private Map<String, Long> tasksByStatus;
}
