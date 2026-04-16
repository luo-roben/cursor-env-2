package com.compliance.module.system.repository;

import com.compliance.module.system.entity.SysUserDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SysUserRepository extends JpaRepository<SysUserDO, Long> {

    Optional<SysUserDO> findByTenantIdAndUsername(Long tenantId, String username);

    List<SysUserDO> findByTenantId(Long tenantId);
}
