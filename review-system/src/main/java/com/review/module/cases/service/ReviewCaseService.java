package com.review.module.cases.service;

import com.review.common.result.PageResult;
import com.review.module.cases.vo.CaseCreateReqVO;
import com.review.module.cases.vo.CasePageReqVO;
import com.review.module.cases.vo.CaseRespVO;

import java.util.List;

public interface ReviewCaseService {

    CaseRespVO create(CaseCreateReqVO reqVO);

    CaseRespVO getById(Long id);

    PageResult<CaseRespVO> page(CasePageReqVO reqVO);

    List<CaseRespVO> listByContentType(String contentType);

    List<CaseRespVO> listTypicalCases();
}
