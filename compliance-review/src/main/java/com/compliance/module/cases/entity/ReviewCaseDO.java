package com.compliance.module.cases.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "review_case")
public class ReviewCaseDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "source", nullable = false, length = 30)
    private String source;

    @Column(name = "content_type", length = 50)
    private String contentType;

    @Column(name = "product_type", length = 50)
    private String productType;

    @Column(name = "channel", length = 50)
    private String channel;

    @Column(name = "reviewed_content", nullable = false, columnDefinition = "TEXT")
    private String reviewedContent;

    @Column(name = "verdict", nullable = false, length = 20)
    private String verdict;

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "law_references", columnDefinition = "JSON")
    private String lawReferences;

    @Column(name = "suggestion", columnDefinition = "TEXT")
    private String suggestion;

    @Column(name = "ai_original_verdict", length = 20)
    private String aiOriginalVerdict;

    @Column(name = "human_action", length = 20)
    private String humanAction;

    @Column(name = "human_reviewer_id")
    private Long humanReviewerId;

    @Column(name = "learning_value_score")
    private Integer learningValueScore;

    @Column(name = "is_typical", nullable = false)
    private Boolean isTypical;

    @Column(name = "is_shared", nullable = false)
    private Boolean isShared;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (learningValueScore == null) {
            learningValueScore = 0;
        }
        if (isTypical == null) {
            isTypical = false;
        }
        if (isShared == null) {
            isShared = false;
        }
    }
}
