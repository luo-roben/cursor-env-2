package com.review.module.knowledge.service;

import com.review.module.agent.dto.TemplateDiffResult;

import java.util.List;

public interface TemplateDiffService {

    List<TemplateDiffResult> diff(String contractContent, String contractType, Long tenantId);
}
