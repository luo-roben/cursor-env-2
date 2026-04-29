package com.review.module.industry.controller;

import com.review.common.result.CommonResult;
import com.review.module.industry.entity.IndustryKnowledgeDO;
import com.review.module.industry.service.IndustryKnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "行业知识库")
@RestController
@RequestMapping("/api/v1/industry-knowledge")
@RequiredArgsConstructor
public class IndustryKnowledgeController {

    private final IndustryKnowledgeService industryKnowledgeService;

    @Operation(summary = "创建行业知识")
    @PostMapping
    public CommonResult<IndustryKnowledgeDO> create(@RequestBody IndustryKnowledgeDO entity) {
        return CommonResult.success(industryKnowledgeService.create(entity));
    }

    @Operation(summary = "获取行业知识详情")
    @GetMapping("/{id}")
    public CommonResult<IndustryKnowledgeDO> getById(@PathVariable Long id) {
        return CommonResult.success(industryKnowledgeService.getById(id));
    }

    @Operation(summary = "获取所有行业知识")
    @GetMapping
    public CommonResult<List<IndustryKnowledgeDO>> list(@RequestParam(required = false) String industry) {
        if (industry != null && !industry.isEmpty()) {
            return CommonResult.success(industryKnowledgeService.listByIndustry(industry));
        }
        return CommonResult.success(industryKnowledgeService.listAll());
    }

    @Operation(summary = "更新行业知识")
    @PutMapping("/{id}")
    public CommonResult<IndustryKnowledgeDO> update(@PathVariable Long id, @RequestBody IndustryKnowledgeDO entity) {
        return CommonResult.success(industryKnowledgeService.update(id, entity));
    }

    @Operation(summary = "删除行业知识")
    @DeleteMapping("/{id}")
    public CommonResult<Void> delete(@PathVariable Long id) {
        industryKnowledgeService.delete(id);
        return CommonResult.success();
    }
}
