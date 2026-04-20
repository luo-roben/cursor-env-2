package com.review.module.agent.impl;

import com.review.common.enums.CardCategory;
import com.review.module.agent.ReviewCardAgent;
import com.review.module.agent.dto.CustomRuleInfo;
import com.review.module.agent.dto.ReviewContext;
import com.review.module.agent.dto.ReviewIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class RedlineAgent implements ReviewCardAgent {

    @Override
    public CardCategory getCardCategory() {
        return CardCategory.REDLINES;
    }

    @Override
    public List<ReviewIssue> review(ReviewContext context) {
        List<ReviewIssue> issues = new ArrayList<>();
        String content = context.getContent();
        if (content == null || content.isEmpty()) {
            return issues;
        }

        List<String> bannedWords = extractBannedWords(context.getCustomRules());
        if (bannedWords.isEmpty()) {
            return issues;
        }

        AhoCorasickMatcher matcher = new AhoCorasickMatcher(bannedWords);
        List<AhoCorasickMatcher.Match> matches = matcher.search(content);

        for (AhoCorasickMatcher.Match match : matches) {
            String matchedWord = match.keyword();
            String severity = findSeverityForWord(matchedWord, context.getCustomRules());

            issues.add(ReviewIssue.builder()
                    .cardCategory(CardCategory.REDLINES.getCode())
                    .charOffset(match.position())
                    .charLength(matchedWord.length())
                    .originalText(matchedWord)
                    .locationText(extractContext(content, match.position(), 20))
                    .verdict("VIOLATION")
                    .confidence(1.0)
                    .issueType("banned_word")
                    .severity(severity)
                    .description("命中禁用词: " + matchedWord)
                    .suggestion("删除或替换禁用词'" + matchedWord + "'")
                    .suggestionType("REVISION")
                    .build());
        }

        return issues;
    }

    @Override
    public boolean isApplicable(ReviewContext context) {
        return true;
    }

    private List<String> extractBannedWords(List<CustomRuleInfo> rules) {
        if (rules == null) return Collections.emptyList();
        return rules.stream()
                .filter(r -> "BANNED_WORD".equalsIgnoreCase(r.getRuleType()))
                .map(CustomRuleInfo::getContent)
                .filter(Objects::nonNull)
                .toList();
    }

    private String findSeverityForWord(String word, List<CustomRuleInfo> rules) {
        if (rules == null) return "MAJOR";
        return rules.stream()
                .filter(r -> "BANNED_WORD".equalsIgnoreCase(r.getRuleType()))
                .filter(r -> word.equals(r.getContent()))
                .map(CustomRuleInfo::getSeverity)
                .findFirst()
                .orElse("MAJOR");
    }

    private String extractContext(String content, int offset, int radius) {
        int start = Math.max(0, offset - radius);
        int end = Math.min(content.length(), offset + radius);
        return content.substring(start, end);
    }

    static class AhoCorasickMatcher {
        private final int[][] goTo;
        private final int[] fail;
        private final List<List<String>> output;
        private int stateCount;

        AhoCorasickMatcher(List<String> keywords) {
            int maxStates = keywords.stream().mapToInt(String::length).sum() + 1;
            maxStates = Math.max(maxStates, 2);
            int alphabetSize = 65536;

            goTo = new int[maxStates][];
            fail = new int[maxStates];
            output = new ArrayList<>();
            for (int i = 0; i < maxStates; i++) {
                output.add(new ArrayList<>());
            }
            stateCount = 1;

            for (String keyword : keywords) {
                addKeyword(keyword);
            }
            buildFailureLinks();
        }

        private void addKeyword(String keyword) {
            int state = 0;
            for (int i = 0; i < keyword.length(); i++) {
                char ch = keyword.charAt(i);
                if (goTo[state] == null) {
                    goTo[state] = new int[0];
                }
                int next = getGoto(state, ch);
                if (next == -1) {
                    next = stateCount++;
                    setGoto(state, ch, next);
                }
                state = next;
            }
            output.get(state).add(keyword);
        }

        private int getGoto(int state, char ch) {
            if (goTo[state] == null) return -1;
            for (int i = 0; i < goTo[state].length; i += 2) {
                if (goTo[state][i] == ch) return goTo[state][i + 1];
            }
            return -1;
        }

        private void setGoto(int state, char ch, int target) {
            if (goTo[state] == null) {
                goTo[state] = new int[]{ch, target};
            } else {
                int[] old = goTo[state];
                int[] newArr = new int[old.length + 2];
                System.arraycopy(old, 0, newArr, 0, old.length);
                newArr[old.length] = ch;
                newArr[old.length + 1] = target;
                goTo[state] = newArr;
            }
        }

        private Set<Character> getChars(int state) {
            Set<Character> chars = new HashSet<>();
            if (goTo[state] != null) {
                for (int i = 0; i < goTo[state].length; i += 2) {
                    chars.add((char) goTo[state][i]);
                }
            }
            return chars;
        }

        private void buildFailureLinks() {
            Queue<Integer> queue = new LinkedList<>();
            for (char ch : getChars(0)) {
                int s = getGoto(0, ch);
                if (s > 0) {
                    fail[s] = 0;
                    queue.add(s);
                }
            }

            while (!queue.isEmpty()) {
                int r = queue.poll();
                for (char ch : getChars(r)) {
                    int s = getGoto(r, ch);
                    queue.add(s);
                    int state = fail[r];
                    while (state != 0 && getGoto(state, ch) == -1) {
                        state = fail[state];
                    }
                    int g = getGoto(state, ch);
                    fail[s] = (g == -1 || g == s) ? 0 : g;
                    output.get(s).addAll(output.get(fail[s]));
                }
            }
        }

        List<Match> search(String text) {
            List<Match> matches = new ArrayList<>();
            int state = 0;
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                while (state != 0 && getGoto(state, ch) == -1) {
                    state = fail[state];
                }
                int next = getGoto(state, ch);
                state = (next == -1) ? 0 : next;

                for (String keyword : output.get(state)) {
                    matches.add(new Match(i - keyword.length() + 1, keyword));
                }
            }
            return matches;
        }

        record Match(int position, String keyword) {}
    }
}
