package com.review.module.review.repository;

import com.review.module.review.entity.ReviewResultDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewResultRepository extends JpaRepository<ReviewResultDO, Long> {

    List<ReviewResultDO> findByTaskIdOrderBySegmentIndexAsc(Long taskId);

    List<ReviewResultDO> findByTaskIdAndCardCategory(Long taskId, Integer cardCategory);
}
