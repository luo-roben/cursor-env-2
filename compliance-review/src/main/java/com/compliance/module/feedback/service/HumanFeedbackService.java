package com.compliance.module.feedback.service;

import com.compliance.module.feedback.vo.FeedbackRespVO;
import com.compliance.module.feedback.vo.FeedbackSubmitReqVO;

import java.util.List;

public interface HumanFeedbackService {

    FeedbackRespVO submit(FeedbackSubmitReqVO reqVO);

    List<FeedbackRespVO> listByTaskId(Long taskId, Long tenantId);
}
