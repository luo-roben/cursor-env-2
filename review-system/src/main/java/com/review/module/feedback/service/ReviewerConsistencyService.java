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

    private static final List<String> CATEGORIES = List.of("CONFIRMED", "REJECTED", "MODIFIED", "SUPPLEMENTED");

    private final HumanFeedbackRepository humanFeedbackRepository;

    public ConsistencyReport calculateConsistency(Long tenantId) {
        List<HumanFeedbackDO> feedbacks = humanFeedbackRepository.findByTenantId(tenantId);

        Map<Long, List<HumanFeedbackDO>> byTask = feedbacks.stream()
                .filter(f -> f.getTaskId() != null)
                .collect(Collectors.groupingBy(HumanFeedbackDO::getTaskId));

        int totalPairs = 0;
        int agreements = 0;
        List<ConsistencyReport.Disagreement> disagreements = new ArrayList<>();

        Map<String, List<String[]>> pairActions = new LinkedHashMap<>();

        for (Map.Entry<Long, List<HumanFeedbackDO>> entry : byTask.entrySet()) {
            List<HumanFeedbackDO> taskFeedbacks = entry.getValue();
            if (taskFeedbacks.size() < 2) continue;

            Map<Long, HumanFeedbackDO> byReviewer = new LinkedHashMap<>();
            for (HumanFeedbackDO f : taskFeedbacks) {
                byReviewer.putIfAbsent(f.getReviewerId(), f);
            }
            List<HumanFeedbackDO> unique = new ArrayList<>(byReviewer.values());

            for (int i = 0; i < unique.size(); i++) {
                for (int j = i + 1; j < unique.size(); j++) {
                    totalPairs++;
                    HumanFeedbackDO f1 = unique.get(i);
                    HumanFeedbackDO f2 = unique.get(j);

                    String a1 = normalizeAction(f1.getAction());
                    String a2 = normalizeAction(f2.getAction());

                    if (a1.equals(a2)) {
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

                    Long r1 = Math.min(f1.getReviewerId(), f2.getReviewerId());
                    Long r2 = Math.max(f1.getReviewerId(), f2.getReviewerId());
                    String pairKey = r1 + ":" + r2;
                    String[] actionPair = f1.getReviewerId().equals(r1)
                            ? new String[]{a1, a2}
                            : new String[]{a2, a1};
                    pairActions.computeIfAbsent(pairKey, k -> new ArrayList<>()).add(actionPair);
                }
            }
        }

        double agreementRate = totalPairs > 0 ? (double) agreements / totalPairs : 1.0;
        double overallKappa = computeOverallKappa(pairActions);

        List<ConsistencyReport.ReviewerPairKappa> perPairKappa = new ArrayList<>();
        for (Map.Entry<String, List<String[]>> entry : pairActions.entrySet()) {
            String[] ids = entry.getKey().split(":");
            double pairKappa = computeKappa(entry.getValue());
            perPairKappa.add(ConsistencyReport.ReviewerPairKappa.builder()
                    .reviewer1(Long.parseLong(ids[0]))
                    .reviewer2(Long.parseLong(ids[1]))
                    .kappa(pairKappa)
                    .pairCount(entry.getValue().size())
                    .build());
        }

        return ConsistencyReport.builder()
                .totalPairs(totalPairs)
                .agreementRate(agreementRate)
                .kappaScore(overallKappa)
                .perPairKappa(perPairKappa)
                .disagreements(disagreements)
                .build();
    }

    private double computeOverallKappa(Map<String, List<String[]>> pairActions) {
        List<String[]> allPairs = pairActions.values().stream()
                .flatMap(List::stream)
                .toList();
        return computeKappa(allPairs);
    }

    private double computeKappa(List<String[]> actionPairs) {
        if (actionPairs.isEmpty()) return 1.0;

        int n = actionPairs.size();
        int observed = 0;
        Map<String, Integer> r1Counts = new HashMap<>();
        Map<String, Integer> r2Counts = new HashMap<>();

        for (String[] pair : actionPairs) {
            if (pair[0].equals(pair[1])) {
                observed++;
            }
            r1Counts.merge(pair[0], 1, Integer::sum);
            r2Counts.merge(pair[1], 1, Integer::sum);
        }

        double po = (double) observed / n;

        double pe = 0.0;
        for (String category : CATEGORIES) {
            double p1 = (double) r1Counts.getOrDefault(category, 0) / n;
            double p2 = (double) r2Counts.getOrDefault(category, 0) / n;
            pe += p1 * p2;
        }

        if (Math.abs(1.0 - pe) < 1e-10) {
            return po >= 1.0 ? 1.0 : 0.0;
        }

        return (po - pe) / (1.0 - pe);
    }

    private String normalizeAction(String action) {
        if (action == null) return "CONFIRMED";
        return action.toUpperCase();
    }
}
