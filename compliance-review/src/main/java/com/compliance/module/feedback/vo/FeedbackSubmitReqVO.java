package com.compliance.module.feedback.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeedbackSubmitReqVO {

    private Long resultId;

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    @NotNull(message = "复核人ID不能为空")
    private Long reviewerId;

    @NotBlank(message = "反馈操作不能为空")
    private String action;

    private String finalVerdict;

    private String modifiedSeverity;

    private String modifiedReason;

    private String rejectReason;

    private String supplementIssue;

    private String comment;

    private Boolean isTypicalCase;
}
