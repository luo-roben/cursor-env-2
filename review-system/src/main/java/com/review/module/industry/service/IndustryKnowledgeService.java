package com.review.module.industry.service;

import com.review.common.exception.ErrorCode;
import com.review.common.exception.ServiceException;
import com.review.module.industry.entity.IndustryKnowledgeDO;
import com.review.module.industry.repository.IndustryKnowledgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndustryKnowledgeService {

    private final IndustryKnowledgeRepository industryKnowledgeRepository;

    public IndustryKnowledgeDO create(IndustryKnowledgeDO entity) {
        return industryKnowledgeRepository.save(entity);
    }

    public IndustryKnowledgeDO getById(Long id) {
        return industryKnowledgeRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
    }

    public List<IndustryKnowledgeDO> listAll() {
        return industryKnowledgeRepository.findAll();
    }

    public List<IndustryKnowledgeDO> listByIndustry(String industry) {
        return industryKnowledgeRepository.findByIndustryAndStatus(industry, "published");
    }

    public List<IndustryKnowledgeDO> listPublished() {
        return industryKnowledgeRepository.findByStatus("published");
    }

    public IndustryKnowledgeDO update(Long id, IndustryKnowledgeDO updated) {
        IndustryKnowledgeDO existing = getById(id);
        existing.setIndustry(updated.getIndustry());
        existing.setKnowledgeType(updated.getKnowledgeType());
        existing.setTitle(updated.getTitle());
        existing.setContent(updated.getContent());
        existing.setApplicableContractTypes(updated.getApplicableContractTypes());
        existing.setApplicableContentTypes(updated.getApplicableContentTypes());
        existing.setSource(updated.getSource());
        existing.setStatus(updated.getStatus());
        return industryKnowledgeRepository.save(existing);
    }

    public void delete(Long id) {
        industryKnowledgeRepository.deleteById(id);
    }
}
