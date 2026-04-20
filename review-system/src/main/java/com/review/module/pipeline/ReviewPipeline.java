package com.review.module.pipeline;

import com.review.module.agent.dto.ReviewContext;
import com.review.module.pipeline.dto.ReviewPipelineResult;

public interface ReviewPipeline {

    ReviewPipelineResult execute(ReviewContext context);
}
