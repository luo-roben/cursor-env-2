package com.review.module.review.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTaskRespVO {

    private Long id;
    private Long tenantId;
    private Long submittedBy;
    private String documentType;
    private String contentType;
    private String contractType;
    private String productType;
    private String channel;
    private String originalContent;
    private String fileUrl;
    private String parsedSegments;
    private String clauseTree;
    private String metadata;
    private String overallVerdict;
    private Integer riskScore;
    private String riskLevel;
    private String reviewStatus;
    private String enabledCards;
    private String llmModel;
    private Integer totalLatencyMs;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;

    private List<ReviewResultRespVO> results;
    private List<ReviewMissingElementRespVO> missingElements;
    private List<ReviewCardResultRespVO> cardResults;
}
