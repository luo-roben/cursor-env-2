package com.compliance.module.filter.service.impl;

import com.compliance.module.checklist.entity.ComplianceChecklistDO;
import com.compliance.module.checklist.repository.ComplianceChecklistRepository;
import com.compliance.module.filter.dto.MissingElementResult;
import com.compliance.module.filter.service.CompletenessCheckerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompletenessCheckerServiceImpl implements CompletenessCheckerService {

    private final ComplianceChecklistRepository checklistRepository;

    @Override
    public List<MissingElementResult> check(String content, String contentType, String productType, Long tenantId) {
        List<ComplianceChecklistDO> checklistItems = new ArrayList<>();

        if (productType != null && !productType.isBlank()) {
            checklistItems.addAll(
                    checklistRepository.findByContentTypeAndProductTypeAndEnabledTrue(contentType, productType));
        } else {
            checklistItems.addAll(
                    checklistRepository.findByContentTypeAndEnabledTrue(contentType));
        }

        if (tenantId != null) {
            List<ComplianceChecklistDO> tenantItems = checklistRepository.findByTenantIdAndEnabledTrue(tenantId);
            for (ComplianceChecklistDO item : tenantItems) {
                if (checklistItems.stream().noneMatch(existing -> existing.getId().equals(item.getId()))) {
                    checklistItems.add(item);
                }
            }
        }

        List<MissingElementResult> missingElements = new ArrayList<>();
        for (ComplianceChecklistDO item : checklistItems) {
            if (!isPresent(content, item)) {
                missingElements.add(MissingElementResult.builder()
                        .checkItem(item.getCheckItem())
                        .requirement(item.getConditionDesc())
                        .lawArticleId(item.getLawArticleId())
                        .severity(item.getSeverityIfMissing())
                        .suggestion("请补充: " + item.getCheckItem())
                        .build());
            }
        }

        log.info("Completeness check: contentType={}, productType={}, tenantId={}, totalItems={}, missing={}",
                contentType, productType, tenantId, checklistItems.size(), missingElements.size());

        return missingElements;
    }

    private boolean isPresent(String content, ComplianceChecklistDO item) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        String checkItem = item.getCheckItem().trim();
        return content.contains(checkItem);
    }
}
