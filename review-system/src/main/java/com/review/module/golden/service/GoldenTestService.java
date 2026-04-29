package com.review.module.golden.service;

import com.review.common.exception.ErrorCode;
import com.review.common.exception.ServiceException;
import com.review.module.agent.dto.ReviewContext;
import com.review.module.golden.dto.GoldenTestReport;
import com.review.module.golden.dto.GoldenTestResult;
import com.review.module.golden.entity.GoldenTestCaseDO;
import com.review.module.golden.repository.GoldenTestCaseRepository;
import com.review.module.pipeline.ReviewPipeline;
import com.review.module.pipeline.dto.ReviewPipelineResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoldenTestService {

    private final GoldenTestCaseRepository goldenTestCaseRepository;
    private final ReviewPipeline reviewPipeline;

    public GoldenTestReport runAll() {
        List<GoldenTestCaseDO> enabledCases = goldenTestCaseRepository.findByEnabled(1);
        List<GoldenTestResult> results = new ArrayList<>();

        for (GoldenTestCaseDO testCase : enabledCases) {
            results.add(executeTestCase(testCase));
        }

        int passed = (int) results.stream().filter(GoldenTestResult::isPassed).count();
        return GoldenTestReport.builder()
                .totalTests(results.size())
                .passedTests(passed)
                .failedTests(results.size() - passed)
                .passRate(results.isEmpty() ? 0 : (double) passed / results.size() * 100)
                .executedAt(LocalDateTime.now())
                .results(results)
                .build();
    }

    public GoldenTestResult runOne(Long id) {
        GoldenTestCaseDO testCase = goldenTestCaseRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
        return executeTestCase(testCase);
    }

    private GoldenTestResult executeTestCase(GoldenTestCaseDO testCase) {
        List<String> mismatches = new ArrayList<>();
        boolean passed = true;

        try {
            ReviewContext context = ReviewContext.builder()
                    .taskId(0L)
                    .tenantId(1L)
                    .content(testCase.getInputContent())
                    .documentType(testCase.getDocumentType())
                    .contentType(testCase.getContentType())
                    .contractType(testCase.getContractType())
                    .build();

            ReviewPipelineResult result = reviewPipeline.execute(context);

            String actualVerdict = result.getOverallVerdict();
            int actualRiskScore = result.getRiskScore();

            if (!testCase.getExpectedVerdict().equalsIgnoreCase(actualVerdict)) {
                mismatches.add("Verdict mismatch: expected=" + testCase.getExpectedVerdict()
                        + ", actual=" + actualVerdict);
                passed = false;
            }

            if (testCase.getExpectedMinRiskScore() != null && actualRiskScore < testCase.getExpectedMinRiskScore()) {
                mismatches.add("Risk score too low: expected>=" + testCase.getExpectedMinRiskScore()
                        + ", actual=" + actualRiskScore);
                passed = false;
            }

            if (testCase.getExpectedMaxRiskScore() != null && actualRiskScore > testCase.getExpectedMaxRiskScore()) {
                mismatches.add("Risk score too high: expected<=" + testCase.getExpectedMaxRiskScore()
                        + ", actual=" + actualRiskScore);
                passed = false;
            }

            return GoldenTestResult.builder()
                    .testCaseId(testCase.getId())
                    .name(testCase.getName())
                    .passed(passed)
                    .expectedVerdict(testCase.getExpectedVerdict())
                    .actualVerdict(actualVerdict)
                    .expectedMinRiskScore(testCase.getExpectedMinRiskScore())
                    .expectedMaxRiskScore(testCase.getExpectedMaxRiskScore())
                    .actualRiskScore(actualRiskScore)
                    .mismatches(mismatches)
                    .build();
        } catch (Exception e) {
            log.error("Golden test case '{}' execution failed: {}", testCase.getName(), e.getMessage(), e);
            mismatches.add("Execution error: " + e.getMessage());
            return GoldenTestResult.builder()
                    .testCaseId(testCase.getId())
                    .name(testCase.getName())
                    .passed(false)
                    .expectedVerdict(testCase.getExpectedVerdict())
                    .actualVerdict("ERROR")
                    .mismatches(mismatches)
                    .build();
        }
    }
}
