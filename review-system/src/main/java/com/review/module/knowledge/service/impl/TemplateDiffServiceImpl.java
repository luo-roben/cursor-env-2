package com.review.module.knowledge.service.impl;

import com.review.module.agent.dto.TemplateDiffResult;
import com.review.module.knowledge.entity.ContractTemplateClauseDO;
import com.review.module.knowledge.entity.ContractTemplateDO;
import com.review.module.knowledge.repository.ContractTemplateClauseRepository;
import com.review.module.knowledge.repository.ContractTemplateRepository;
import com.review.module.knowledge.service.TemplateDiffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateDiffServiceImpl implements TemplateDiffService {

    private static final double MATCH_THRESHOLD = 0.7;
    private static final double DEVIANT_THRESHOLD = 0.3;

    private final ContractTemplateRepository contractTemplateRepository;
    private final ContractTemplateClauseRepository contractTemplateClauseRepository;

    @Override
    public List<TemplateDiffResult> diff(String contractContent, String contractType, Long tenantId) {
        List<TemplateDiffResult> results = new ArrayList<>();

        if (contractContent == null || contractType == null) {
            return results;
        }

        // Find best matching template by contractType (published status)
        List<ContractTemplateDO> templates = contractTemplateRepository
                .findByContractTypeAndStatus(contractType, "published");
        if (templates.isEmpty()) {
            log.debug("No published template found for contractType={}", contractType);
            return results;
        }

        ContractTemplateDO template = templates.get(0);

        // Load template clauses
        List<ContractTemplateClauseDO> templateClauses = contractTemplateClauseRepository
                .findByTemplateIdOrderByClauseIdAsc(template.getTemplateId());

        // Track which parts of the contract are matched
        boolean[] contractMatched = new boolean[contractContent.length()];

        for (ContractTemplateClauseDO clause : templateClauses) {
            String templateText = clause.getClauseText();
            if (templateText == null || templateText.isEmpty()) continue;

            // Find best matching substring in contract content
            MatchResult match = findBestMatch(contractContent, templateText);

            if (match == null || match.similarity < DEVIANT_THRESHOLD) {
                results.add(TemplateDiffResult.builder()
                        .clauseNumber(clause.getClauseId())
                        .templateClauseText(templateText)
                        .actualClauseText(null)
                        .diffType("MISSING")
                        .severity(clause.getIsRequired() == 1 ? "HIGH" : "MEDIUM")
                        .build());
            } else if (match.similarity < MATCH_THRESHOLD) {
                results.add(TemplateDiffResult.builder()
                        .clauseNumber(clause.getClauseId())
                        .templateClauseText(templateText)
                        .actualClauseText(match.matchedText)
                        .diffType("DEVIANT")
                        .severity("MEDIUM")
                        .build());
                markMatched(contractMatched, match.startIndex, match.endIndex);
            } else {
                markMatched(contractMatched, match.startIndex, match.endIndex);
            }
        }

        return results;
    }

    private MatchResult findBestMatch(String content, String templateText) {
        if (content.isEmpty() || templateText.isEmpty()) return null;

        int windowSize = Math.min(templateText.length() * 2, content.length());
        int step = Math.max(1, templateText.length() / 4);
        double bestSimilarity = 0;
        int bestStart = -1;
        int bestEnd = -1;
        String bestText = null;

        for (int i = 0; i <= content.length() - Math.min(templateText.length() / 2, content.length()); i += step) {
            int end = Math.min(i + windowSize, content.length());
            String candidate = content.substring(i, end);
            double similarity = lcsRatio(templateText, candidate);
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestStart = i;
                bestEnd = end;
                bestText = candidate;
            }
        }

        if (bestSimilarity < DEVIANT_THRESHOLD / 2) return null;

        return new MatchResult(bestText, bestSimilarity, bestStart, bestEnd);
    }

    private double lcsRatio(String a, String b) {
        int m = a.length(), n = b.length();
        if (m == 0 || n == 0) return 0;
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++)
            for (int j = 1; j <= n; j++)
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? dp[i - 1][j - 1] + 1
                        : Math.max(dp[i - 1][j], dp[i][j - 1]);
        return (double) dp[m][n] / Math.max(m, n);
    }

    private void markMatched(boolean[] matched, int start, int end) {
        for (int i = start; i < end && i < matched.length; i++) {
            matched[i] = true;
        }
    }

    private record MatchResult(String matchedText, double similarity, int startIndex, int endIndex) {}
}
