package com.review.module.filter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickFilterResult {

    private List<BannedWordHit> bannedWordHits;
    private List<String> missingStatements;
    private boolean passed;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BannedWordHit {
        private String word;
        private int offset;
        private String severity;
    }
}
