package com.review.module.feedback.entity;

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
@Table(name = "reviewer_metrics", uniqueConstraints = {
        @UniqueConstraint(name = "uk_reviewer_period", columnNames = {"reviewer_id", "tenant_id", "period"})
})
public class ReviewerMetricsDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "period", nullable = false, length = 20)
    private String period;

    @Column(name = "total_reviews")
    private Integer totalReviews;

    @Column(name = "confirmed_count")
    private Integer confirmedCount;

    @Column(name = "rejected_count")
    private Integer rejectedCount;

    @Column(name = "modified_count")
    private Integer modifiedCount;

    @Column(name = "supplemented_count")
    private Integer supplementedCount;

    @Column(name = "avg_review_time_seconds")
    private Integer avgReviewTimeSeconds;

    @Column(name = "agreement_rate", precision = 5, scale = 2)
    private BigDecimal agreementRate;

    @Column(name = "kappa_score", precision = 5, scale = 3)
    private BigDecimal kappaScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.totalReviews == null) this.totalReviews = 0;
        if (this.confirmedCount == null) this.confirmedCount = 0;
        if (this.rejectedCount == null) this.rejectedCount = 0;
        if (this.modifiedCount == null) this.modifiedCount = 0;
        if (this.supplementedCount == null) this.supplementedCount = 0;
    }
}
