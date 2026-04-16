package com.compliance.module.checklist.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChecklistCreateReqVO {

    @NotBlank(message = "内容类型不能为空")
    private String contentType;

    private String productType;

    @NotBlank(message = "检查项不能为空")
    private String checkItem;

    @NotBlank(message = "检查方法不能为空")
    private String checkMethod;

    private Long lawArticleId;

    private String conditionDesc;

    private String severityIfMissing;

    private Long tenantId;
}
