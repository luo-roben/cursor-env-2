package com.review.module.review.entity;

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
@Table(name = "contract_clause_node")
public class ClauseNodeDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "clause_number", nullable = false, length = 50)
    private String clauseNumber;

    @Column(name = "clause_title", length = 200)
    private String clauseTitle;

    @Column(name = "clause_type", length = 50)
    private String clauseType;

    @Column(name = "char_offset")
    private Integer charOffset;

    @Column(name = "char_length")
    private Integer charLength;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
