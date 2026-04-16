package com.compliance.module.checklist.repository;

import com.compliance.module.checklist.entity.ComplianceChecklistDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplianceChecklistRepository extends JpaRepository<ComplianceChecklistDO, Long> {

    List<ComplianceChecklistDO> findByContentTypeAndEnabledTrue(String contentType);

    List<ComplianceChecklistDO> findByContentTypeAndProductTypeAndEnabledTrue(String contentType, String productType);

    List<ComplianceChecklistDO> findByTenantIdAndEnabledTrue(Long tenantId);
}
