package com.compliance.module.checklist.controller;

import com.compliance.common.result.CommonResult;
import com.compliance.module.checklist.service.ComplianceChecklistService;
import com.compliance.module.checklist.vo.ChecklistCreateReqVO;
import com.compliance.module.checklist.vo.ChecklistRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "合规自检清单", description = "Compliance checklist management")
@RestController
@RequestMapping("/api/v1/checklists")
@RequiredArgsConstructor
public class ComplianceChecklistController {

    private final ComplianceChecklistService checklistService;

    @Operation(summary = "创建检查项", description = "Create a new checklist item")
    @PostMapping
    public CommonResult<ChecklistRespVO> create(@Valid @RequestBody ChecklistCreateReqVO reqVO) {
        return CommonResult.success(checklistService.create(reqVO));
    }

    @Operation(summary = "查询检查项列表", description = "List checklist items by content type and optional product type")
    @GetMapping
    public CommonResult<List<ChecklistRespVO>> list(
            @Parameter(description = "内容类型") @RequestParam String contentType,
            @Parameter(description = "产品类型") @RequestParam(required = false) String productType) {
        return CommonResult.success(checklistService.listByContentType(contentType, productType));
    }

    @Operation(summary = "删除检查项", description = "Delete a checklist item by ID")
    @DeleteMapping("/{id}")
    public CommonResult<Void> delete(
            @Parameter(description = "检查项ID") @PathVariable Long id) {
        checklistService.delete(id);
        return CommonResult.success();
    }
}
