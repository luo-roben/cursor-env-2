package com.compliance.module.cases.service;

import com.compliance.module.cases.entity.ReviewCaseDO;
import com.compliance.module.cases.repository.ReviewCaseRepository;
import com.compliance.module.feedback.entity.HumanFeedbackDO;
import com.compliance.module.review.entity.ReviewResultDO;
import com.compliance.module.review.entity.ReviewTaskDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseSedimentationService {

    private final ReviewCaseRepository reviewCaseRepository;

    @Transactional
    public void sedimentFromFeedback(HumanFeedbackDO feedback, ReviewResultDO result, ReviewTaskDO task) {
        int learningValueScore = calculateLearningValueScore(feedback.getAction());

        ReviewCaseDO reviewCase = ReviewCaseDO.builder()
                .tenantId(feedback.getTenantId())
                .source("internal_review")
                .contentType(task.getContentType())
                .productType(task.getProductType())
                .channel(task.getChannel())
                .reviewedContent(result != null ? result.getOriginalText() : task.getOriginalContent())
                .verdict(feedback.getFinalVerdict() != null ? feedback.getFinalVerdict() : (result != null ? result.getVerdict() : task.getOverallVerdict()))
                .severity(feedback.getModifiedSeverity() != null ? feedback.getModifiedSeverity() : (result != null ? result.getSeverity() : null))
                .reason(feedback.getModifiedReason() != null ? feedback.getModifiedReason() : (result != null ? result.getDescription() : null))
                .suggestion(result != null ? result.getSuggestion() : null)
                .aiOriginalVerdict(result != null ? result.getVerdict() : task.getOverallVerdict())
                .humanAction(feedback.getAction())
                .humanReviewerId(feedback.getReviewerId())
                .learningValueScore(learningValueScore)
                .isTypical(feedback.getIsTypicalCase() != null ? feedback.getIsTypicalCase() : false)
                .build();

        reviewCase = reviewCaseRepository.save(reviewCase);
        log.info("Sedimented case from feedback: caseId={}, feedbackId={}, learningValueScore={}",
                reviewCase.getId(), feedback.getId(), learningValueScore);
    }

    private int calculateLearningValueScore(String action) {
        return switch (action) {
            case "supplemented" -> 3;
            case "rejected" -> 2;
            case "modified" -> 1;
            case "confirmed" -> 0;
            default -> 0;
        };
    }
}
