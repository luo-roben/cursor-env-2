package com.review.module.cases.entity;

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
@Table(name = "review_case")
public class ReviewCaseDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "source", nullable = false, length = 30)
    private String source;

    @Column(name = "document_type", length = 50)
    private String documentType;

    @Column(name = "content_type", length = 50)
    private String contentType;

    @Column(name = "contract_type", length = 50)
    private String contractType;

    @Column(name = "product_type", length = 50)
    private String productType;

    @Column(name = "channel", length = 50)
    private String channel;

    @Column(name = "review_card")
    private Integer reviewCard;

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

    @Column(name = "suggestion_type", length = 30)
    private String suggestionType;

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

    @Column(name = "decay_weight", precision = 5, scale = 4)
    private BigDecimal decayWeight;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (isTypical == null) isTypical = false;
        if (isShared == null) isShared = false;
        if (decayWeight == null) decayWeight = BigDecimal.ONE;
        if (status == null) status = "active";
        if (learningValueScore == null) learningValueScore = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
