package com.review.module.cases.service.impl;

import com.review.module.cases.service.CaseSedimentationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseSedimentationServiceImpl implements CaseSedimentationService {

    private static final Map<String, Integer> LEARNING_VALUE_SCORES = Map.of(
            "SUPPLEMENTED", 3,
            "REJECTED", 2,
            "MODIFIED", 1,
            "CONFIRMED", 0
    );

    @Override
    public int calculateLearningValue(String humanAction) {
        if (humanAction == null) return 0;
        return LEARNING_VALUE_SCORES.getOrDefault(humanAction.toUpperCase(), 0);
    }

    @Override
    public void sediment(Long feedbackId) {
        log.info("Case sedimentation triggered for feedback: {}", feedbackId);
        // In full implementation:
        // 1. Load feedback from DB
        // 2. Calculate learning value
        // 3. Create or update review case
        // 4. Index in vector store for future retrieval
    }
}
