package com.review.module.rules.controller;

import com.review.common.result.CommonResult;
import com.review.module.rules.dto.RuleConflict;
import com.review.module.rules.entity.TenantCustomRuleDO;
import com.review.module.rules.service.RuleConflictDetector;
import com.review.module.rules.service.TenantCustomRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "租户自定义规则", description = "租户自定义规则管理接口")
@RestController
@RequestMapping("/api/v1/tenant/rules")
@RequiredArgsConstructor
public class TenantCustomRuleController {

    private final TenantCustomRuleService tenantCustomRuleService;
    private final RuleConflictDetector ruleConflictDetector;

    @Operation(summary = "创建规则")
    @PostMapping
    public CommonResult<Map<String, Object>> create(@RequestBody TenantCustomRuleDO rule) {
        TenantCustomRuleDO saved = tenantCustomRuleService.create(rule);
        List<RuleConflict> conflicts = ruleConflictDetector.detectConflicts(saved.getTenantId());
        Map<String, Object> result = new HashMap<>();
        result.put("rule", saved);
        if (!conflicts.isEmpty()) {
            result.put("warnings", conflicts);
        }
        return CommonResult.success(result);
    }

    @Operation(summary = "更新规则")
    @PutMapping("/{id}")
    public CommonResult<Map<String, Object>> update(@PathVariable Long id, @RequestBody TenantCustomRuleDO rule) {
        TenantCustomRuleDO updated = tenantCustomRuleService.update(id, rule);
        List<RuleConflict> conflicts = ruleConflictDetector.detectConflicts(updated.getTenantId());
        Map<String, Object> result = new HashMap<>();
        result.put("rule", updated);
        if (!conflicts.isEmpty()) {
            result.put("warnings", conflicts);
        }
        return CommonResult.success(result);
    }

    @Operation(summary = "删除规则")
    @DeleteMapping("/{id}")
    public CommonResult<Void> delete(@PathVariable Long id) {
        tenantCustomRuleService.delete(id);
        return CommonResult.success();
    }

    @Operation(summary = "查询规则详情")
    @GetMapping("/{id}")
    public CommonResult<TenantCustomRuleDO> getById(@PathVariable Long id) {
        return CommonResult.success(tenantCustomRuleService.getById(id));
    }

    @Operation(summary = "查询租户规则列表")
    @GetMapping("/tenant/{tenantId}")
    public CommonResult<List<TenantCustomRuleDO>> listByTenantId(@PathVariable Long tenantId) {
        return CommonResult.success(tenantCustomRuleService.listByTenantId(tenantId));
    }

    @Operation(summary = "查询租户启用规则")
    @GetMapping("/tenant/{tenantId}/enabled")
    public CommonResult<List<TenantCustomRuleDO>> listEnabledByTenantId(@PathVariable Long tenantId) {
        return CommonResult.success(tenantCustomRuleService.listEnabledByTenantId(tenantId));
    }

    @Operation(summary = "检测规则冲突")
    @GetMapping("/conflicts")
    public CommonResult<List<RuleConflict>> detectConflicts(@RequestParam Long tenantId) {
        return CommonResult.success(ruleConflictDetector.detectConflicts(tenantId));
    }
}
