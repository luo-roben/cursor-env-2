package com.review.module.rules.entity;

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
@Table(name = "tenant_custom_rule")
public class TenantCustomRuleDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "rule_type", nullable = false, length = 30)
    private String ruleType;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "applicable_content_types", columnDefinition = "JSON")
    private String applicableContentTypes;

    @Column(name = "applicable_doc_types", columnDefinition = "JSON")
    private String applicableDocTypes;

    @Column(name = "applicable_contract_types", columnDefinition = "JSON")
    private String applicableContractTypes;

    @Column(name = "match_mode", length = 20)
    private String matchMode;

    @Column(name = "severity_if_triggered", length = 20)
    private String severityIfTriggered;

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
        if (priority == null) priority = 0;
        if (enabled == null) enabled = true;
        if (matchMode == null) matchMode = "exact";
        if (severityIfTriggered == null) severityIfTriggered = "major";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
