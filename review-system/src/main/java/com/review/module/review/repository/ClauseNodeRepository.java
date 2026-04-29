package com.review.module.review.repository;

import com.review.module.review.entity.ClauseNodeDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClauseNodeRepository extends JpaRepository<ClauseNodeDO, Long> {

    List<ClauseNodeDO> findByTaskId(Long taskId);

    List<ClauseNodeDO> findByTaskIdAndClauseNumber(Long taskId, String clauseNumber);
}
