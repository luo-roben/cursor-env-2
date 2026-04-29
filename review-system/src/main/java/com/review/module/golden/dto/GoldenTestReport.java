package com.review.module.golden.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoldenTestReport {

    private int totalTests;
    private int passedTests;
    private int failedTests;
    private double passRate;
    private LocalDateTime executedAt;
    private List<GoldenTestResult> results;
}
