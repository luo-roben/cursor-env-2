package com.review.module.registry.entity;

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
@Table(name = "doc_type_registry")
public class DocTypeRegistryDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_type", nullable = false, length = 50, unique = true)
    private String documentType;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "applicable_cards", nullable = false, columnDefinition = "JSON")
    private String applicableCards;

    @Column(name = "default_parser", length = 50)
    private String defaultParser;

    @Column(name = "required_elements", columnDefinition = "JSON")
    private String requiredElements;

    @Column(name = "max_content_length")
    private Integer maxContentLength;

    @Column(name = "enable_cross_clause")
    private Boolean enableCrossClause;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.maxContentLength == null) this.maxContentLength = 100000;
        if (this.enableCrossClause == null) this.enableCrossClause = false;
        if (this.enabled == null) this.enabled = true;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
