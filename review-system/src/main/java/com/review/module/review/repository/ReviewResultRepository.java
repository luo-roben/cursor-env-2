package com.review.module.review.repository;

import com.review.module.review.entity.ReviewResultDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewResultRepository extends JpaRepository<ReviewResultDO, Long> {

    List<ReviewResultDO> findByTaskIdOrderBySegmentIndexAsc(Long taskId);

    List<ReviewResultDO> findByTaskIdAndCardCategory(Long taskId, Integer cardCategory);

    @Query("SELECT rr.cardCategory, COUNT(rr) FROM ReviewResultDO rr WHERE rr.tenantId = :tenantId GROUP BY rr.cardCategory")
    List<Object[]> countByCardCategory(@Param("tenantId") Long tenantId);

    @Query("SELECT rr.cardCategory, COUNT(rr) FROM ReviewResultDO rr GROUP BY rr.cardCategory")
    List<Object[]> countByCardCategoryAll();

    @Query("SELECT rr.severity, COUNT(rr) FROM ReviewResultDO rr WHERE rr.tenantId = :tenantId GROUP BY rr.severity")
    List<Object[]> countBySeverity(@Param("tenantId") Long tenantId);

    @Query("SELECT rr.severity, COUNT(rr) FROM ReviewResultDO rr GROUP BY rr.severity")
    List<Object[]> countBySeverityAll();
}
