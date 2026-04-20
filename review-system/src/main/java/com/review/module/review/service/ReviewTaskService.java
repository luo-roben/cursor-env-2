package com.review.module.review.service;

import com.review.common.result.PageResult;
import com.review.module.review.vo.ReviewSubmitReqVO;
import com.review.module.review.vo.ReviewTaskRespVO;

public interface ReviewTaskService {

    ReviewTaskRespVO submit(ReviewSubmitReqVO reqVO);

    ReviewTaskRespVO getById(Long id, Long tenantId);

    PageResult<ReviewTaskRespVO> page(Long tenantId, String reviewStatus, String documentType,
                                       String contentType, Integer pageNum, Integer pageSize);
}
