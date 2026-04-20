package com.review.module.knowledge.repository;

import com.review.module.knowledge.entity.ContractTemplateClauseDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractTemplateClauseRepository extends JpaRepository<ContractTemplateClauseDO, Long> {

    List<ContractTemplateClauseDO> findByTemplateIdOrderByClauseIdAsc(String templateId);

    List<ContractTemplateClauseDO> findByTemplateIdAndClauseRole(String templateId, String clauseRole);

    List<ContractTemplateClauseDO> findByTemplateIdAndIsRequired(String templateId, Integer isRequired);
}
