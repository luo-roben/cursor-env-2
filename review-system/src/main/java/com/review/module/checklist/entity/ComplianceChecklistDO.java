package com.review.module.checklist.entity;

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

    @Column(name = "document_type", length = 50)
    private String documentType;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "contract_type", length = 50)
    private String contractType;

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

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (enabled == null) enabled = true;
        if (severityIfMissing == null) severityIfMissing = "major";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
