package com.review.module.feedback.service;

import com.review.module.feedback.dto.ConsistencyReport;
import com.review.module.feedback.entity.HumanFeedbackDO;
import com.review.module.feedback.repository.HumanFeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewerConsistencyService {

    private final HumanFeedbackRepository humanFeedbackRepository;

    public ConsistencyReport calculateConsistency(Long tenantId) {
        List<HumanFeedbackDO> feedbacks = humanFeedbackRepository.findByTenantId(tenantId);

        // Group by taskId
        Map<Long, List<HumanFeedbackDO>> byTask = feedbacks.stream()
                .filter(f -> f.getTaskId() != null)
                .collect(Collectors.groupingBy(HumanFeedbackDO::getTaskId));

        int totalPairs = 0;
        int agreements = 0;
        List<ConsistencyReport.Disagreement> disagreements = new ArrayList<>();

        for (Map.Entry<Long, List<HumanFeedbackDO>> entry : byTask.entrySet()) {
            List<HumanFeedbackDO> taskFeedbacks = entry.getValue();
            if (taskFeedbacks.size() < 2) continue;

            // Deduplicate by reviewer
            Map<Long, HumanFeedbackDO> byReviewer = new LinkedHashMap<>();
            for (HumanFeedbackDO f : taskFeedbacks) {
                byReviewer.putIfAbsent(f.getReviewerId(), f);
            }
            List<HumanFeedbackDO> unique = new ArrayList<>(byReviewer.values());

            // Compare pairs
            for (int i = 0; i < unique.size(); i++) {
                for (int j = i + 1; j < unique.size(); j++) {
                    totalPairs++;
                    HumanFeedbackDO f1 = unique.get(i);
                    HumanFeedbackDO f2 = unique.get(j);

                    boolean agree = isAgreement(f1.getAction(), f2.getAction());
                    if (agree) {
                        agreements++;
                    } else {
                        disagreements.add(ConsistencyReport.Disagreement.builder()
                                .taskId(entry.getKey())
                                .reviewer1(f1.getReviewerId())
                                .reviewer2(f2.getReviewerId())
                                .action1(f1.getAction())
                                .action2(f2.getAction())
                                .build());
                    }
                }
            }
        }

        double agreementRate = totalPairs > 0 ? (double) agreements / totalPairs : 1.0;

        return ConsistencyReport.builder()
                .totalPairs(totalPairs)
                .agreementRate(agreementRate)
                .disagreements(disagreements)
                .build();
    }

    private boolean isAgreement(String action1, String action2) {
        if (action1 == null || action2 == null) return false;
        // CONFIRMED vs REJECTED is a clear disagreement
        // Same action type = agreement
        String norm1 = normalizeAction(action1);
        String norm2 = normalizeAction(action2);
        return norm1.equals(norm2);
    }

    private String normalizeAction(String action) {
        if (action == null) return "";
        return switch (action.toUpperCase()) {
            case "CONFIRMED" -> "CONFIRMED";
            case "REJECTED" -> "REJECTED";
            case "MODIFIED", "SUPPLEMENTED" -> "MODIFIED";
            default -> action.toUpperCase();
        };
    }
}
