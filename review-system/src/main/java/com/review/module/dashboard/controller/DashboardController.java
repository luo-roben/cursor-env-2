package com.review.module.dashboard.controller;

import com.review.common.result.CommonResult;
import com.review.module.dashboard.service.DashboardService;
import com.review.module.dashboard.vo.DashboardOverviewVO;
import com.review.module.dashboard.vo.ReviewStatsVO;
import com.review.module.feedback.dto.ConsistencyReport;
import com.review.module.feedback.service.ReviewerConsistencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "仪表盘", description = "仪表盘概览接口")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final ReviewerConsistencyService reviewerConsistencyService;

    @Operation(summary = "获取仪表盘概览")
    @GetMapping("/overview")
    public CommonResult<DashboardOverviewVO> getOverview(
            @RequestParam(required = false) Long tenantId) {
        return CommonResult.success(dashboardService.getOverview(tenantId));
    }

    @Operation(summary = "获取审查统计")
    @GetMapping("/stats")
    public CommonResult<ReviewStatsVO> getStats(
            @RequestParam(required = false) Long tenantId) {
        return CommonResult.success(dashboardService.getStats(tenantId));
    }

    @Operation(summary = "获取审查一致性报告")
    @GetMapping("/consistency")
    public CommonResult<ConsistencyReport> getConsistency(
            @RequestParam Long tenantId) {
        return CommonResult.success(reviewerConsistencyService.calculateConsistency(tenantId));
    }
}
