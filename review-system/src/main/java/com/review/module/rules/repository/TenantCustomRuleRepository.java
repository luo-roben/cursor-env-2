package com.review.module.rules.repository;

import com.review.module.rules.entity.TenantCustomRuleDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TenantCustomRuleRepository extends JpaRepository<TenantCustomRuleDO, Long> {

    List<TenantCustomRuleDO> findByTenantIdAndEnabledTrue(Long tenantId);

    List<TenantCustomRuleDO> findByTenantIdAndRuleTypeAndEnabledTrue(Long tenantId, String ruleType);

    List<TenantCustomRuleDO> findByTenantId(Long tenantId);
}
