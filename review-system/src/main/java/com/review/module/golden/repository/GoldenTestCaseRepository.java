package com.review.module.golden.repository;

import com.review.module.golden.entity.GoldenTestCaseDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoldenTestCaseRepository extends JpaRepository<GoldenTestCaseDO, Long> {

    List<GoldenTestCaseDO> findByEnabled(Integer enabled);
}
