package com.review.module.dashboard.service;

import com.review.module.dashboard.vo.DashboardOverviewVO;
import com.review.module.dashboard.vo.ReviewStatsVO;

public interface DashboardService {
    DashboardOverviewVO getOverview(Long tenantId);
    ReviewStatsVO getStats(Long tenantId);
}
