package com.review.module.review.entity;

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
@Table(name = "review_card_result", uniqueConstraints = {
        @UniqueConstraint(name = "uk_task_card", columnNames = {"task_id", "card_category"})
})
public class ReviewCardResultDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "card_category", nullable = false)
    private Integer cardCategory;

    @Column(name = "card_name", nullable = false, length = 100)
    private String cardName;

    @Column(name = "issue_count", nullable = false)
    private Integer issueCount;

    @Column(name = "max_severity", length = 20)
    private String maxSeverity;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "agent_name", length = 100)
    private String agentName;

    @Column(name = "model_used", length = 100)
    private String modelUsed;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "token_consumed")
    private Integer tokenConsumed;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.issueCount == null) {
            this.issueCount = 0;
        }
        if (this.status == null) {
            this.status = "PENDING";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
