package com.compliance.module.law.controller;

import com.compliance.common.result.CommonResult;
import com.compliance.common.result.PageResult;
import com.compliance.module.law.service.LawArticleService;
import com.compliance.module.law.vo.LawArticleCreateReqVO;
import com.compliance.module.law.vo.LawArticlePageReqVO;
import com.compliance.module.law.vo.LawArticleRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "法条知识管理", description = "Law article knowledge base management")
@RestController
@RequestMapping("/api/v1/law/articles")
@RequiredArgsConstructor
public class LawArticleController {

    private final LawArticleService lawArticleService;

    @Operation(summary = "创建/导入法条", description = "Create or import a law article into the knowledge base")
    @PostMapping
    public CommonResult<LawArticleRespVO> create(@Valid @RequestBody LawArticleCreateReqVO reqVO) {
        return CommonResult.success(lawArticleService.create(reqVO));
    }

    @Operation(summary = "获取法条详情", description = "Get a single law article by ID")
    @GetMapping("/{id}")
    public CommonResult<LawArticleRespVO> getById(
            @Parameter(description = "法条ID") @PathVariable Long id) {
        return CommonResult.success(lawArticleService.getById(id));
    }

    @Operation(summary = "法条分页查询", description = "List law articles with pagination and filters")
    @GetMapping
    public CommonResult<PageResult<LawArticleRespVO>> page(LawArticlePageReqVO reqVO) {
        return CommonResult.success(lawArticleService.page(reqVO));
    }

    @Operation(summary = "发布法条", description = "Publish (confirm) a law article to make it available for review")
    @PutMapping("/{id}/publish")
    public CommonResult<Void> publish(
            @Parameter(description = "法条ID") @PathVariable Long id,
            @Parameter(description = "确认人ID") @RequestParam(defaultValue = "1") Long confirmedBy) {
        lawArticleService.publish(id, confirmedBy);
        return CommonResult.success();
    }

    @Operation(summary = "更新法条", description = "Update an existing law article")
    @PutMapping("/{id}")
    public CommonResult<LawArticleRespVO> update(
            @Parameter(description = "法条ID") @PathVariable Long id,
            @Valid @RequestBody LawArticleCreateReqVO reqVO) {
        return CommonResult.success(lawArticleService.update(id, reqVO));
    }

    @Operation(summary = "废弃法条", description = "Deprecate a law article")
    @PutMapping("/{id}/deprecate")
    public CommonResult<Void> deprecate(
            @Parameter(description = "法条ID") @PathVariable Long id) {
        lawArticleService.deprecate(id);
        return CommonResult.success();
    }

    @Operation(summary = "删除法条", description = "Delete a draft law article")
    @DeleteMapping("/{id}")
    public CommonResult<Void> delete(
            @Parameter(description = "法条ID") @PathVariable Long id) {
        lawArticleService.delete(id);
        return CommonResult.success();
    }
}
