package com.compliance.module.checklist.service;

import com.compliance.module.checklist.vo.ChecklistCreateReqVO;
import com.compliance.module.checklist.vo.ChecklistRespVO;

import java.util.List;

public interface ComplianceChecklistService {

    ChecklistRespVO create(ChecklistCreateReqVO reqVO);

    List<ChecklistRespVO> listByContentType(String contentType, String productType);

    void delete(Long id);
}
