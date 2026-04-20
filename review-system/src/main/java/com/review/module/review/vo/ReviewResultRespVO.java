package com.review.module.review.vo;

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
public class ReviewResultRespVO {

    private Long id;
    private Long taskId;
    private Long tenantId;
    private Integer cardCategory;
    private Integer segmentIndex;
    private String clauseId;
    private String crossRefClauses;
    private String originalText;
    private String matchedText;
    private Integer charOffsetStart;
    private Integer charOffsetEnd;
    private String verdict;
    private BigDecimal confidence;
    private String issueType;
    private String severity;
    private String description;
    private Long lawArticleId;
    private String citedArticleCode;
    private String citedLawName;
    private String citationStatus;
    private String verifiedOriginalText;
    private String suggestion;
    private String suggestionType;
    private String revisedText;
    private LocalDateTime createdAt;
}
