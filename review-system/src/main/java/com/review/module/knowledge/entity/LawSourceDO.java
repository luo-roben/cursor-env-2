package com.review.module.knowledge.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "law_source")
public class LawSourceDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_id", nullable = false, unique = true, length = 100)
    private String sourceId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "issuer", nullable = false, length = 200)
    private String issuer;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "doc_type", nullable = false, length = 50)
    private String docType;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "full_text", columnDefinition = "LONGTEXT")
    private String fullText;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "applicable_doc_types", columnDefinition = "JSON")
    private String applicableDocTypes;

    @Column(name = "parse_status", nullable = false, length = 20)
    private String parseStatus;

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
            this.status = "active";
        }
        if (this.parseStatus == null) {
            this.parseStatus = "pending";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
