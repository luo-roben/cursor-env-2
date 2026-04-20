package com.review.module.review.repository;

import com.review.module.review.entity.ReviewMissingElementDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewMissingElementRepository extends JpaRepository<ReviewMissingElementDO, Long> {

    List<ReviewMissingElementDO> findByTaskId(Long taskId);
}
