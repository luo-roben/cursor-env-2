package com.review.module.filter.impl;

import com.review.module.agent.dto.CustomRuleInfo;
import com.review.module.filter.QuickFilterService;
import com.review.module.filter.dto.QuickFilterResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class QuickFilterServiceImpl implements QuickFilterService {

    @Override
    public QuickFilterResult filter(String content, List<CustomRuleInfo> customRules) {
        List<QuickFilterResult.BannedWordHit> bannedWordHits = new ArrayList<>();
        List<String> missingStatements = new ArrayList<>();

        if (content == null || content.isEmpty()) {
            return QuickFilterResult.builder()
                    .bannedWordHits(bannedWordHits)
                    .missingStatements(missingStatements)
                    .passed(true)
                    .build();
        }

        if (customRules != null) {
            List<String> bannedWords = customRules.stream()
                    .filter(r -> "BANNED_WORD".equalsIgnoreCase(r.getRuleType()))
                    .map(CustomRuleInfo::getContent)
                    .filter(Objects::nonNull)
                    .toList();

            if (!bannedWords.isEmpty()) {
                bannedWordHits.addAll(ahoCorasickMatch(content, bannedWords, customRules));
            }

            List<String> requiredStatements = customRules.stream()
                    .filter(r -> "REQUIRED_STATEMENT".equalsIgnoreCase(r.getRuleType()))
                    .map(CustomRuleInfo::getContent)
                    .filter(Objects::nonNull)
                    .toList();

            for (String statement : requiredStatements) {
                if (!content.contains(statement)) {
                    missingStatements.add(statement);
                }
            }
        }

        boolean passed = bannedWordHits.isEmpty() && missingStatements.isEmpty();

        return QuickFilterResult.builder()
                .bannedWordHits(bannedWordHits)
                .missingStatements(missingStatements)
                .passed(passed)
                .build();
    }

    private List<QuickFilterResult.BannedWordHit> ahoCorasickMatch(
            String content, List<String> bannedWords, List<CustomRuleInfo> rules) {
        List<QuickFilterResult.BannedWordHit> hits = new ArrayList<>();

        for (String word : bannedWords) {
            int idx = 0;
            while ((idx = content.indexOf(word, idx)) >= 0) {
                String severity = findSeverity(word, rules);
                hits.add(QuickFilterResult.BannedWordHit.builder()
                        .word(word)
                        .offset(idx)
                        .severity(severity)
                        .build());
                idx += word.length();
            }
        }

        return hits;
    }

    private String findSeverity(String word, List<CustomRuleInfo> rules) {
        if (rules == null) return "MAJOR";
        return rules.stream()
                .filter(r -> "BANNED_WORD".equalsIgnoreCase(r.getRuleType()))
                .filter(r -> word.equals(r.getContent()))
                .map(CustomRuleInfo::getSeverity)
                .findFirst()
                .orElse("MAJOR");
    }
}
