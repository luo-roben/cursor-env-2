package com.compliance.module.law.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LawSourceRespVO {

    private Long id;
    private String sourceId;
    private String title;
    private String issuer;
    private LocalDate issueDate;
    private LocalDate effectiveDate;
    private String docType;
    private String status;
    private String fullText;
    private String sourceUrl;
    private String parseStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
