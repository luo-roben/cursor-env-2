package com.review.module.system.repository;

import com.review.module.system.entity.TenantDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<TenantDO, Long> {

    Optional<TenantDO> findByCode(String code);
}
