package com.review.module.golden.controller;

import com.review.common.result.CommonResult;
import com.review.module.golden.dto.GoldenTestReport;
import com.review.module.golden.dto.GoldenTestResult;
import com.review.module.golden.entity.GoldenTestCaseDO;
import com.review.module.golden.repository.GoldenTestCaseRepository;
import com.review.module.golden.service.GoldenTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "金标准测试")
@RestController
@RequestMapping("/api/v1/golden")
@RequiredArgsConstructor
public class GoldenTestController {

    private final GoldenTestService goldenTestService;
    private final GoldenTestCaseRepository goldenTestCaseRepository;

    @Operation(summary = "运行所有测试")
    @PostMapping("/run")
    public CommonResult<GoldenTestReport> runAll() {
        return CommonResult.success(goldenTestService.runAll());
    }

    @Operation(summary = "运行单个测试")
    @PostMapping("/run/{id}")
    public CommonResult<GoldenTestResult> runOne(@PathVariable Long id) {
        return CommonResult.success(goldenTestService.runOne(id));
    }

    @Operation(summary = "获取所有测试用例")
    @GetMapping
    public CommonResult<List<GoldenTestCaseDO>> list() {
        return CommonResult.success(goldenTestCaseRepository.findAll());
    }

    @Operation(summary = "创建测试用例")
    @PostMapping
    public CommonResult<GoldenTestCaseDO> create(@RequestBody GoldenTestCaseDO testCase) {
        return CommonResult.success(goldenTestCaseRepository.save(testCase));
    }
}
