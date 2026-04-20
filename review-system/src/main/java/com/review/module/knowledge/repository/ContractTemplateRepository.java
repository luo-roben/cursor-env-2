package com.review.module.knowledge.repository;

import com.review.module.knowledge.entity.ContractTemplateDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractTemplateRepository extends JpaRepository<ContractTemplateDO, Long> {

    List<ContractTemplateDO> findByContractTypeAndStatus(String contractType, String status);

    @Query("SELECT ct FROM ContractTemplateDO ct WHERE ct.tenantId = :tenantId OR ct.tenantId IS NULL")
    List<ContractTemplateDO> findByTenantIdOrTenantIdIsNull(@Param("tenantId") Long tenantId);

    @Query("SELECT ct FROM ContractTemplateDO ct WHERE "
            + "(:templateName IS NULL OR ct.templateName LIKE CONCAT('%', :templateName, '%')) "
            + "AND (:contractType IS NULL OR ct.contractType = :contractType) "
            + "AND (:status IS NULL OR ct.status = :status) "
            + "AND (:tenantId IS NULL OR ct.tenantId = :tenantId OR ct.tenantId IS NULL)")
    Page<ContractTemplateDO> findByFilters(
            @Param("templateName") String templateName,
            @Param("contractType") String contractType,
            @Param("status") String status,
            @Param("tenantId") Long tenantId,
            Pageable pageable);
}
