package com.review.module.dashboard.controller;

import com.review.common.result.CommonResult;
import com.review.module.dashboard.vo.DashboardOverviewVO;
import com.review.module.dashboard.vo.ReviewStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "仪表盘", description = "仪表盘概览接口")
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    @Operation(summary = "获取仪表盘概览")
    @GetMapping("/overview")
    public CommonResult<DashboardOverviewVO> getOverview(
            @RequestParam(required = false) Long tenantId) {

        ReviewStatsVO stats = ReviewStatsVO.builder()
                .totalTasks(0)
                .completedTasks(0)
                .pendingTasks(0)
                .violationCount(0)
                .compliantCount(0)
                .needsReviewCount(0)
                .avgRiskScore(0.0)
                .highRiskCount(0)
                .mediumRiskCount(0)
                .lowRiskCount(0)
                .build();

        DashboardOverviewVO overview = DashboardOverviewVO.builder()
                .stats(stats)
                .issuesByCategory(Map.of())
                .issuesBySeverity(Map.of())
                .tasksByDocumentType(Map.of())
                .tasksByStatus(Map.of())
                .build();

        return CommonResult.success(overview);
    }

    @Operation(summary = "获取审查统计")
    @GetMapping("/stats")
    public CommonResult<ReviewStatsVO> getStats(
            @RequestParam(required = false) Long tenantId) {

        ReviewStatsVO stats = ReviewStatsVO.builder()
                .totalTasks(0)
                .completedTasks(0)
                .pendingTasks(0)
                .violationCount(0)
                .compliantCount(0)
                .needsReviewCount(0)
                .avgRiskScore(0.0)
                .highRiskCount(0)
                .mediumRiskCount(0)
                .lowRiskCount(0)
                .build();

        return CommonResult.success(stats);
    }
}
