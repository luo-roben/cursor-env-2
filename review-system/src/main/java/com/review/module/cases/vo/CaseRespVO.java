package com.review.module.cases.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseRespVO {

    private Long id;
    private Long tenantId;
    private String source;
    private String documentType;
    private String contentType;
    private String contractType;
    private String productType;
    private String channel;
    private Integer reviewCard;
    private String reviewedContent;
    private String verdict;
    private String severity;
    private String reason;
    private String lawReferences;
    private String suggestion;
    private String suggestionType;
    private String aiOriginalVerdict;
    private String humanAction;
    private Long humanReviewerId;
    private Integer learningValueScore;
    private Boolean isTypical;
    private Boolean isShared;
    private BigDecimal decayWeight;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
