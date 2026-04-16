package com.compliance.module.cases.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CaseRespVO {

    private Long id;
    private Long tenantId;
    private String source;
    private String contentType;
    private String productType;
    private String channel;
    private String reviewedContent;
    private String verdict;
    private String severity;
    private String reason;
    private String lawReferences;
    private String suggestion;
    private String aiOriginalVerdict;
    private String humanAction;
    private Long humanReviewerId;
    private Integer learningValueScore;
    private Boolean isTypical;
    private Boolean isShared;
    private LocalDateTime createdAt;
}
