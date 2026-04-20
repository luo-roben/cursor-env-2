package com.review.module.llm.entity;

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
@Table(name = "llm_call_log")
public class LlmCallLogDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_task_id")
    private Long reviewTaskId;

    @Column(name = "call_type", nullable = false, length = 30)
    private String callType;

    @Column(name = "agent_name", length = 100)
    private String agentName;

    @Column(name = "card_category")
    private Integer cardCategory;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "raw_prompt", columnDefinition = "LONGTEXT")
    private String rawPrompt;

    @Column(name = "raw_response", columnDefinition = "LONGTEXT")
    private String rawResponse;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "model_fallback", nullable = false)
    private Boolean modelFallback;

    @Column(name = "cache_hit", nullable = false)
    private Boolean cacheHit;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (success == null) {
            success = true;
        }
        if (modelFallback == null) {
            modelFallback = false;
        }
        if (cacheHit == null) {
            cacheHit = false;
        }
    }
}
