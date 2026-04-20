package com.review.module.cases.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.infrastructure.vector.VectorStoreService;
import com.review.module.cases.entity.ReviewCaseDO;
import com.review.module.cases.repository.ReviewCaseRepository;
import com.review.module.cases.service.CaseSedimentationService;
import com.review.module.feedback.entity.HumanFeedbackDO;
import com.review.module.feedback.repository.HumanFeedbackRepository;
import com.review.module.review.entity.ReviewResultDO;
import com.review.module.review.entity.ReviewTaskDO;
import com.review.module.review.repository.ReviewResultRepository;
import com.review.module.review.repository.ReviewTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseSedimentationServiceImpl implements CaseSedimentationService {

    private static final Map<String, Integer> LEARNING_VALUE_SCORES = Map.of(
            "SUPPLEMENTED", 3,
            "REJECTED", 2,
            "MODIFIED", 1,
            "CONFIRMED", 0
    );

    private final HumanFeedbackRepository humanFeedbackRepository;
    private final ReviewResultRepository reviewResultRepository;
    private final ReviewTaskRepository reviewTaskRepository;
    private final ReviewCaseRepository reviewCaseRepository;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private VectorStoreService vectorStoreService;

    @Override
    public int calculateLearningValue(String humanAction) {
        if (humanAction == null) return 0;
        return LEARNING_VALUE_SCORES.getOrDefault(humanAction.toUpperCase(), 0);
    }

    @Override
    @Transactional
    public void sediment(Long feedbackId) {
        log.info("Case sedimentation triggered for feedback: {}", feedbackId);

        HumanFeedbackDO feedback = humanFeedbackRepository.findById(feedbackId).orElse(null);
        if (feedback == null) {
            log.warn("Feedback not found for sedimentation: {}", feedbackId);
            return;
        }

        ReviewResultDO result = null;
        if (feedback.getResultId() != null) {
            result = reviewResultRepository.findById(feedback.getResultId()).orElse(null);
        }
        if (result == null) {
            List<ReviewResultDO> results = reviewResultRepository.findByTaskIdOrderBySegmentIndexAsc(feedback.getTaskId());
            if (!results.isEmpty()) {
                result = results.get(0);
            }
        }

        ReviewTaskDO task = reviewTaskRepository.findById(feedback.getTaskId()).orElse(null);
        if (task == null) {
            log.warn("Task not found for sedimentation, taskId: {}", feedback.getTaskId());
            return;
        }

        int learningValueScore = calculateLearningValue(feedback.getAction());

        String reviewedContent = result != null && result.getOriginalText() != null
                ? result.getOriginalText()
                : task.getOriginalContent();

        String severity = result != null && result.getSeverity() != null
                ? result.getSeverity()
                : feedback.getModifiedSeverity();

        String reason = feedback.getModifiedReason();
        if (reason == null || reason.isBlank()) {
            reason = feedback.getRejectReason();
        }
        if (reason == null || reason.isBlank()) {
            reason = "人工确认";
        }

        String lawReferences = buildLawReferences(result);
        String suggestion = result != null ? result.getSuggestion() : null;
        String clauseId = result != null ? result.getClauseId() : null;
        String clauseText = result != null ? result.getMatchedText() : null;

        ReviewCaseDO reviewCase = ReviewCaseDO.builder()
                .tenantId(task.getTenantId())
                .source("internal_review")
                .contentType(task.getContentType())
                .productType(task.getProductType())
                .contractType(task.getContractType())
                .documentType(task.getDocumentType())
                .reviewedContent(reviewedContent != null ? reviewedContent : "")
                .verdict(feedback.getFinalVerdict() != null ? feedback.getFinalVerdict() : "UNKNOWN")
                .severity(severity)
                .reason(reason)
                .lawReferences(lawReferences)
                .suggestion(suggestion)
                .aiOriginalVerdict(feedback.getOriginalVerdict())
                .humanAction(feedback.getAction())
                .humanReviewerId(feedback.getReviewerId())
                .learningValueScore(learningValueScore)
                .isTypical(feedback.getIsTypicalCase() != null ? feedback.getIsTypicalCase() : false)
                .build();

        reviewCase = reviewCaseRepository.save(reviewCase);
        log.info("Review case created: id={}, verdict={}", reviewCase.getId(), reviewCase.getVerdict());

        try {
            indexInVectorStore(reviewCase);
        } catch (Exception e) {
            log.warn("Failed to index review case in vector store, caseId={}: {}", reviewCase.getId(), e.getMessage());
        }
    }

    private String buildLawReferences(ReviewResultDO result) {
        if (result == null || result.getCitedArticleCode() == null) {
            return null;
        }
        try {
            Map<String, String> ref = new HashMap<>();
            ref.put("articleCode", result.getCitedArticleCode());
            if (result.getCitedLawName() != null) {
                ref.put("lawName", result.getCitedLawName());
            }
            return objectMapper.writeValueAsString(List.of(ref));
        } catch (Exception e) {
            log.warn("Failed to serialize law references: {}", e.getMessage());
            return null;
        }
    }

    private void indexInVectorStore(ReviewCaseDO reviewCase) {
        if (vectorStoreService == null) {
            return;
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", reviewCase.getId());
        metadata.put("reviewedContent", reviewCase.getReviewedContent());
        metadata.put("verdict", reviewCase.getVerdict());
        metadata.put("severity", reviewCase.getSeverity());
        metadata.put("reason", reviewCase.getReason());
        metadata.put("contentType", reviewCase.getContentType());
        metadata.put("isTypical", reviewCase.getIsTypical());

        vectorStoreService.index(
                reviewCase.getId().toString(),
                reviewCase.getReviewedContent(),
                "review_cases_vectors",
                metadata
        );
        log.info("Review case indexed in vector store: id={}", reviewCase.getId());
    }
}
