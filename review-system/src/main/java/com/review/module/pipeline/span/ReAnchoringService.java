package com.review.module.pipeline.span;

import com.review.module.agent.dto.ReviewIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ReAnchoringService {

    public List<ReviewIssue> reAnchor(String originalContent, String modifiedContent, List<ReviewIssue> issues) {
        if (originalContent == null || modifiedContent == null || issues == null) {
            return issues;
        }

        String[] oldLines = originalContent.split("\n", -1);
        String[] newLines = modifiedContent.split("\n", -1);

        int[] cumulativeOldOffset = computeLineOffsets(oldLines);
        int[] cumulativeNewOffset = computeLineOffsets(newLines);

        List<LineDiff> diffs = computeLineDiffs(oldLines, newLines);

        int[] shiftAtOldLine = computeShiftMap(oldLines, newLines, diffs, cumulativeOldOffset, cumulativeNewOffset);

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

            int oldLineIndex = findLineForOffset(cumulativeOldOffset, offset);
            int shift = oldLineIndex < shiftAtOldLine.length ? shiftAtOldLine[oldLineIndex] : 0;
            int newOffset = offset + shift;

            if (newOffset >= 0 && newOffset + length <= modifiedContent.length()
                    && modifiedContent.substring(newOffset, newOffset + length).equals(matchedText)) {
                issue.setCharOffset(newOffset);
            } else {
                int fallbackIndex = modifiedContent.indexOf(matchedText);
                if (fallbackIndex >= 0) {
                    issue.setCharOffset(fallbackIndex);
                } else {
                    log.debug("Text was edited, marking issue as stale: '{}'",
                            matchedText.substring(0, Math.min(50, matchedText.length())));
                    issue.setVerdict("stale");
                    issue.setCharOffset(null);
                    issue.setCharLength(null);
                }
            }
        }

        return issues;
    }

    private int[] computeLineOffsets(String[] lines) {
        int[] offsets = new int[lines.length + 1];
        offsets[0] = 0;
        for (int i = 0; i < lines.length; i++) {
            offsets[i + 1] = offsets[i] + lines[i].length() + 1;
        }
        return offsets;
    }

    private int findLineForOffset(int[] cumulativeOffset, int charOffset) {
        for (int i = 0; i < cumulativeOffset.length - 1; i++) {
            if (charOffset < cumulativeOffset[i + 1]) {
                return i;
            }
        }
        return cumulativeOffset.length - 2;
    }

    private List<LineDiff> computeLineDiffs(String[] oldLines, String[] newLines) {
        List<LineDiff> diffs = new ArrayList<>();
        int[][] lcs = new int[oldLines.length + 1][newLines.length + 1];
        for (int i = 1; i <= oldLines.length; i++) {
            for (int j = 1; j <= newLines.length; j++) {
                if (oldLines[i - 1].equals(newLines[j - 1])) {
                    lcs[i][j] = lcs[i - 1][j - 1] + 1;
                } else {
                    lcs[i][j] = Math.max(lcs[i - 1][j], lcs[i][j - 1]);
                }
            }
        }

        int i = oldLines.length, j = newLines.length;
        List<LineDiff> reversed = new ArrayList<>();
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && oldLines[i - 1].equals(newLines[j - 1])) {
                reversed.add(new LineDiff(DiffOp.EQUAL, i - 1, j - 1));
                i--;
                j--;
            } else if (j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j])) {
                reversed.add(new LineDiff(DiffOp.INSERT, -1, j - 1));
                j--;
            } else {
                reversed.add(new LineDiff(DiffOp.DELETE, i - 1, -1));
                i--;
            }
        }

        for (int k = reversed.size() - 1; k >= 0; k--) {
            diffs.add(reversed.get(k));
        }
        return diffs;
    }

    private int[] computeShiftMap(String[] oldLines, String[] newLines,
                                  List<LineDiff> diffs,
                                  int[] oldOffsets, int[] newOffsets) {
        int[] shiftAtOldLine = new int[oldLines.length];
        int cumulativeShift = 0;
        int oldIdx = 0;

        for (LineDiff diff : diffs) {
            switch (diff.op) {
                case EQUAL:
                    while (oldIdx <= diff.oldLine && oldIdx < shiftAtOldLine.length) {
                        shiftAtOldLine[oldIdx] = newOffsets[diff.newLine] - oldOffsets[diff.oldLine];
                        oldIdx++;
                    }
                    break;
                case INSERT:
                    cumulativeShift += newLines[diff.newLine].length() + 1;
                    break;
                case DELETE:
                    if (oldIdx <= diff.oldLine && oldIdx < shiftAtOldLine.length) {
                        shiftAtOldLine[diff.oldLine] = cumulativeShift;
                        oldIdx = diff.oldLine + 1;
                    }
                    cumulativeShift -= oldLines[diff.oldLine].length() + 1;
                    break;
            }
        }
        for (; oldIdx < shiftAtOldLine.length; oldIdx++) {
            shiftAtOldLine[oldIdx] = cumulativeShift;
        }

        return shiftAtOldLine;
    }

    private enum DiffOp { EQUAL, INSERT, DELETE }

    private record LineDiff(DiffOp op, int oldLine, int newLine) {}
}
