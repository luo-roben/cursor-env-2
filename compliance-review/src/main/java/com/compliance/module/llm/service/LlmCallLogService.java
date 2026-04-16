package com.compliance.module.llm.service;

import com.compliance.module.llm.entity.LlmCallLogDO;

import java.util.List;

public interface LlmCallLogService {

    LlmCallLogDO log(LlmCallLogDO logEntry);

    List<LlmCallLogDO> listByTaskId(Long reviewTaskId);
}
