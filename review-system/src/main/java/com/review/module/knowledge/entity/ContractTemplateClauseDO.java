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
@Table(name = "contract_template_clause", uniqueConstraints = {
        @UniqueConstraint(name = "uk_template_clause", columnNames = {"template_id", "clause_id"})
})
public class ContractTemplateClauseDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false, length = 64)
    private String templateId;

    @Column(name = "clause_id", nullable = false, length = 32)
    private String clauseId;

    @Column(name = "clause_title", length = 256)
    private String clauseTitle;

    @Column(name = "clause_role", nullable = false, length = 32)
    private String clauseRole;

    @Column(name = "clause_level", nullable = false)
    private Integer clauseLevel;

    @Column(name = "parent_clause_id", length = 32)
    private String parentClauseId;

    @Column(name = "clause_text", nullable = false, columnDefinition = "TEXT")
    private String clauseText;

    @Column(name = "is_required", nullable = false)
    private Integer isRequired;

    @Column(name = "risk_level", length = 16)
    private String riskLevel;

    @Column(name = "annotations", columnDefinition = "JSON")
    private String annotations;

    @Column(name = "char_offset_start")
    private Integer charOffsetStart;

    @Column(name = "char_offset_end")
    private Integer charOffsetEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.isRequired == null) {
            this.isRequired = 0;
        }
        if (this.clauseLevel == null) {
            this.clauseLevel = 1;
        }
        if (this.riskLevel == null) {
            this.riskLevel = "low";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
