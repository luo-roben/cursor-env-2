package com.compliance.module.law.service;

import com.compliance.common.result.PageResult;
import com.compliance.module.law.vo.LawSourceCreateReqVO;
import com.compliance.module.law.vo.LawSourcePageReqVO;
import com.compliance.module.law.vo.LawSourceRespVO;

public interface LawSourceService {

    LawSourceRespVO create(LawSourceCreateReqVO reqVO);

    LawSourceRespVO getById(Long id);

    PageResult<LawSourceRespVO> page(LawSourcePageReqVO reqVO);

    void updateParseStatus(Long id, String parseStatus);
}
