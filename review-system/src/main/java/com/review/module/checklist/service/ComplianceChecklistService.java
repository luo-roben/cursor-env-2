package com.review.module.checklist.service;

import com.review.module.checklist.entity.ComplianceChecklistDO;

import java.util.List;

public interface ComplianceChecklistService {

    ComplianceChecklistDO create(ComplianceChecklistDO checklist);

    ComplianceChecklistDO update(Long id, ComplianceChecklistDO checklist);

    void delete(Long id);

    ComplianceChecklistDO getById(Long id);

    List<ComplianceChecklistDO> listByContentType(String contentType);

    List<ComplianceChecklistDO> listByDocumentType(String documentType);

    List<ComplianceChecklistDO> listAll();
}
