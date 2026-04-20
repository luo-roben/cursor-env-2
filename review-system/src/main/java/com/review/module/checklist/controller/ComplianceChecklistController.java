package com.review.module.checklist.controller;

import com.review.common.result.CommonResult;
import com.review.module.checklist.entity.ComplianceChecklistDO;
import com.review.module.checklist.service.ComplianceChecklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "合规自检清单", description = "合规自检清单管理接口")
@RestController
@RequestMapping("/api/v1/checklists")
@RequiredArgsConstructor
public class ComplianceChecklistController {

    private final ComplianceChecklistService complianceChecklistService;

    @Operation(summary = "创建清单项")
    @PostMapping
    public CommonResult<ComplianceChecklistDO> create(@RequestBody ComplianceChecklistDO checklist) {
        return CommonResult.success(complianceChecklistService.create(checklist));
    }

    @Operation(summary = "更新清单项")
    @PutMapping("/{id}")
    public CommonResult<ComplianceChecklistDO> update(@PathVariable Long id, @RequestBody ComplianceChecklistDO checklist) {
        return CommonResult.success(complianceChecklistService.update(id, checklist));
    }

    @Operation(summary = "删除清单项")
    @DeleteMapping("/{id}")
    public CommonResult<Void> delete(@PathVariable Long id) {
        complianceChecklistService.delete(id);
        return CommonResult.success();
    }

    @Operation(summary = "查询清单项详情")
    @GetMapping("/{id}")
    public CommonResult<ComplianceChecklistDO> getById(@PathVariable Long id) {
        return CommonResult.success(complianceChecklistService.getById(id));
    }

    @Operation(summary = "按内容类型查询")
    @GetMapping("/content-type/{contentType}")
    public CommonResult<List<ComplianceChecklistDO>> listByContentType(@PathVariable String contentType) {
        return CommonResult.success(complianceChecklistService.listByContentType(contentType));
    }

    @Operation(summary = "查询全部启用清单")
    @GetMapping
    public CommonResult<List<ComplianceChecklistDO>> listAll() {
        return CommonResult.success(complianceChecklistService.listAll());
    }
}
