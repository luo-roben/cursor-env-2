package com.review.module.llm.service;

import com.review.module.llm.entity.LlmCallLogDO;

import java.util.List;

public interface LlmCallLogService {

    LlmCallLogDO log(LlmCallLogDO logEntry);

    List<LlmCallLogDO> listByTaskId(Long taskId);
}
