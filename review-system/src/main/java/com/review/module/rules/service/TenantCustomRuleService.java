package com.review.module.rules.service;

import com.review.module.rules.entity.TenantCustomRuleDO;

import java.util.List;

public interface TenantCustomRuleService {

    TenantCustomRuleDO create(TenantCustomRuleDO rule);

    TenantCustomRuleDO update(Long id, TenantCustomRuleDO rule);

    void delete(Long id);

    TenantCustomRuleDO getById(Long id);

    List<TenantCustomRuleDO> listByTenantId(Long tenantId);

    List<TenantCustomRuleDO> listEnabledByTenantId(Long tenantId);

    List<TenantCustomRuleDO> listByTenantIdAndType(Long tenantId, String ruleType);
}
