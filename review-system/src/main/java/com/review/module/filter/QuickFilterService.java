package com.review.module.filter;

import com.review.module.agent.dto.CustomRuleInfo;
import com.review.module.filter.dto.QuickFilterResult;

import java.util.List;

public interface QuickFilterService {

    QuickFilterResult filter(String content, List<CustomRuleInfo> customRules);
}
