package com.review.module.feedback.service;

import com.review.module.feedback.vo.FeedbackRespVO;
import com.review.module.feedback.vo.FeedbackSubmitReqVO;

import java.util.List;

public interface HumanFeedbackService {

    FeedbackRespVO submit(FeedbackSubmitReqVO reqVO);

    List<FeedbackRespVO> listByTaskId(Long taskId);

    FeedbackRespVO getById(Long id);
}
