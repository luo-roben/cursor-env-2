package com.review.module.knowledge.entity;

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
@Table(name = "contract_template")
public class ContractTemplateDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false, unique = true, length = 64)
    private String templateId;

    @Column(name = "template_name", nullable = false, length = 256)
    private String templateName;

    @Column(name = "contract_type", nullable = false, length = 64)
    private String contractType;

    @Column(name = "industry", length = 64)
    private String industry;

    @Column(name = "source", nullable = false, length = 64)
    private String source;

    @Column(name = "source_name", length = 256)
    private String sourceName;

    @Column(name = "version", length = 32)
    private String version;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "total_clauses")
    private Integer totalClauses;

    @Column(name = "total_chars")
    private Integer totalChars;

    @Column(name = "parties", columnDefinition = "JSON")
    private String parties;

    @Column(name = "required_clause_roles", columnDefinition = "JSON")
    private String requiredClauseRoles;

    @Column(name = "applicable_scenarios", columnDefinition = "JSON")
    private String applicableScenarios;

    @Column(name = "confirmed_by")
    private Long confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = "draft";
        }
        if (this.industry == null) {
            this.industry = "通用";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
