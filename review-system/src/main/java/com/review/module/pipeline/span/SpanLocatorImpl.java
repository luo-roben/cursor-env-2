package com.review.module.pipeline.span;

import com.review.module.agent.dto.ReviewIssue;
import org.springframework.stereotype.Component;

@Component
public class SpanLocatorImpl implements SpanLocator {

    @Override
    public ReviewIssue locate(ReviewIssue issue, String fullContent) {
        if (issue == null || fullContent == null || fullContent.isEmpty()) {
            return issue;
        }

        if (issue.getCharOffset() != null && issue.getCharOffset() > 0) {
            if (validateExistingOffset(issue, fullContent)) {
                return issue;
            }
        }

        if (issue.getLocationText() != null && !issue.getLocationText().isEmpty()) {
            int idx = fullContent.indexOf(issue.getLocationText());
            if (idx >= 0) {
                issue.setCharOffset(idx);
                issue.setCharLength(issue.getLocationText().length());
                return issue;
            }

            int normalizedIdx = normalizedSearch(issue.getLocationText(), fullContent);
            if (normalizedIdx >= 0) {
                issue.setCharOffset(normalizedIdx);
                issue.setCharLength(issue.getLocationText().length());
                return issue;
            }
        }

        if (issue.getOriginalText() != null && !issue.getOriginalText().isEmpty()) {
            int idx = fullContent.indexOf(issue.getOriginalText());
            if (idx >= 0) {
                issue.setCharOffset(idx);
                issue.setCharLength(issue.getOriginalText().length());
                return issue;
            }
        }

        return issue;
    }

    private boolean validateExistingOffset(ReviewIssue issue, String fullContent) {
        int offset = issue.getCharOffset();
        Integer length = issue.getCharLength();
        if (length == null || length <= 0) return false;
        if (offset < 0 || offset + length > fullContent.length()) return false;

        String substring = fullContent.substring(offset, offset + length);
        return issue.getOriginalText() != null && substring.equals(issue.getOriginalText());
    }

    private int normalizedSearch(String needle, String haystack) {
        String normalizedNeedle = normalize(needle);
        String normalizedHaystack = normalize(haystack);

        int idx = normalizedHaystack.indexOf(normalizedNeedle);
        if (idx < 0) return -1;

        int originalIdx = mapNormalizedIndexToOriginal(haystack, idx);
        return originalIdx;
    }

    private String normalize(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!Character.isWhitespace(c) && !isPunctuation(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private boolean isPunctuation(char c) {
        return c == ',' || c == '.' || c == '!' || c == '?' || c == ';' || c == ':' ||
                c == '，' || c == '。' || c == '！' || c == '？' || c == '；' || c == '：' ||
                c == '"' || c == '"' || c == '\'' || c == '（' || c == '）' ||
                c == '(' || c == ')' || c == '【' || c == '】' || c == '《' || c == '》';
    }

    private int mapNormalizedIndexToOriginal(String original, int normalizedIndex) {
        int normalizedCount = 0;
        for (int i = 0; i < original.length(); i++) {
            char c = original.charAt(i);
            if (!Character.isWhitespace(c) && !isPunctuation(c)) {
                if (normalizedCount == normalizedIndex) {
                    return i;
                }
                normalizedCount++;
            }
        }
        return -1;
    }
}
