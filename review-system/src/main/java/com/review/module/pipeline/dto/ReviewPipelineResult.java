package com.review.module.pipeline.dto;

import com.review.module.agent.dto.ReviewIssue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewPipelineResult {

    private List<ReviewIssue> results;
    private List<String> missingElements;
    private Map<Integer, List<ReviewIssue>> cardResults;
    private String overallVerdict;
    private int riskScore;
    private String riskLevel;
    private long totalLatencyMs;
}
