package com.compliance.module.filter.service.impl;

import com.compliance.module.filter.dto.QuickFilterResult;
import com.compliance.module.filter.service.QuickFilterService;
import com.compliance.module.tenant.entity.TenantCustomRuleDO;
import com.compliance.module.tenant.repository.TenantCustomRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuickFilterServiceImpl implements QuickFilterService {

    private final TenantCustomRuleRepository tenantCustomRuleRepository;

    @Override
    public QuickFilterResult filter(String content, Long tenantId) {
        List<TenantCustomRuleDO> allRules = tenantCustomRuleRepository
                .findByTenantIdAndEnabledTrueOrderByPriorityDesc(tenantId);

        List<TenantCustomRuleDO> bannedWordRules = allRules.stream()
                .filter(r -> "banned_word".equals(r.getRuleType()))
                .toList();

        List<TenantCustomRuleDO> requiredStatementRules = allRules.stream()
                .filter(r -> "required_statement".equals(r.getRuleType()))
                .toList();

        List<QuickFilterResult.BannedWordHit> bannedWordHits = searchBannedWords(content, bannedWordRules);
        List<QuickFilterResult.MissingStatement> missingStatements = checkRequiredStatements(content, requiredStatementRules);

        boolean passed = bannedWordHits.isEmpty() && missingStatements.isEmpty();

        log.info("QuickFilter result: tenantId={}, bannedWordHits={}, missingStatements={}, passed={}",
                tenantId, bannedWordHits.size(), missingStatements.size(), passed);

        return QuickFilterResult.builder()
                .bannedWordHits(bannedWordHits)
                .missingStatements(missingStatements)
                .passed(passed)
                .build();
    }

    private List<QuickFilterResult.BannedWordHit> searchBannedWords(String content,
                                                                     List<TenantCustomRuleDO> bannedWordRules) {
        if (bannedWordRules.isEmpty() || content == null || content.isEmpty()) {
            return Collections.emptyList();
        }

        AhoCorasickAutomaton automaton = new AhoCorasickAutomaton();
        Map<String, Long> wordToRuleId = new HashMap<>();
        for (TenantCustomRuleDO rule : bannedWordRules) {
            String word = rule.getContent().trim();
            if (!word.isEmpty()) {
                automaton.addPattern(word);
                wordToRuleId.put(word, rule.getId());
            }
        }
        automaton.build();

        List<QuickFilterResult.BannedWordHit> hits = new ArrayList<>();
        automaton.search(content, (word, position) ->
                hits.add(QuickFilterResult.BannedWordHit.builder()
                        .word(word)
                        .position(position)
                        .ruleId(wordToRuleId.get(word))
                        .build()));

        return hits;
    }

    private List<QuickFilterResult.MissingStatement> checkRequiredStatements(String content,
                                                                              List<TenantCustomRuleDO> requiredStatementRules) {
        if (requiredStatementRules.isEmpty()) {
            return Collections.emptyList();
        }

        List<QuickFilterResult.MissingStatement> missing = new ArrayList<>();
        for (TenantCustomRuleDO rule : requiredStatementRules) {
            String statement = rule.getContent().trim();
            if (!statement.isEmpty() && !content.contains(statement)) {
                missing.add(QuickFilterResult.MissingStatement.builder()
                        .statement(statement)
                        .ruleId(rule.getId())
                        .severity(rule.getSeverityIfTriggered())
                        .build());
            }
        }
        return missing;
    }

    /**
     * Aho-Corasick automaton for multi-pattern string matching.
     */
    private static class AhoCorasickAutomaton {

        private final int[][] go;
        private final int[] fail;
        private final List<String>[] output;
        private int size;
        private static final int ALPHABET_SIZE = Character.MAX_VALUE + 1;

        @SuppressWarnings("unchecked")
        AhoCorasickAutomaton() {
            int maxNodes = 50000;
            go = new int[maxNodes][];
            fail = new int[maxNodes];
            output = new List[maxNodes];
            size = 1;
            go[0] = null;
            output[0] = null;
        }

        void addPattern(String pattern) {
            int cur = 0;
            for (int i = 0; i < pattern.length(); i++) {
                char c = pattern.charAt(i);
                if (go[cur] == null) {
                    go[cur] = new int[0];
                }
                int next = getTransition(cur, c);
                if (next == -1) {
                    next = size++;
                    setTransition(cur, c, next);
                }
                cur = next;
            }
            if (output[cur] == null) {
                output[cur] = new ArrayList<>();
            }
            output[cur].add(pattern);
        }

        void build() {
            Queue<Integer> queue = new LinkedList<>();
            for (int c = 0; c < ALPHABET_SIZE; c++) {
                int s = getTransition(0, (char) c);
                if (s > 0) {
                    fail[s] = 0;
                    queue.add(s);
                }
            }

            while (!queue.isEmpty()) {
                int u = queue.poll();
                if (go[u] == null) continue;
                for (int i = 0; i < go[u].length; i += 2) {
                    char c = (char) go[u][i];
                    int v = go[u][i + 1];
                    queue.add(v);

                    int f = fail[u];
                    while (f != 0 && getTransition(f, c) == -1) {
                        f = fail[f];
                    }
                    int t = getTransition(f, c);
                    fail[v] = (t == -1 || t == v) ? 0 : t;

                    if (output[fail[v]] != null) {
                        if (output[v] == null) {
                            output[v] = new ArrayList<>();
                        }
                        output[v].addAll(output[fail[v]]);
                    }
                }
            }
        }

        void search(String text, MatchCallback callback) {
            int cur = 0;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                while (cur != 0 && getTransition(cur, c) == -1) {
                    cur = fail[cur];
                }
                int next = getTransition(cur, c);
                cur = (next == -1) ? 0 : next;

                if (output[cur] != null) {
                    for (String pattern : output[cur]) {
                        callback.onMatch(pattern, i - pattern.length() + 1);
                    }
                }
            }
        }

        private int getTransition(int state, char c) {
            if (go[state] == null) return -1;
            for (int i = 0; i < go[state].length; i += 2) {
                if (go[state][i] == c) {
                    return go[state][i + 1];
                }
            }
            return -1;
        }

        private void setTransition(int state, char c, int target) {
            if (go[state] == null) {
                go[state] = new int[]{c, target};
            } else {
                int[] old = go[state];
                int[] newArr = new int[old.length + 2];
                System.arraycopy(old, 0, newArr, 0, old.length);
                newArr[old.length] = c;
                newArr[old.length + 1] = target;
                go[state] = newArr;
            }
        }

        @FunctionalInterface
        interface MatchCallback {
            void onMatch(String word, int position);
        }
    }
}
