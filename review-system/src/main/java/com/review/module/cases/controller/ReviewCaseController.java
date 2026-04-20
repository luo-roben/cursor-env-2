package com.review.module.cases.controller;

import com.review.common.result.CommonResult;
import com.review.common.result.PageResult;
import com.review.module.cases.service.ReviewCaseService;
import com.review.module.cases.vo.CaseCreateReqVO;
import com.review.module.cases.vo.CasePageReqVO;
import com.review.module.cases.vo.CaseRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "审核案例", description = "审核案例管理接口")
@RestController
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
public class ReviewCaseController {

    private final ReviewCaseService reviewCaseService;

    @Operation(summary = "创建案例")
    @PostMapping
    public CommonResult<CaseRespVO> create(@Valid @RequestBody CaseCreateReqVO reqVO) {
        return CommonResult.success(reviewCaseService.create(reqVO));
    }

    @Operation(summary = "查询案例详情")
    @GetMapping("/{id}")
    public CommonResult<CaseRespVO> getById(@PathVariable Long id) {
        return CommonResult.success(reviewCaseService.getById(id));
    }

    @Operation(summary = "分页查询案例")
    @GetMapping
    public CommonResult<PageResult<CaseRespVO>> page(CasePageReqVO reqVO) {
        return CommonResult.success(reviewCaseService.page(reqVO));
    }

    @Operation(summary = "查询典型案例")
    @GetMapping("/typical")
    public CommonResult<List<CaseRespVO>> listTypical() {
        return CommonResult.success(reviewCaseService.listTypicalCases());
    }
}
