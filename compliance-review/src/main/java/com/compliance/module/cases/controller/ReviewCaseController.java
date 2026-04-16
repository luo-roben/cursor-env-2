package com.compliance.module.cases.controller;

import com.compliance.common.result.CommonResult;
import com.compliance.common.result.PageResult;
import com.compliance.module.cases.service.ReviewCaseService;
import com.compliance.module.cases.vo.CaseCreateReqVO;
import com.compliance.module.cases.vo.CasePageReqVO;
import com.compliance.module.cases.vo.CaseRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "审核案例管理", description = "Review case management")
@RestController
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
public class ReviewCaseController {

    private final ReviewCaseService reviewCaseService;

    @Operation(summary = "手动导入案例", description = "Manually import a review case")
    @PostMapping
    public CommonResult<CaseRespVO> create(@Valid @RequestBody CaseCreateReqVO reqVO) {
        return CommonResult.success(reviewCaseService.create(reqVO));
    }

    @Operation(summary = "案例分页查询", description = "List review cases with pagination and filters")
    @GetMapping
    public CommonResult<PageResult<CaseRespVO>> page(@Valid CasePageReqVO reqVO) {
        return CommonResult.success(reviewCaseService.page(reqVO));
    }

    @Operation(summary = "获取案例详情", description = "Get a review case by ID")
    @GetMapping("/{id}")
    public CommonResult<CaseRespVO> getById(
            @Parameter(description = "案例ID") @PathVariable Long id,
            @Parameter(description = "租户ID") @RequestParam Long tenantId) {
        return CommonResult.success(reviewCaseService.getById(id, tenantId));
    }
}
