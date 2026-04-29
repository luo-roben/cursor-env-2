package com.review.module.review.service;

import com.review.module.review.entity.ReviewResultDO;
import com.review.module.review.repository.ReviewResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RevisionLinkageService {

    private final ReviewResultRepository reviewResultRepository;

    @Transactional
    public void invalidateByClause(Long taskId, String clauseId) {
        List<ReviewResultDO> results = reviewResultRepository.findByTaskIdAndClauseId(taskId, clauseId);
        for (ReviewResultDO result : results) {
            result.setRevisionStatus("invalidated");
            reviewResultRepository.save(result);
        }
        log.info("Invalidated {} review results for taskId={}, clauseId={}", results.size(), taskId, clauseId);
    }

    @Transactional
    public void markStale(Long taskId) {
        List<ReviewResultDO> results = reviewResultRepository.findByTaskId(taskId);
        for (ReviewResultDO result : results) {
            result.setRevisionStatus("stale");
            reviewResultRepository.save(result);
        }
        log.info("Marked {} review results as stale for taskId={}", results.size(), taskId);
    }
}
