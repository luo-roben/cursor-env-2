package com.review.module.review.service.impl;

import com.review.common.exception.ErrorCode;
import com.review.common.exception.ServiceException;
import com.review.common.result.PageResult;
import com.review.module.review.entity.ReviewCardResultDO;
import com.review.module.review.entity.ReviewMissingElementDO;
import com.review.module.review.entity.ReviewResultDO;
import com.review.module.review.entity.ReviewTaskDO;
import com.review.module.review.repository.ReviewCardResultRepository;
import com.review.module.review.repository.ReviewMissingElementRepository;
import com.review.module.review.repository.ReviewResultRepository;
import com.review.module.review.repository.ReviewTaskRepository;
import com.review.module.review.service.ReviewTaskService;
import com.review.module.review.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewTaskServiceImpl implements ReviewTaskService {

    private final ReviewTaskRepository reviewTaskRepository;
    private final ReviewResultRepository reviewResultRepository;
    private final ReviewMissingElementRepository reviewMissingElementRepository;
    private final ReviewCardResultRepository reviewCardResultRepository;

    @Override
    @Transactional
    public ReviewTaskRespVO submit(ReviewSubmitReqVO reqVO) {
        if (reqVO.getOriginalContent() == null && reqVO.getFileUrl() == null) {
            throw new ServiceException(ErrorCode.REVIEW_CONTENT_EMPTY);
        }

        ReviewTaskDO task = ReviewTaskDO.builder()
                .tenantId(reqVO.getTenantId())
                .submittedBy(reqVO.getSubmittedBy())
                .contentType(reqVO.getContentType())
                .documentType(reqVO.getDocumentType() != null ? reqVO.getDocumentType() : "OTHER")
                .contractType(reqVO.getContractType())
                .productType(reqVO.getProductType())
                .channel(reqVO.getChannel())
                .originalContent(reqVO.getOriginalContent())
                .fileUrl(reqVO.getFileUrl())
                .reviewStatus("PENDING")
                .build();
        ReviewTaskDO saved = reviewTaskRepository.save(task);
        return toRespVO(saved);
    }

    @Override
    public ReviewTaskRespVO getById(Long id, Long tenantId) {
        ReviewTaskDO task;
        if (tenantId != null) {
            task = reviewTaskRepository.findByIdAndTenantId(id, tenantId)
                    .orElseThrow(() -> new ServiceException(ErrorCode.REVIEW_TASK_NOT_FOUND));
        } else {
            task = reviewTaskRepository.findById(id)
                    .orElseThrow(() -> new ServiceException(ErrorCode.REVIEW_TASK_NOT_FOUND));
        }

        ReviewTaskRespVO respVO = toRespVO(task);

        List<ReviewResultDO> results = reviewResultRepository.findByTaskIdOrderBySegmentIndexAsc(task.getId());
        respVO.setResults(results.stream().map(this::toResultRespVO).toList());

        List<ReviewMissingElementDO> missingElements = reviewMissingElementRepository.findByTaskId(task.getId());
        respVO.setMissingElements(missingElements.stream().map(this::toMissingElementRespVO).toList());

        List<ReviewCardResultDO> cardResults = reviewCardResultRepository.findByTaskId(task.getId());
        respVO.setCardResults(cardResults.stream().map(this::toCardResultRespVO).toList());

        return respVO;
    }

    @Override
    public PageResult<ReviewTaskRespVO> page(Long tenantId, String reviewStatus, String documentType,
                                              String contentType, Integer pageNum, Integer pageSize) {
        PageRequest pageRequest = PageRequest.of(
                pageNum - 1,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReviewTaskDO> page = reviewTaskRepository.findByTenantIdAndFilters(
                tenantId, reviewStatus, documentType, contentType, pageRequest);
        return PageResult.of(
                page.getContent().stream().map(this::toRespVO).toList(),
                page.getTotalElements(),
                pageNum,
                pageSize);
    }

    private ReviewTaskRespVO toRespVO(ReviewTaskDO entity) {
        return ReviewTaskRespVO.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .submittedBy(entity.getSubmittedBy())
                .documentType(entity.getDocumentType())
                .contentType(entity.getContentType())
                .contractType(entity.getContractType())
                .productType(entity.getProductType())
                .channel(entity.getChannel())
                .originalContent(entity.getOriginalContent())
                .fileUrl(entity.getFileUrl())
                .parsedSegments(entity.getParsedSegments())
                .clauseTree(entity.getClauseTree())
                .metadata(entity.getMetadata())
                .overallVerdict(entity.getOverallVerdict())
                .riskScore(entity.getRiskScore())
                .riskLevel(entity.getRiskLevel())
                .reviewStatus(entity.getReviewStatus())
                .enabledCards(entity.getEnabledCards())
                .llmModel(entity.getLlmModel())
                .totalLatencyMs(entity.getTotalLatencyMs())
                .createdAt(entity.getCreatedAt())
                .completedAt(entity.getCompletedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ReviewResultRespVO toResultRespVO(ReviewResultDO entity) {
        return ReviewResultRespVO.builder()
                .id(entity.getId())
                .taskId(entity.getTaskId())
                .tenantId(entity.getTenantId())
                .cardCategory(entity.getCardCategory())
                .segmentIndex(entity.getSegmentIndex())
                .clauseId(entity.getClauseId())
                .crossRefClauses(entity.getCrossRefClauses())
                .originalText(entity.getOriginalText())
                .matchedText(entity.getMatchedText())
                .charOffsetStart(entity.getCharOffsetStart())
                .charOffsetEnd(entity.getCharOffsetEnd())
                .verdict(entity.getVerdict())
                .confidence(entity.getConfidence())
                .issueType(entity.getIssueType())
                .severity(entity.getSeverity())
                .description(entity.getDescription())
                .lawArticleId(entity.getLawArticleId())
                .citedArticleCode(entity.getCitedArticleCode())
                .citedLawName(entity.getCitedLawName())
                .citationStatus(entity.getCitationStatus())
                .verifiedOriginalText(entity.getVerifiedOriginalText())
                .suggestion(entity.getSuggestion())
                .suggestionType(entity.getSuggestionType())
                .revisedText(entity.getRevisedText())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private ReviewMissingElementRespVO toMissingElementRespVO(ReviewMissingElementDO entity) {
        return ReviewMissingElementRespVO.builder()
                .id(entity.getId())
                .taskId(entity.getTaskId())
                .tenantId(entity.getTenantId())
                .cardCategory(entity.getCardCategory())
                .element(entity.getElement())
                .requirement(entity.getRequirement())
                .lawArticleId(entity.getLawArticleId())
                .severity(entity.getSeverity())
                .suggestion(entity.getSuggestion())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private ReviewCardResultRespVO toCardResultRespVO(ReviewCardResultDO entity) {
        return ReviewCardResultRespVO.builder()
                .id(entity.getId())
                .taskId(entity.getTaskId())
                .cardCategory(entity.getCardCategory())
                .cardName(entity.getCardName())
                .issueCount(entity.getIssueCount())
                .maxSeverity(entity.getMaxSeverity())
                .status(entity.getStatus())
                .agentName(entity.getAgentName())
                .modelUsed(entity.getModelUsed())
                .latencyMs(entity.getLatencyMs())
                .tokenConsumed(entity.getTokenConsumed())
                .errorMessage(entity.getErrorMessage())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
