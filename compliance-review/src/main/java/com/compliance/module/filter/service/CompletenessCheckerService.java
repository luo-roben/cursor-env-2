package com.compliance.module.filter.service;

import com.compliance.module.filter.dto.MissingElementResult;

import java.util.List;

public interface CompletenessCheckerService {

    List<MissingElementResult> check(String content, String contentType, String productType, Long tenantId);
}
