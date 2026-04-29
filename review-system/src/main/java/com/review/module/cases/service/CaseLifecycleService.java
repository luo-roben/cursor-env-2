package com.review.module.cases.service;

import com.review.module.cases.entity.ReviewCaseDO;
import com.review.module.cases.repository.ReviewCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseLifecycleService {

    private final ReviewCaseRepository reviewCaseRepository;

    @Scheduled(cron = "0 0 2 ? * MON")
    @Transactional
    public void decayCases() {
        log.info("Running weekly case decay task");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twelveMonthsAgo = now.minusMonths(12);
        LocalDateTime twentyFourMonthsAgo = now.minusMonths(24);

        List<ReviewCaseDO> oldCases = reviewCaseRepository.findActiveOlderThan(twelveMonthsAgo);
        int decayed = 0;
        for (ReviewCaseDO c : oldCases) {
            int decay = c.getCreatedAt().isBefore(twentyFourMonthsAgo) ? 2 : 1;
            int currentScore = c.getLearningValueScore() != null ? c.getLearningValueScore() : 0;
            c.setLearningValueScore(Math.max(0, currentScore - decay));
            decayed++;
        }
        if (!oldCases.isEmpty()) {
            reviewCaseRepository.saveAll(oldCases);
        }
        log.info("Decayed {} cases", decayed);
    }

    @Scheduled(cron = "0 0 3 1 * ?")
    @Transactional
    public void archiveCases() {
        log.info("Running monthly case archive task");
        LocalDateTime thirtySixMonthsAgo = LocalDateTime.now().minusMonths(36);
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);

        List<ReviewCaseDO> archiveCandidates = reviewCaseRepository
                .findActiveOlderThanWithNoRecentHits(thirtySixMonthsAgo, sixMonthsAgo);

        int archived = 0;
        for (ReviewCaseDO c : archiveCandidates) {
            c.setStatus("archived");
            archived++;
        }
        if (!archiveCandidates.isEmpty()) {
            reviewCaseRepository.saveAll(archiveCandidates);
        }
        log.info("Archived {} cases", archived);
    }

    @Scheduled(cron = "0 0 4 ? * MON")
    @Transactional
    public void deduplicateCases() {
        log.info("Running weekly case deduplication task");
        List<ReviewCaseDO> activeCases = reviewCaseRepository.findAll().stream()
                .filter(c -> "active".equals(c.getStatus()))
                .collect(Collectors.toList());

        // Group by tenant
        Map<Long, List<ReviewCaseDO>> byTenant = activeCases.stream()
                .filter(c -> c.getTenantId() != null)
                .collect(Collectors.groupingBy(ReviewCaseDO::getTenantId));

        int superseded = 0;
        for (Map.Entry<Long, List<ReviewCaseDO>> entry : byTenant.entrySet()) {
            List<ReviewCaseDO> tenantCases = entry.getValue();
            Set<Long> marked = new HashSet<>();

            for (int i = 0; i < tenantCases.size(); i++) {
                if (marked.contains(tenantCases.get(i).getId())) continue;
                for (int j = i + 1; j < tenantCases.size(); j++) {
                    if (marked.contains(tenantCases.get(j).getId())) continue;

                    String text1 = tenantCases.get(i).getReviewedContent();
                    String text2 = tenantCases.get(j).getReviewedContent();
                    if (text1 == null || text2 == null) continue;

                    double similarity = substringOverlapRatio(text1, text2);
                    if (similarity > 0.95) {
                        // Mark lower-value duplicate as superseded
                        int score1 = tenantCases.get(i).getLearningValueScore() != null ? tenantCases.get(i).getLearningValueScore() : 0;
                        int score2 = tenantCases.get(j).getLearningValueScore() != null ? tenantCases.get(j).getLearningValueScore() : 0;
                        ReviewCaseDO toSupersede = score1 <= score2 ? tenantCases.get(i) : tenantCases.get(j);
                        toSupersede.setStatus("superseded");
                        marked.add(toSupersede.getId());
                        superseded++;
                    }
                }
            }
        }
        if (superseded > 0) {
            reviewCaseRepository.saveAll(activeCases);
        }
        log.info("Superseded {} duplicate cases", superseded);
    }

    static double substringOverlapRatio(String a, String b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return 0.0;

        int matchCount = 0;
        String shorter = a.length() <= b.length() ? a : b;
        String longer = a.length() > b.length() ? a : b;

        for (int i = 0; i < shorter.length(); i++) {
            if (longer.indexOf(shorter.charAt(i), Math.max(0, i - 10)) >= 0) {
                matchCount++;
            }
        }
        return (double) matchCount / Math.max(a.length(), b.length());
    }
}
