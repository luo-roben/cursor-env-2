package com.compliance.module.filter.dto;

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
    private List<MissingStatement> missingStatements;
    private boolean passed;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BannedWordHit {
        private String word;
        private int position;
        private Long ruleId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissingStatement {
        private String statement;
        private Long ruleId;
        private String severity;
    }
}
