package com.review.module.pipeline.span;

import com.review.module.agent.dto.ReviewIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ReAnchoringService {

    public List<ReviewIssue> reAnchor(String originalContent, String modifiedContent, List<ReviewIssue> issues) {
        if (originalContent == null || modifiedContent == null || issues == null) {
            return issues;
        }

        for (ReviewIssue issue : issues) {
            if (issue.getCharOffset() == null || issue.getCharLength() == null) {
                continue;
            }

            int offset = issue.getCharOffset();
            int length = issue.getCharLength();

            if (offset < 0 || offset + length > originalContent.length()) {
                issue.setCharOffset(null);
                issue.setCharLength(null);
                continue;
            }

            String matchedText = originalContent.substring(offset, offset + length);

            int newIndex = modifiedContent.indexOf(matchedText);
            if (newIndex >= 0) {
                issue.setCharOffset(newIndex);
            } else {
                log.debug("Text was edited, marking issue as stale: '{}'",
                        matchedText.substring(0, Math.min(50, matchedText.length())));
                issue.setVerdict("stale");
                issue.setCharOffset(null);
                issue.setCharLength(null);
            }
        }

        return issues;
    }
}
