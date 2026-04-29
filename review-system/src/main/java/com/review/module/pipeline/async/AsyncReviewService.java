package com.review.module.pipeline.async;

import com.review.module.pipeline.ReviewPipeline;
import com.review.module.pipeline.dto.ReviewPipelineResult;
import com.review.module.review.entity.ReviewTaskDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncReviewService {

    private final ReviewPipeline reviewPipeline;

    @Async("reviewTaskExecutor")
    public CompletableFuture<ReviewPipelineResult> executeAsync(ReviewTaskDO task) {
        log.info("Async review started for taskId={}", task.getId());
        try {
            ReviewPipelineResult result = reviewPipeline.execute(task);
            log.info("Async review completed for taskId={}", task.getId());
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Async review failed for taskId={}: {}", task.getId(), e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
