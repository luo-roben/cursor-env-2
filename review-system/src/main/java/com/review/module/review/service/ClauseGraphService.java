package com.review.module.review.service;

import com.review.module.parser.dto.ParsedContract;
import com.review.module.review.entity.ClauseEdgeDO;
import com.review.module.review.entity.ClauseNodeDO;

import java.util.List;

public interface ClauseGraphService {

    void buildGraph(Long taskId, ParsedContract parsedContract);

    List<ClauseNodeDO> findRelatedClauses(Long taskId, String clauseNumber);

    List<ClauseEdgeDO> findConflicts(Long taskId);
}
