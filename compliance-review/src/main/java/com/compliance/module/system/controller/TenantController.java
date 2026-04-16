package com.compliance.module.system.controller;

import com.compliance.common.exception.ErrorCode;
import com.compliance.common.exception.ServiceException;
import com.compliance.common.result.CommonResult;
import com.compliance.module.system.entity.TenantDO;
import com.compliance.module.system.repository.TenantRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "租户管理", description = "Tenant management")
@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantRepository tenantRepository;

    @Operation(summary = "租户列表", description = "List all tenants")
    @GetMapping
    public CommonResult<List<TenantDO>> list() {
        return CommonResult.success(tenantRepository.findAll());
    }

    @Operation(summary = "获取租户详情", description = "Get tenant by ID")
    @GetMapping("/{id}")
    public CommonResult<TenantDO> getById(
            @Parameter(description = "租户ID") @PathVariable Long id) {
        TenantDO tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
        return CommonResult.success(tenant);
    }
}
