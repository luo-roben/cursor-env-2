package com.compliance.module.law.controller;

import com.compliance.common.result.CommonResult;
import com.compliance.common.result.PageResult;
import com.compliance.module.law.service.LawSourceService;
import com.compliance.module.law.vo.LawSourceCreateReqVO;
import com.compliance.module.law.vo.LawSourcePageReqVO;
import com.compliance.module.law.vo.LawSourceRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "法规来源管理", description = "Law source management")
@RestController
@RequestMapping("/api/v1/law/sources")
@RequiredArgsConstructor
public class LawSourceController {

    private final LawSourceService lawSourceService;

    @Operation(summary = "创建法规来源", description = "Create a new law source")
    @PostMapping
    public CommonResult<LawSourceRespVO> create(@Valid @RequestBody LawSourceCreateReqVO reqVO) {
        return CommonResult.success(lawSourceService.create(reqVO));
    }

    @Operation(summary = "获取法规来源详情", description = "Get a single law source by ID")
    @GetMapping("/{id}")
    public CommonResult<LawSourceRespVO> getById(
            @Parameter(description = "法规来源ID") @PathVariable Long id) {
        return CommonResult.success(lawSourceService.getById(id));
    }

    @Operation(summary = "法规来源分页查询", description = "List law sources with pagination and filters")
    @GetMapping
    public CommonResult<PageResult<LawSourceRespVO>> page(LawSourcePageReqVO reqVO) {
        return CommonResult.success(lawSourceService.page(reqVO));
    }

    @Operation(summary = "更新解析状态", description = "Update the parse status of a law source")
    @PutMapping("/{id}/parse-status")
    public CommonResult<Void> updateParseStatus(
            @Parameter(description = "法规来源ID") @PathVariable Long id,
            @Parameter(description = "解析状态") @RequestParam String parseStatus) {
        lawSourceService.updateParseStatus(id, parseStatus);
        return CommonResult.success();
    }
}
