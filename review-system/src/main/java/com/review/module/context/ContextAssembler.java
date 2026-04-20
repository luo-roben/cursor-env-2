package com.review.module.context;

import com.review.module.agent.dto.ReviewContext;
import com.review.module.context.dto.AssembledContext;

public interface ContextAssembler {

    AssembledContext assemble(ReviewContext context);
}
