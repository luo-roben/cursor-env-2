package com.compliance.module.feedback.entity;

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
@Table(name = "human_feedback")
public class HumanFeedbackDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "result_id")
    private Long resultId;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @Column(name = "original_verdict", length = 20)
    private String originalVerdict;

    @Column(name = "final_verdict", length = 20)
    private String finalVerdict;

    @Column(name = "modified_severity", length = 20)
    private String modifiedSeverity;

    @Column(name = "modified_reason", columnDefinition = "TEXT")
    private String modifiedReason;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "supplement_issue", columnDefinition = "JSON")
    private String supplementIssue;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "is_typical_case", nullable = false)
    private Boolean isTypicalCase;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        if (reviewedAt == null) {
            reviewedAt = LocalDateTime.now();
        }
        if (isTypicalCase == null) {
            isTypicalCase = false;
        }
    }
}
