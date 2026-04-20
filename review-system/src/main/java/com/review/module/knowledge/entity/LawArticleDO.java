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
@Table(name = "law_article")
public class LawArticleDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "law_name", nullable = false, length = 500)
    private String lawName;

    @Column(name = "law_short_name", length = 200)
    private String lawShortName;

    @Column(name = "article_id", nullable = false, length = 100)
    private String articleId;

    @Column(name = "original_text", nullable = false, columnDefinition = "TEXT")
    private String originalText;

    @Column(name = "norm_type", nullable = false, length = 20)
    private String normType;

    @Column(name = "subject", columnDefinition = "JSON")
    private String subject;

    @Column(name = "behavior", columnDefinition = "TEXT")
    private String behavior;

    @Column(name = "object_desc", length = 500)
    private String objectDesc;

    @Column(name = "applicable_condition", columnDefinition = "TEXT")
    private String applicableCondition;

    @Column(name = "applicable_scenarios", columnDefinition = "JSON")
    private String applicableScenarios;

    @Column(name = "applicable_content_types", columnDefinition = "JSON")
    private String applicableContentTypes;

    @Column(name = "applicable_product_types", columnDefinition = "JSON")
    private String applicableProductTypes;

    @Column(name = "applicable_doc_types", columnDefinition = "JSON")
    private String applicableDocTypes;

    @Column(name = "contract_clause_role", length = 50)
    private String contractClauseRole;

    @Column(name = "contract_types", columnDefinition = "JSON")
    private String contractTypes;

    @Column(name = "key_phrases", columnDefinition = "JSON")
    private String keyPhrases;

    @Column(name = "semantic_extensions", columnDefinition = "JSON")
    private String semanticExtensions;

    @Column(name = "violation_examples", columnDefinition = "JSON")
    private String violationExamples;

    @Column(name = "compliant_examples", columnDefinition = "JSON")
    private String compliantExamples;

    @Column(name = "penalty", columnDefinition = "TEXT")
    private String penalty;

    @Column(name = "related_articles", columnDefinition = "JSON")
    private String relatedArticles;

    @Column(name = "authority_level", nullable = false)
    private Integer authorityLevel;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

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
        if (this.authorityLevel == null) {
            this.authorityLevel = 3;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
