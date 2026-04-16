package com.compliance.module.feedback.service.impl;

import com.compliance.common.exception.ErrorCode;
import com.compliance.common.exception.ServiceException;
import com.compliance.module.cases.service.CaseSedimentationService;
import com.compliance.module.feedback.entity.HumanFeedbackDO;
import com.compliance.module.feedback.repository.HumanFeedbackRepository;
import com.compliance.module.feedback.service.HumanFeedbackService;
import com.compliance.module.feedback.vo.FeedbackRespVO;
import com.compliance.module.feedback.vo.FeedbackSubmitReqVO;
import com.compliance.module.review.entity.ReviewResultDO;
import com.compliance.module.review.entity.ReviewTaskDO;
import com.compliance.module.review.repository.ReviewResultRepository;
import com.compliance.module.review.repository.ReviewTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HumanFeedbackServiceImpl implements HumanFeedbackService {

    private final HumanFeedbackRepository humanFeedbackRepository;
    private final ReviewTaskRepository reviewTaskRepository;
    private final ReviewResultRepository reviewResultRepository;

    @Autowired(required = false)
    private CaseSedimentationService caseSedimentationService;

    @Override
    @Transactional
    public FeedbackRespVO submit(FeedbackSubmitReqVO reqVO) {
        ReviewTaskDO task = reviewTaskRepository.findByIdAndTenantId(reqVO.getTaskId(), reqVO.getTenantId())
                .orElseThrow(() -> new ServiceException(ErrorCode.FEEDBACK_TASK_NOT_FOUND));

        ReviewResultDO result = null;
        if (reqVO.getResultId() != null) {
            result = reviewResultRepository.findById(reqVO.getResultId()).orElse(null);
        }

        HumanFeedbackDO feedback = HumanFeedbackDO.builder()
                .resultId(reqVO.getResultId())
                .taskId(reqVO.getTaskId())
                .tenantId(reqVO.getTenantId())
                .reviewerId(reqVO.getReviewerId())
                .action(reqVO.getAction())
                .originalVerdict(result != null ? result.getVerdict() : task.getOverallVerdict())
                .finalVerdict(reqVO.getFinalVerdict())
                .modifiedSeverity(reqVO.getModifiedSeverity())
                .modifiedReason(reqVO.getModifiedReason())
                .rejectReason(reqVO.getRejectReason())
                .supplementIssue(reqVO.getSupplementIssue())
                .comment(reqVO.getComment())
                .isTypicalCase(reqVO.getIsTypicalCase() != null ? reqVO.getIsTypicalCase() : false)
                .build();

        feedback = humanFeedbackRepository.save(feedback);
        log.info("Saved human feedback: id={}, taskId={}, action={}", feedback.getId(), feedback.getTaskId(), feedback.getAction());

        task.setReviewStatus("human_reviewed");
        reviewTaskRepository.save(task);
        log.info("Updated task status to human_reviewed: taskId={}", task.getId());

        if (caseSedimentationService != null) {
            try {
                caseSedimentationService.sedimentFromFeedback(feedback, result, task);
                log.info("Case sedimentation completed for feedback: id={}", feedback.getId());
            } catch (Exception e) {
                log.warn("Case sedimentation failed for feedback: id={}, error={}", feedback.getId(), e.getMessage());
            }
        }

        return toRespVO(feedback);
    }

    @Override
    public List<FeedbackRespVO> listByTaskId(Long taskId, Long tenantId) {
        List<HumanFeedbackDO> feedbacks = humanFeedbackRepository.findByTaskIdAndTenantId(taskId, tenantId);
        return feedbacks.stream().map(this::toRespVO).toList();
    }

    private FeedbackRespVO toRespVO(HumanFeedbackDO entity) {
        FeedbackRespVO vo = new FeedbackRespVO();
        vo.setId(entity.getId());
        vo.setResultId(entity.getResultId());
        vo.setTaskId(entity.getTaskId());
        vo.setTenantId(entity.getTenantId());
        vo.setReviewerId(entity.getReviewerId());
        vo.setAction(entity.getAction());
        vo.setOriginalVerdict(entity.getOriginalVerdict());
        vo.setFinalVerdict(entity.getFinalVerdict());
        vo.setModifiedSeverity(entity.getModifiedSeverity());
        vo.setModifiedReason(entity.getModifiedReason());
        vo.setRejectReason(entity.getRejectReason());
        vo.setSupplementIssue(entity.getSupplementIssue());
        vo.setComment(entity.getComment());
        vo.setIsTypicalCase(entity.getIsTypicalCase());
        vo.setReviewedAt(entity.getReviewedAt());
        return vo;
    }
}
