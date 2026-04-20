package com.review.module.knowledge.service;

import com.review.common.result.PageResult;
import com.review.module.knowledge.vo.LawSourceCreateReqVO;
import com.review.module.knowledge.vo.LawSourcePageReqVO;
import com.review.module.knowledge.vo.LawSourceRespVO;

public interface LawSourceService {

    LawSourceRespVO create(LawSourceCreateReqVO reqVO);

    LawSourceRespVO getById(Long id);

    PageResult<LawSourceRespVO> page(LawSourcePageReqVO reqVO);

    void updateParseStatus(Long id, String parseStatus);
}
