package com.review.module.system.controller;

import com.review.common.result.CommonResult;
import com.review.module.system.entity.SysUserDO;
import com.review.module.system.repository.SysUserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserRepository sysUserRepository;

    @Operation(summary = "根据租户ID获取用户列表")
    @GetMapping
    public CommonResult<List<SysUserDO>> listByTenantId(@RequestParam Long tenantId) {
        return CommonResult.success(sysUserRepository.findByTenantId(tenantId));
    }
}
