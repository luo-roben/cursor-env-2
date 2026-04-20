package com.review.module.pipeline;

import com.review.module.agent.dto.ReviewContext;
import com.review.module.pipeline.dto.ReviewPipelineResult;
import com.review.module.review.entity.ReviewTaskDO;

public interface ReviewPipeline {

    ReviewPipelineResult execute(ReviewContext context);

    default ReviewPipelineResult execute(ReviewTaskDO task) {
        ReviewContext context = ReviewContext.builder()
                .taskId(task.getId())
                .tenantId(task.getTenantId())
                .content(task.getOriginalContent())
                .documentType(task.getDocumentType())
                .contractType(task.getContractType())
                .contentType(task.getContentType())
                .productType(task.getProductType())
                .channel(task.getChannel())
                .build();
        return execute(context);
    }
}
