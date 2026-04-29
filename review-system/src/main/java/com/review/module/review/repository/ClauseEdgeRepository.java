package com.review.module.review.repository;

import com.review.module.review.entity.ClauseEdgeDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClauseEdgeRepository extends JpaRepository<ClauseEdgeDO, Long> {

    List<ClauseEdgeDO> findByTaskId(Long taskId);

    List<ClauseEdgeDO> findByTaskIdAndSourceClauseOrTaskIdAndTargetClause(
            Long taskId1, String sourceClause, Long taskId2, String targetClause);

    List<ClauseEdgeDO> findByTaskIdAndRelationType(Long taskId, String relationType);
}
