package com.review.module.feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsistencyReport {

    private int totalPairs;
    private double agreementRate;
    private double kappaScore;
    private List<ReviewerPairKappa> perPairKappa;
    private List<Disagreement> disagreements;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Disagreement {
        private Long taskId;
        private Long reviewer1;
        private Long reviewer2;
        private String action1;
        private String action2;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewerPairKappa {
        private Long reviewer1;
        private Long reviewer2;
        private double kappa;
        private int pairCount;
    }
}
