package com.review.module.dashboard.service.impl;

import com.review.module.dashboard.service.DashboardService;
import com.review.module.dashboard.vo.DashboardOverviewVO;
import com.review.module.dashboard.vo.ReviewStatsVO;
import com.review.module.review.repository.ReviewResultRepository;
import com.review.module.review.repository.ReviewTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ReviewTaskRepository reviewTaskRepository;
    private final ReviewResultRepository reviewResultRepository;

    @Override
    public ReviewStatsVO getStats(Long tenantId) {
        long totalTasks;
        long completedTasks;
        long pendingTasks;
        long violationCount;
        long compliantCount;
        long needsReviewCount;
        Double avgRiskScore;
        long highRiskCount;
        long mediumRiskCount;
        long lowRiskCount;

        if (tenantId != null) {
            totalTasks = reviewTaskRepository.countByTenantId(tenantId);
            completedTasks = reviewTaskRepository.countByTenantIdAndReviewStatus(tenantId, "COMPLETED");
            pendingTasks = reviewTaskRepository.countByTenantIdAndReviewStatus(tenantId, "PENDING");
            violationCount = reviewTaskRepository.countByTenantIdAndOverallVerdict(tenantId, "VIOLATION");
            compliantCount = reviewTaskRepository.countByTenantIdAndOverallVerdict(tenantId, "COMPLIANT");
            needsReviewCount = reviewTaskRepository.countByTenantIdAndOverallVerdict(tenantId, "NEEDS_REVIEW");
            avgRiskScore = reviewTaskRepository.avgRiskScoreByTenantId(tenantId);
            highRiskCount = reviewTaskRepository.countByTenantIdAndRiskLevel(tenantId, "high");
            mediumRiskCount = reviewTaskRepository.countByTenantIdAndRiskLevel(tenantId, "medium");
            lowRiskCount = reviewTaskRepository.countByTenantIdAndRiskLevel(tenantId, "low");
        } else {
            totalTasks = reviewTaskRepository.count();
            completedTasks = reviewTaskRepository.countByReviewStatus("COMPLETED");
            pendingTasks = reviewTaskRepository.countByReviewStatus("PENDING");
            violationCount = reviewTaskRepository.countByOverallVerdict("VIOLATION");
            compliantCount = reviewTaskRepository.countByOverallVerdict("COMPLIANT");
            needsReviewCount = reviewTaskRepository.countByOverallVerdict("NEEDS_REVIEW");
            avgRiskScore = reviewTaskRepository.avgRiskScoreAll();
            highRiskCount = reviewTaskRepository.countByRiskLevel("high");
            mediumRiskCount = reviewTaskRepository.countByRiskLevel("medium");
            lowRiskCount = reviewTaskRepository.countByRiskLevel("low");
        }

        return ReviewStatsVO.builder()
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .pendingTasks(pendingTasks)
                .violationCount(violationCount)
                .compliantCount(compliantCount)
                .needsReviewCount(needsReviewCount)
                .avgRiskScore(avgRiskScore != null ? avgRiskScore : 0.0)
                .highRiskCount(highRiskCount)
                .mediumRiskCount(mediumRiskCount)
                .lowRiskCount(lowRiskCount)
                .build();
    }

    @Override
    public DashboardOverviewVO getOverview(Long tenantId) {
        ReviewStatsVO stats = getStats(tenantId);

        Map<String, Long> issuesByCategory = toStringLongMap(
                tenantId != null
                        ? reviewResultRepository.countByCardCategory(tenantId)
                        : reviewResultRepository.countByCardCategoryAll());

        Map<String, Long> issuesBySeverity = toStringLongMap(
                tenantId != null
                        ? reviewResultRepository.countBySeverity(tenantId)
                        : reviewResultRepository.countBySeverityAll());

        Map<String, Long> tasksByDocumentType = toStringLongMap(
                tenantId != null
                        ? reviewTaskRepository.countByDocumentType(tenantId)
                        : reviewTaskRepository.countByDocumentTypeAll());

        Map<String, Long> tasksByStatus = toStringLongMap(
                tenantId != null
                        ? reviewTaskRepository.countByStatus(tenantId)
                        : reviewTaskRepository.countByStatusAll());

        return DashboardOverviewVO.builder()
                .stats(stats)
                .issuesByCategory(issuesByCategory)
                .issuesBySeverity(issuesBySeverity)
                .tasksByDocumentType(tasksByDocumentType)
                .tasksByStatus(tasksByStatus)
                .build();
    }

    private Map<String, Long> toStringLongMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        if (rows != null) {
            for (Object[] row : rows) {
                String key = row[0] != null ? String.valueOf(row[0]) : "UNKNOWN";
                Long value = ((Number) row[1]).longValue();
                map.put(key, value);
            }
        }
        return map;
    }
}
