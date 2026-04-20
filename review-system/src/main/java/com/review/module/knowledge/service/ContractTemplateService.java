package com.review.module.knowledge.service;

import com.review.common.result.PageResult;
import com.review.module.knowledge.vo.*;

import java.util.List;

public interface ContractTemplateService {

    ContractTemplateRespVO create(ContractTemplateCreateReqVO reqVO);

    ContractTemplateRespVO getById(Long id);

    PageResult<ContractTemplateRespVO> page(String templateName, String contractType, String status, Long tenantId, Integer pageNum, Integer pageSize);

    ContractTemplateClauseRespVO addClause(Long templateId, ContractTemplateClauseCreateReqVO reqVO);

    List<ContractTemplateClauseRespVO> listClauses(Long templateId);

    void publish(Long id);
}
