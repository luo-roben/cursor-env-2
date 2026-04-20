package com.review.module.pipeline.dto;

import com.review.module.agent.dto.ReviewIssue;
import com.review.module.review.entity.ReviewCardResultDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewPipelineResult {

    private List<ReviewIssue> results;
    private List<MissingElementInfo> missingElements;
    private List<ReviewCardResultDO> cardResults;
    private String overallVerdict;
    private int riskScore;
    private String riskLevel;
    private int totalLatencyMs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissingElementInfo {
        private int cardCategory;
        private String element;
        private String requirement;
        private String severity;
        private String suggestion;
        private Long lawArticleId;
    }
}
