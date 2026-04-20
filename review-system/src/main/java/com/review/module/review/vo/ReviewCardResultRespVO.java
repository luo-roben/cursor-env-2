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
public class ReviewCardResultRespVO {

    private Long id;
    private Long taskId;
    private Integer cardCategory;
    private String cardName;
    private Integer issueCount;
    private String maxSeverity;
    private String status;
    private String agentName;
    private String modelUsed;
    private Integer latencyMs;
    private Integer tokenConsumed;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
