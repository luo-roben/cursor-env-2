package com.compliance.module.checklist.entity;

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
@Table(name = "compliance_checklist")
public class ComplianceChecklistDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "product_type", length = 50)
    private String productType;

    @Column(name = "check_item", nullable = false, length = 500)
    private String checkItem;

    @Column(name = "check_method", nullable = false, length = 20)
    private String checkMethod;

    @Column(name = "law_article_id")
    private Long lawArticleId;

    @Column(name = "condition_desc", length = 500)
    private String conditionDesc;

    @Column(name = "severity_if_missing", nullable = false, length = 20)
    private String severityIfMissing;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (severityIfMissing == null) {
            severityIfMissing = "major";
        }
        if (enabled == null) {
            enabled = true;
        }
    }
}
