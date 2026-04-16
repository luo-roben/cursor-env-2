package com.compliance.module.cases.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CaseCreateReqVO {

    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    private String source;

    private String contentType;

    private String productType;

    private String channel;

    @NotBlank(message = "审查内容不能为空")
    private String reviewedContent;

    @NotBlank(message = "判定结果不能为空")
    private String verdict;

    private String severity;

    private String reason;

    private String lawReferences;

    private String suggestion;
}
