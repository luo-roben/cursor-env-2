package com.compliance.module.feedback.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FeedbackRespVO {

    private Long id;
    private Long resultId;
    private Long taskId;
    private Long tenantId;
    private Long reviewerId;
    private String action;
    private String originalVerdict;
    private String finalVerdict;
    private String modifiedSeverity;
    private String modifiedReason;
    private String rejectReason;
    private String supplementIssue;
    private String comment;
    private Boolean isTypicalCase;
    private LocalDateTime reviewedAt;
}
