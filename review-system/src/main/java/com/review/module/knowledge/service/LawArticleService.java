package com.review.module.knowledge.service;

import com.review.common.result.PageResult;
import com.review.module.knowledge.vo.LawArticleCreateReqVO;
import com.review.module.knowledge.vo.LawArticlePageReqVO;
import com.review.module.knowledge.vo.LawArticleRespVO;

public interface LawArticleService {

    LawArticleRespVO create(LawArticleCreateReqVO reqVO);

    LawArticleRespVO getById(Long id);

    PageResult<LawArticleRespVO> page(LawArticlePageReqVO reqVO);

    void publish(Long id);

    LawArticleRespVO update(Long id, LawArticleCreateReqVO reqVO);

    void deprecate(Long id);

    void delete(Long id);
}
