package com.review.module.feedback.service.impl;

import com.review.common.exception.ErrorCode;
import com.review.common.exception.ServiceException;
import com.review.module.cases.service.CaseSedimentationService;
import com.review.module.feedback.entity.HumanFeedbackDO;
import com.review.module.feedback.repository.HumanFeedbackRepository;
import com.review.module.feedback.service.HumanFeedbackService;
import com.review.module.feedback.vo.FeedbackRespVO;
import com.review.module.feedback.vo.FeedbackSubmitReqVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HumanFeedbackServiceImpl implements HumanFeedbackService {

    private final HumanFeedbackRepository humanFeedbackRepository;
    private final CaseSedimentationService caseSedimentationService;

    @Override
    @Transactional
    public FeedbackRespVO submit(FeedbackSubmitReqVO reqVO) {
        HumanFeedbackDO entity = HumanFeedbackDO.builder()
                .resultId(reqVO.getResultId())
                .taskId(reqVO.getTaskId())
                .tenantId(reqVO.getTenantId())
                .reviewerId(reqVO.getReviewerId())
                .action(reqVO.getAction())
                .originalVerdict(reqVO.getOriginalVerdict())
                .finalVerdict(reqVO.getFinalVerdict())
                .modifiedSeverity(reqVO.getModifiedSeverity())
                .modifiedReason(reqVO.getModifiedReason())
                .rejectReason(reqVO.getRejectReason())
                .supplementIssue(reqVO.getSupplementIssue())
                .comment(reqVO.getComment())
                .isTypicalCase(reqVO.getIsTypicalCase() != null ? reqVO.getIsTypicalCase() : false)
                .build();

        entity = humanFeedbackRepository.save(entity);
        log.info("Feedback submitted: id={}, taskId={}, action={}", entity.getId(), entity.getTaskId(), entity.getAction());

        try {
            caseSedimentationService.sediment(entity.getId());
        } catch (Exception e) {
            log.error("Case sedimentation failed for feedback id={}, but feedback was saved successfully", entity.getId(), e);
        }

        return toRespVO(entity);
    }

    @Override
    public List<FeedbackRespVO> listByTaskId(Long taskId) {
        return humanFeedbackRepository.findByTaskId(taskId).stream()
                .map(this::toRespVO)
                .collect(Collectors.toList());
    }

    @Override
    public FeedbackRespVO getById(Long id) {
        HumanFeedbackDO entity = humanFeedbackRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.FEEDBACK_NOT_FOUND));
        return toRespVO(entity);
    }

    private FeedbackRespVO toRespVO(HumanFeedbackDO entity) {
        return FeedbackRespVO.builder()
                .id(entity.getId())
                .resultId(entity.getResultId())
                .taskId(entity.getTaskId())
                .tenantId(entity.getTenantId())
                .reviewerId(entity.getReviewerId())
                .action(entity.getAction())
                .originalVerdict(entity.getOriginalVerdict())
                .finalVerdict(entity.getFinalVerdict())
                .modifiedSeverity(entity.getModifiedSeverity())
                .modifiedReason(entity.getModifiedReason())
                .rejectReason(entity.getRejectReason())
                .supplementIssue(entity.getSupplementIssue())
                .comment(entity.getComment())
                .isTypicalCase(entity.getIsTypicalCase())
                .reviewedAt(entity.getReviewedAt())
                .build();
    }
}
