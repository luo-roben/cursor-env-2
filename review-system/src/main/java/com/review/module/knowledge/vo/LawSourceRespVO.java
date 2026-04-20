package com.review.module.knowledge.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private String applicableDocTypes;
    private String parseStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
