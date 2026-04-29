package com.review.module.industry.repository;

import com.review.module.industry.entity.IndustryKnowledgeDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndustryKnowledgeRepository extends JpaRepository<IndustryKnowledgeDO, Long> {

    List<IndustryKnowledgeDO> findByIndustryAndStatus(String industry, String status);

    List<IndustryKnowledgeDO> findByStatus(String status);

    List<IndustryKnowledgeDO> findByKnowledgeTypeAndStatus(String knowledgeType, String status);
}
