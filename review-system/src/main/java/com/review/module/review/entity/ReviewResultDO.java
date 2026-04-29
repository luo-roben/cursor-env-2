package com.review.module.review.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "review_result")
public class ReviewResultDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "card_category", nullable = false)
    private Integer cardCategory;

    @Column(name = "segment_index")
    private Integer segmentIndex;

    @Column(name = "clause_id", length = 32)
    private String clauseId;

    @Column(name = "cross_ref_clauses", columnDefinition = "JSON")
    private String crossRefClauses;

    @Column(name = "original_text", columnDefinition = "TEXT")
    private String originalText;

    @Column(name = "matched_text", length = 1000)
    private String matchedText;

    @Column(name = "char_offset_start")
    private Integer charOffsetStart;

    @Column(name = "char_offset_end")
    private Integer charOffsetEnd;

    @Column(name = "verdict", nullable = false, length = 20)
    private String verdict;

    @Column(name = "confidence", precision = 3, scale = 2)
    private BigDecimal confidence;

    @Column(name = "issue_type", length = 100)
    private String issueType;

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "law_article_id")
    private Long lawArticleId;

    @Column(name = "cited_article_code", length = 100)
    private String citedArticleCode;

    @Column(name = "cited_law_name", length = 500)
    private String citedLawName;

    @Column(name = "citation_status", length = 20)
    private String citationStatus;

    @Column(name = "verified_original_text", columnDefinition = "TEXT")
    private String verifiedOriginalText;

    @Column(name = "suggestion", columnDefinition = "TEXT")
    private String suggestion;

    @Column(name = "suggestion_type", length = 30)
    private String suggestionType;

    @Column(name = "revised_text", columnDefinition = "TEXT")
    private String revisedText;

    @Column(name = "revision_status", length = 20)
    private String revisionStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.cardCategory == null) {
            this.cardCategory = 4;
        }
        if (this.citationStatus == null) {
            this.citationStatus = "PENDING";
        }
        if (this.revisionStatus == null) {
            this.revisionStatus = "active";
        }
    }
}
