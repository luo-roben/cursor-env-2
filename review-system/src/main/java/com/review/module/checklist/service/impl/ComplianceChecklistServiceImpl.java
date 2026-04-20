package com.review.module.checklist.service.impl;

import com.review.common.exception.ErrorCode;
import com.review.common.exception.ServiceException;
import com.review.module.checklist.entity.ComplianceChecklistDO;
import com.review.module.checklist.repository.ComplianceChecklistRepository;
import com.review.module.checklist.service.ComplianceChecklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceChecklistServiceImpl implements ComplianceChecklistService {

    private final ComplianceChecklistRepository complianceChecklistRepository;

    @Override
    @Transactional
    public ComplianceChecklistDO create(ComplianceChecklistDO checklist) {
        return complianceChecklistRepository.save(checklist);
    }

    @Override
    @Transactional
    public ComplianceChecklistDO update(Long id, ComplianceChecklistDO checklist) {
        ComplianceChecklistDO existing = complianceChecklistRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.CHECKLIST_NOT_FOUND));
        existing.setDocumentType(checklist.getDocumentType());
        existing.setContentType(checklist.getContentType());
        existing.setContractType(checklist.getContractType());
        existing.setProductType(checklist.getProductType());
        existing.setCheckItem(checklist.getCheckItem());
        existing.setCheckMethod(checklist.getCheckMethod());
        existing.setLawArticleId(checklist.getLawArticleId());
        existing.setConditionDesc(checklist.getConditionDesc());
        existing.setSeverityIfMissing(checklist.getSeverityIfMissing());
        existing.setEnabled(checklist.getEnabled());
        return complianceChecklistRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!complianceChecklistRepository.existsById(id)) {
            throw new ServiceException(ErrorCode.CHECKLIST_NOT_FOUND);
        }
        complianceChecklistRepository.deleteById(id);
    }

    @Override
    public ComplianceChecklistDO getById(Long id) {
        return complianceChecklistRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.CHECKLIST_NOT_FOUND));
    }

    @Override
    public List<ComplianceChecklistDO> listByContentType(String contentType) {
        return complianceChecklistRepository.findByContentTypeAndEnabledTrue(contentType);
    }

    @Override
    public List<ComplianceChecklistDO> listByDocumentType(String documentType) {
        return complianceChecklistRepository.findByDocumentTypeAndEnabledTrue(documentType);
    }

    @Override
    public List<ComplianceChecklistDO> listAll() {
        return complianceChecklistRepository.findByEnabledTrue();
    }
}
