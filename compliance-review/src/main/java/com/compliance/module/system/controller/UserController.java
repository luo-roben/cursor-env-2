package com.compliance.module.system.controller;

import com.compliance.common.result.CommonResult;
import com.compliance.module.system.entity.SysUserDO;
import com.compliance.module.system.repository.SysUserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户管理", description = "User management")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserRepository sysUserRepository;

    @Operation(summary = "查询租户用户列表", description = "List users for a tenant")
    @GetMapping
    public CommonResult<List<SysUserDO>> list(
            @Parameter(description = "租户ID") @RequestParam Long tenantId) {
        return CommonResult.success(sysUserRepository.findByTenantId(tenantId));
    }
}
