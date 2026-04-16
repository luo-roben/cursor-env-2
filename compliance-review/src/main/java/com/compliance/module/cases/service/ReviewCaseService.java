package com.compliance.module.cases.service;

import com.compliance.common.result.PageResult;
import com.compliance.module.cases.vo.CaseCreateReqVO;
import com.compliance.module.cases.vo.CasePageReqVO;
import com.compliance.module.cases.vo.CaseRespVO;

public interface ReviewCaseService {

    CaseRespVO create(CaseCreateReqVO reqVO);

    PageResult<CaseRespVO> page(CasePageReqVO reqVO);

    CaseRespVO getById(Long id, Long tenantId);
}
