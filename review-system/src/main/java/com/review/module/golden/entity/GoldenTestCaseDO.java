package com.review.module.golden.entity;

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
@Table(name = "golden_test_case")
public class GoldenTestCaseDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "contract_type", length = 100)
    private String contractType;

    @Column(name = "input_content", nullable = false, columnDefinition = "LONGTEXT")
    private String inputContent;

    @Column(name = "expected_verdict", nullable = false, length = 20)
    private String expectedVerdict;

    @Column(name = "expected_issues", columnDefinition = "JSON")
    private String expectedIssues;

    @Column(name = "expected_min_risk_score")
    private Integer expectedMinRiskScore;

    @Column(name = "expected_max_risk_score")
    private Integer expectedMaxRiskScore;

    @Column(name = "tags", columnDefinition = "JSON")
    private String tags;

    @Column(name = "enabled", nullable = false)
    private Integer enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.enabled == null) {
            this.enabled = 1;
        }
    }
}
