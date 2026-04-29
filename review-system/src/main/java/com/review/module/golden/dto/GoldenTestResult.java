package com.review.module.golden.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoldenTestResult {

    private Long testCaseId;
    private String name;
    private boolean passed;
    private String expectedVerdict;
    private String actualVerdict;
    private Integer expectedMinRiskScore;
    private Integer expectedMaxRiskScore;
    private Integer actualRiskScore;
    private List<String> mismatches;
}
