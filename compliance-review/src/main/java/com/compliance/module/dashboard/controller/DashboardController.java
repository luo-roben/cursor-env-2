package com.compliance.module.dashboard.controller;

import com.compliance.common.result.CommonResult;
import com.compliance.module.cases.repository.ReviewCaseRepository;
import com.compliance.module.dashboard.vo.DashboardOverviewVO;
import com.compliance.module.dashboard.vo.ReviewStatsVO;
import com.compliance.module.law.repository.LawArticleRepository;
import com.compliance.module.review.entity.ReviewTaskDO;
import com.compliance.module.review.repository.ReviewTaskRepository;
import com.compliance.module.tenant.repository.TenantCustomRuleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "统计仪表盘", description = "Dashboard statistics")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ReviewTaskRepository reviewTaskRepository;
    private final ReviewCaseRepository reviewCaseRepository;
    private final LawArticleRepository lawArticleRepository;
    private final TenantCustomRuleRepository tenantCustomRuleRepository;

    @Operation(summary = "总览统计", description = "Get dashboard overview statistics")
    @GetMapping("/overview")
    public CommonResult<DashboardOverviewVO> overview(
            @Parameter(description = "租户ID") @RequestParam Long tenantId) {

        Page<ReviewTaskDO> allTasks = reviewTaskRepository.findByTenantIdAndFilters(
                tenantId, null, null, PageRequest.of(0, Integer.MAX_VALUE));
        List<ReviewTaskDO> tasks = allTasks.getContent();

        long totalTasks = tasks.size();
        long pendingTasks = tasks.stream().filter(t -> "pending".equals(t.getReviewStatus())).count();
        long completedTasks = tasks.stream().filter(t -> "completed".equals(t.getReviewStatus())).count();
        long humanReviewedTasks = tasks.stream().filter(t -> "human_reviewed".equals(t.getReviewStatus())).count();

        long totalCases = reviewCaseRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).size();
        long totalLawArticles = lawArticleRepository.count();
        long totalCustomRules = tenantCustomRuleRepository.findByTenantIdOrderByPriorityDesc(tenantId).size();

        return CommonResult.success(DashboardOverviewVO.builder()
                .totalTasks(totalTasks)
                .pendingTasks(pendingTasks)
                .completedTasks(completedTasks)
                .humanReviewedTasks(humanReviewedTasks)
                .totalCases(totalCases)
                .totalLawArticles(totalLawArticles)
                .totalCustomRules(totalCustomRules)
                .build());
    }

    @Operation(summary = "审查统计", description = "Get review statistics")
    @GetMapping("/review-stats")
    public CommonResult<ReviewStatsVO> reviewStats(
            @Parameter(description = "租户ID") @RequestParam Long tenantId) {

        Page<ReviewTaskDO> allTasks = reviewTaskRepository.findByTenantIdAndFilters(
                tenantId, null, null, PageRequest.of(0, Integer.MAX_VALUE));
        List<ReviewTaskDO> tasks = allTasks.getContent();

        long totalReviews = tasks.size();
        long violationCount = tasks.stream().filter(t -> "violation".equals(t.getOverallVerdict())).count();
        long compliantCount = tasks.stream().filter(t -> "compliant".equals(t.getOverallVerdict())).count();
        long needsReviewCount = tasks.stream().filter(t -> "needs_review".equals(t.getOverallVerdict())).count();
        long highRiskCount = tasks.stream().filter(t -> "high".equals(t.getRiskLevel())).count();
        long mediumRiskCount = tasks.stream().filter(t -> "medium".equals(t.getRiskLevel())).count();
        long lowRiskCount = tasks.stream().filter(t -> "low".equals(t.getRiskLevel())).count();
        double avgRiskScore = tasks.stream()
                .filter(t -> t.getRiskScore() != null)
                .mapToInt(ReviewTaskDO::getRiskScore)
                .average()
                .orElse(0.0);

        return CommonResult.success(ReviewStatsVO.builder()
                .totalReviews(totalReviews)
                .violationCount(violationCount)
                .compliantCount(compliantCount)
                .needsReviewCount(needsReviewCount)
                .highRiskCount(highRiskCount)
                .mediumRiskCount(mediumRiskCount)
                .lowRiskCount(lowRiskCount)
                .avgRiskScore(Math.round(avgRiskScore * 100.0) / 100.0)
                .build());
    }
}
