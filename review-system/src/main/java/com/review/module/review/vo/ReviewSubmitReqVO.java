package com.review.module.review.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSubmitReqVO {

    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    @NotNull(message = "提交人ID不能为空")
    private Long submittedBy;

    @NotBlank(message = "内容类型不能为空")
    private String contentType;

    private String documentType;

    private String contractType;

    private String productType;

    private String channel;

    private String originalContent;

    private String fileUrl;
}
