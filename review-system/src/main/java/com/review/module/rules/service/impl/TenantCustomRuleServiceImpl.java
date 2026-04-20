package com.review.module.rules.service.impl;

import com.review.common.exception.ErrorCode;
import com.review.common.exception.ServiceException;
import com.review.module.rules.entity.TenantCustomRuleDO;
import com.review.module.rules.repository.TenantCustomRuleRepository;
import com.review.module.rules.service.TenantCustomRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantCustomRuleServiceImpl implements TenantCustomRuleService {

    private final TenantCustomRuleRepository tenantCustomRuleRepository;

    @Override
    @Transactional
    public TenantCustomRuleDO create(TenantCustomRuleDO rule) {
        return tenantCustomRuleRepository.save(rule);
    }

    @Override
    @Transactional
    public TenantCustomRuleDO update(Long id, TenantCustomRuleDO rule) {
        TenantCustomRuleDO existing = tenantCustomRuleRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.CUSTOM_RULE_NOT_FOUND));
        existing.setRuleType(rule.getRuleType());
        existing.setContent(rule.getContent());
        existing.setPriority(rule.getPriority());
        existing.setApplicableContentTypes(rule.getApplicableContentTypes());
        existing.setApplicableDocTypes(rule.getApplicableDocTypes());
        existing.setApplicableContractTypes(rule.getApplicableContractTypes());
        existing.setMatchMode(rule.getMatchMode());
        existing.setSeverityIfTriggered(rule.getSeverityIfTriggered());
        existing.setEnabled(rule.getEnabled());
        return tenantCustomRuleRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!tenantCustomRuleRepository.existsById(id)) {
            throw new ServiceException(ErrorCode.CUSTOM_RULE_NOT_FOUND);
        }
        tenantCustomRuleRepository.deleteById(id);
    }

    @Override
    public TenantCustomRuleDO getById(Long id) {
        return tenantCustomRuleRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.CUSTOM_RULE_NOT_FOUND));
    }

    @Override
    public List<TenantCustomRuleDO> listByTenantId(Long tenantId) {
        return tenantCustomRuleRepository.findByTenantId(tenantId);
    }

    @Override
    public List<TenantCustomRuleDO> listEnabledByTenantId(Long tenantId) {
        return tenantCustomRuleRepository.findByTenantIdAndEnabledTrue(tenantId);
    }

    @Override
    public List<TenantCustomRuleDO> listByTenantIdAndType(Long tenantId, String ruleType) {
        return tenantCustomRuleRepository.findByTenantIdAndRuleTypeAndEnabledTrue(tenantId, ruleType);
    }
}
