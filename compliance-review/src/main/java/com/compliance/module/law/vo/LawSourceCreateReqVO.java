package com.compliance.module.law.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LawSourceCreateReqVO {

    @NotBlank(message = "法规来源ID不能为空")
    private String sourceId;

    @NotBlank(message = "法规标题不能为空")
    private String title;

    @NotBlank(message = "发布机构不能为空")
    private String issuer;

    private LocalDate issueDate;

    private LocalDate effectiveDate;

    @NotBlank(message = "文档类型不能为空")
    private String docType;

    @NotBlank(message = "法规全文不能为空")
    private String fullText;

    private String sourceUrl;
}
