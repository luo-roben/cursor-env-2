package com.compliance.module.filter.service;

import com.compliance.module.filter.dto.QuickFilterResult;

public interface QuickFilterService {

    QuickFilterResult filter(String content, Long tenantId);
}
