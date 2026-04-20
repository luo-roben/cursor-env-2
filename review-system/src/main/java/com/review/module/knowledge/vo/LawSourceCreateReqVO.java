package com.review.module.knowledge.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LawSourceCreateReqVO {

    @NotBlank(message = "来源ID不能为空")
    private String sourceId;

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "发布机构不能为空")
    private String issuer;

    private LocalDate issueDate;

    private LocalDate effectiveDate;

    @NotBlank(message = "文档类型不能为空")
    private String docType;

    private String status;

    private String fullText;

    private String sourceUrl;

    private String applicableDocTypes;
}
