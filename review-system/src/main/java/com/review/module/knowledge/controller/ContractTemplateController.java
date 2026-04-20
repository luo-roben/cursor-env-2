package com.review.module.knowledge.controller;

import com.review.common.result.CommonResult;
import com.review.common.result.PageResult;
import com.review.module.knowledge.service.ContractTemplateService;
import com.review.module.knowledge.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "合同模板管理")
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class ContractTemplateController {

    private final ContractTemplateService contractTemplateService;

    @Operation(summary = "创建合同模板")
    @PostMapping
    public CommonResult<ContractTemplateRespVO> create(@Valid @RequestBody ContractTemplateCreateReqVO reqVO) {
        return CommonResult.success(contractTemplateService.create(reqVO));
    }

    @Operation(summary = "根据ID获取合同模板")
    @GetMapping("/{id}")
    public CommonResult<ContractTemplateRespVO> getById(@PathVariable Long id) {
        return CommonResult.success(contractTemplateService.getById(id));
    }

    @Operation(summary = "分页查询合同模板")
    @GetMapping
    public CommonResult<PageResult<ContractTemplateRespVO>> page(
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String contractType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(contractTemplateService.page(templateName, contractType, status, tenantId, pageNum, pageSize));
    }

    @Operation(summary = "添加模板条款")
    @PostMapping("/{id}/clauses")
    public CommonResult<ContractTemplateClauseRespVO> addClause(
            @PathVariable Long id,
            @Valid @RequestBody ContractTemplateClauseCreateReqVO reqVO) {
        return CommonResult.success(contractTemplateService.addClause(id, reqVO));
    }

    @Operation(summary = "获取模板条款列表")
    @GetMapping("/{id}/clauses")
    public CommonResult<List<ContractTemplateClauseRespVO>> listClauses(@PathVariable Long id) {
        return CommonResult.success(contractTemplateService.listClauses(id));
    }

    @Operation(summary = "发布合同模板")
    @PutMapping("/{id}/publish")
    public CommonResult<Void> publish(@PathVariable Long id) {
        contractTemplateService.publish(id);
        return CommonResult.success();
    }
}
