package com.review.module.review.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewMissingElementRespVO {

    private Long id;
    private Long taskId;
    private Long tenantId;
    private Integer cardCategory;
    private String element;
    private String requirement;
    private Long lawArticleId;
    private String severity;
    private String suggestion;
    private LocalDateTime createdAt;
}
