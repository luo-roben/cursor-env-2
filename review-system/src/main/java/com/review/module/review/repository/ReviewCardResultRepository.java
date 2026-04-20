package com.review.module.review.repository;

import com.review.module.review.entity.ReviewCardResultDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewCardResultRepository extends JpaRepository<ReviewCardResultDO, Long> {

    List<ReviewCardResultDO> findByTaskId(Long taskId);
}
