package com.review.module.system.controller;

import com.review.common.exception.ErrorCode;
import com.review.common.exception.ServiceException;
import com.review.common.result.CommonResult;
import com.review.module.system.entity.TenantDO;
import com.review.module.system.repository.TenantRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "租户管理")
@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantRepository tenantRepository;

    @Operation(summary = "获取所有租户")
    @GetMapping
    public CommonResult<List<TenantDO>> list() {
        return CommonResult.success(tenantRepository.findAll());
    }

    @Operation(summary = "根据ID获取租户")
    @GetMapping("/{id}")
    public CommonResult<TenantDO> getById(@PathVariable Long id) {
        TenantDO tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND, "租户不存在"));
        return CommonResult.success(tenant);
    }
}
