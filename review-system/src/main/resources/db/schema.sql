-- ============================================================
-- Universal Intelligent Review System — v2 Schema
-- MySQL 8.x, InnoDB, utf8mb4
-- ============================================================

-- 1. 租户表
CREATE TABLE IF NOT EXISTS tenant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL COMMENT '租户名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '租户编码',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1=正常 0=禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';

-- 2. 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    username VARCHAR(100) NOT NULL COMMENT '用户名',
    password VARCHAR(200) NOT NULL COMMENT '密码(BCrypt)',
    real_name VARCHAR(100) COMMENT '真实姓名',
    role VARCHAR(50) NOT NULL DEFAULT 'REVIEWER' COMMENT '角色: SUPER_ADMIN/TENANT_ADMIN/REVIEWER/AUDITOR/VIEWER',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1=正常 0=禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id),
    UNIQUE KEY uk_tenant_username (tenant_id, username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 3. 法规来源表
CREATE TABLE IF NOT EXISTS law_source (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_id VARCHAR(100) NOT NULL UNIQUE COMMENT '全局唯一标识, 格式: {来源缩写}-{年份}-{序号}',
    title VARCHAR(500) NOT NULL COMMENT '法规全称',
    issuer VARCHAR(200) NOT NULL COMMENT '发布机构',
    issue_date DATE COMMENT '发布日期',
    effective_date DATE COMMENT '生效日期',
    doc_type VARCHAR(50) NOT NULL COMMENT '法律/行政法规/部门规章/规范性文件/自律规则/监管问答',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT 'active/amended/repealed',
    full_text LONGTEXT COMMENT '法规全文',
    source_url VARCHAR(1000) COMMENT '原始来源URL',
    applicable_doc_types JSON COMMENT '适用文档类型列表',
    parse_status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/parsing/parsed/confirmed/failed',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='法规来源表';

-- 4. 法条知识表
CREATE TABLE IF NOT EXISTS law_article (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_id BIGINT NOT NULL COMMENT '关联法规来源ID',
    law_name VARCHAR(500) NOT NULL COMMENT '法律全称',
    law_short_name VARCHAR(200) COMMENT '法律简称',
    article_id VARCHAR(100) NOT NULL COMMENT '法条编号',
    original_text TEXT NOT NULL COMMENT '法条原文(不可改写)',
    norm_type VARCHAR(20) NOT NULL COMMENT '禁止/义务/权利/定义/程序',
    subject JSON COMMENT '规范主体列表',
    behavior TEXT COMMENT '规范行为描述',
    object_desc VARCHAR(500) COMMENT '行为作用对象',
    applicable_condition TEXT COMMENT '适用条件/前提',
    applicable_scenarios JSON COMMENT '适用场景列表',
    applicable_content_types JSON COMMENT '适用内容类型列表',
    applicable_product_types JSON COMMENT '适用产品类型列表',
    applicable_doc_types JSON COMMENT '适用文档类型列表',
    contract_clause_role VARCHAR(50) COMMENT '合同条款角色: definition/rights/obligations/liability/dispute_resolution/termination/general',
    contract_types JSON COMMENT '适用合同类型列表',
    key_phrases JSON COMMENT '关键词列表',
    semantic_extensions JSON COMMENT '语义延伸列表',
    violation_examples JSON COMMENT '违规示例列表',
    compliant_examples JSON COMMENT '合规示例列表',
    penalty TEXT COMMENT '违反后果',
    related_articles JSON COMMENT '关联法条列表',
    authority_level TINYINT NOT NULL DEFAULT 3 COMMENT '效力层级: 1=法律 2=行政法规 3=部门规章 4=规范性文件 5=自律规则 6=监管问答',
    status VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT 'draft/pending_review/published/deprecated',
    confirmed_by BIGINT COMMENT '确认人ID',
    confirmed_at DATETIME COMMENT '确认时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_law_name (law_name(100)),
    INDEX idx_article_id (article_id),
    INDEX idx_status (status),
    INDEX idx_norm_type (norm_type),
    INDEX idx_authority (authority_level),
    INDEX idx_source (source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='法条知识表';

-- 5. 合同模板表
CREATE TABLE IF NOT EXISTS contract_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id VARCHAR(64) NOT NULL UNIQUE COMMENT '模板唯一标识',
    template_name VARCHAR(256) NOT NULL COMMENT '模板名称',
    contract_type VARCHAR(64) NOT NULL COMMENT '合同类型: PURCHASE/SALES/SERVICE/NDA/EMPLOYMENT/EQUITY/LEASE/LOAN/GENERAL',
    industry VARCHAR(64) DEFAULT '通用' COMMENT '行业',
    source VARCHAR(64) NOT NULL COMMENT '来源: national_standard/industry_standard/enterprise/review_deposit',
    source_name VARCHAR(256) COMMENT '来源名称',
    version VARCHAR(32) COMMENT '版本号',
    status VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT 'draft/pending_review/published/deprecated',
    tenant_id BIGINT COMMENT '租户ID(null=全局模板)',
    total_clauses INT COMMENT '条款总数',
    total_chars INT COMMENT '总字数',
    parties JSON COMMENT '合同当事方',
    required_clause_roles JSON COMMENT '必备条款角色列表',
    applicable_scenarios JSON COMMENT '适用场景列表',
    confirmed_by BIGINT COMMENT '确认人ID',
    confirmed_at DATETIME COMMENT '确认时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_contract_type (contract_type),
    INDEX idx_industry (industry),
    INDEX idx_status (status),
    INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同模板表';

-- 6. 合同模板条款表
CREATE TABLE IF NOT EXISTS contract_template_clause (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id VARCHAR(64) NOT NULL COMMENT '关联模板ID',
    clause_id VARCHAR(32) NOT NULL COMMENT '条款编号, 如 1, 1.1, 1.1.1',
    clause_title VARCHAR(256) COMMENT '条款标题',
    clause_role VARCHAR(32) NOT NULL COMMENT '条款角色: DEFINITION/OBLIGATION/RIGHT/LIABILITY/TERMINATION/DISPUTE/GENERAL',
    clause_level INT NOT NULL DEFAULT 1 COMMENT '层级: 1=条 2=款 3=项',
    parent_clause_id VARCHAR(32) COMMENT '父级条款编号',
    clause_text TEXT NOT NULL COMMENT '条款原文',
    is_required TINYINT NOT NULL DEFAULT 0 COMMENT '是否必备条款',
    risk_level VARCHAR(16) DEFAULT 'low' COMMENT 'high/medium/low',
    annotations JSON COMMENT '标注信息列表',
    char_offset_start INT COMMENT '字符起始偏移',
    char_offset_end INT COMMENT '字符结束偏移',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_template (template_id),
    INDEX idx_clause_role (clause_role),
    INDEX idx_required (is_required),
    UNIQUE KEY uk_template_clause (template_id, clause_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同模板条款表';

-- 7. 审查任务表
CREATE TABLE IF NOT EXISTS review_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    submitted_by BIGINT NOT NULL COMMENT '提交人ID',
    document_type VARCHAR(50) NOT NULL DEFAULT 'OTHER' COMMENT '文档类型: MARKETING/CONTRACT/PROSPECTUS/REPORT/OTHER',
    content_type VARCHAR(50) NOT NULL COMMENT '内容类型: 营销海报/采购合同/招募说明书等',
    contract_type VARCHAR(50) COMMENT '合同类型(仅合同文档)',
    product_type VARCHAR(50) COMMENT '产品类型',
    channel VARCHAR(50) COMMENT '渠道',
    original_content LONGTEXT COMMENT '原始文本内容',
    file_url VARCHAR(1000) COMMENT '上传文件URL',
    parsed_segments JSON COMMENT '解析后的分段内容',
    clause_tree JSON COMMENT '条款树(合同文档)',
    metadata JSON COMMENT '元数据',
    overall_verdict VARCHAR(20) COMMENT 'violation/compliant/needs_review',
    risk_score INT DEFAULT 0 COMMENT '风险评分 0-100',
    risk_level VARCHAR(10) COMMENT 'high/medium/low',
    review_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/REVIEWING/COMPLETED/HUMAN_REVIEWED',
    enabled_cards JSON COMMENT '启用的审查卡片列表',
    llm_model VARCHAR(100) COMMENT '使用的LLM模型',
    total_latency_ms INT COMMENT '总耗时(毫秒)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME COMMENT '完成时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id),
    INDEX idx_status (review_status),
    INDEX idx_submitted_by (submitted_by),
    INDEX idx_created_at (created_at),
    INDEX idx_document_type (document_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审查任务表';

-- 8. 审查结果明细表 (v2: 字符级定位 + 分层建议)
CREATE TABLE IF NOT EXISTS review_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '审查任务ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    card_category INT NOT NULL DEFAULT 4 COMMENT '审查卡片: 1=文本基础 2=红线 3=要素 4=语义合规 5=逻辑条款',
    segment_index INT COMMENT '段落序号',
    clause_id VARCHAR(32) COMMENT '条款编号(合同文档)',
    cross_ref_clauses JSON COMMENT '关联条款编号列表',
    original_text TEXT COMMENT '段落/条款原文',
    matched_text VARCHAR(1000) COMMENT '精确匹配的原文子串(字符级定位)',
    char_offset_start INT COMMENT '字符起始偏移',
    char_offset_end INT COMMENT '字符结束偏移',
    verdict VARCHAR(20) NOT NULL COMMENT 'violation/compliant/needs_review',
    confidence DECIMAL(3,2) COMMENT 'LLM置信度(0-1)',
    issue_type VARCHAR(100) COMMENT '问题类型标识',
    severity VARCHAR(20) COMMENT 'critical/major/minor/info',
    description TEXT COMMENT '问题详细描述',
    law_article_id BIGINT COMMENT '引用的法条ID',
    cited_article_code VARCHAR(100) COMMENT 'LLM输出的法条编号',
    cited_law_name VARCHAR(500) COMMENT 'LLM输出的法律名称',
    citation_status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'VERIFIED/CORRECTED/UNVERIFIED/PENDING',
    verified_original_text TEXT COMMENT '回填的法条原文',
    suggestion TEXT COMMENT '修改建议',
    suggestion_type VARCHAR(30) COMMENT '建议类型: TIP/REVISION/NEGOTIATION_POINT',
    revised_text TEXT COMMENT '可直接采纳的修订文本',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task (task_id),
    INDEX idx_tenant (tenant_id),
    INDEX idx_verdict (verdict),
    INDEX idx_card (card_category),
    INDEX idx_severity (severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审查结果明细表';

-- 9. 审查缺失项表
CREATE TABLE IF NOT EXISTS review_missing_element (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '审查任务ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    card_category INT NOT NULL DEFAULT 3 COMMENT '审查卡片(通常为3=要素审查)',
    element VARCHAR(200) NOT NULL COMMENT '缺失元素名称',
    requirement TEXT COMMENT '法律要求说明',
    law_article_id BIGINT COMMENT '对应法条ID',
    severity VARCHAR(20) NOT NULL DEFAULT 'major' COMMENT 'critical/major/minor',
    suggestion TEXT COMMENT '补充建议',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task (task_id),
    INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审查缺失项表';

-- 10. 人工反馈表
CREATE TABLE IF NOT EXISTS human_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    result_id BIGINT COMMENT '审查结果明细ID',
    task_id BIGINT NOT NULL COMMENT '审查任务ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    reviewer_id BIGINT NOT NULL COMMENT '复核人ID',
    action VARCHAR(20) NOT NULL COMMENT 'CONFIRMED/REJECTED/MODIFIED/SUPPLEMENTED',
    original_verdict VARCHAR(20) COMMENT 'LLM原始判定',
    final_verdict VARCHAR(20) COMMENT '人工最终判定',
    modified_severity VARCHAR(20) COMMENT '修改后的严重程度',
    modified_reason TEXT COMMENT '修改后的理由',
    reject_reason TEXT COMMENT '驳回理由',
    supplement_issue JSON COMMENT '补充的问题',
    comment TEXT COMMENT '复核备注',
    is_typical_case TINYINT NOT NULL DEFAULT 0 COMMENT '是否标记为典型案例',
    reviewed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task (task_id),
    INDEX idx_tenant (tenant_id),
    INDEX idx_reviewer (reviewer_id),
    INDEX idx_result (result_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人工反馈表';

-- 11. 审核案例表
CREATE TABLE IF NOT EXISTS review_case (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    source VARCHAR(30) NOT NULL COMMENT 'internal_review/external_penalty/manual_import',
    document_type VARCHAR(50) COMMENT '文档类型',
    content_type VARCHAR(50) COMMENT '内容类型',
    contract_type VARCHAR(50) COMMENT '合同类型',
    product_type VARCHAR(50) COMMENT '产品类型',
    channel VARCHAR(50) COMMENT '渠道',
    review_card INT COMMENT '审查卡片编号',
    reviewed_content TEXT NOT NULL COMMENT '被审查内容',
    verdict VARCHAR(20) NOT NULL COMMENT 'violation/compliant',
    severity VARCHAR(20) COMMENT 'critical/major/minor',
    reason TEXT COMMENT '判定理由',
    law_references JSON COMMENT '法条引用列表',
    suggestion TEXT COMMENT '修改建议',
    suggestion_type VARCHAR(30) COMMENT '建议类型',
    ai_original_verdict VARCHAR(20) COMMENT 'LLM原始判定',
    human_action VARCHAR(20) COMMENT 'CONFIRMED/REJECTED/MODIFIED/SUPPLEMENTED',
    human_reviewer_id BIGINT COMMENT '人工复核人',
    learning_value_score INT DEFAULT 0 COMMENT '学习价值分',
    is_typical TINYINT NOT NULL DEFAULT 0 COMMENT '是否为典型案例',
    is_shared TINYINT NOT NULL DEFAULT 0 COMMENT '是否跨租户共享',
    decay_weight DECIMAL(5,4) DEFAULT 1.0000 COMMENT '时间衰减权重',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT 'active/archived/deprecated',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id),
    INDEX idx_verdict (verdict),
    INDEX idx_content_type (content_type),
    INDEX idx_document_type (document_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核案例表';

-- 12. 企业自定义规则表
CREATE TABLE IF NOT EXISTS tenant_custom_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    rule_type VARCHAR(30) NOT NULL COMMENT 'BANNED_WORD/REQUIRED_STATEMENT/PRODUCT_INFO/NATURAL_LANGUAGE/CONTRACT_CLAUSE_TEMPLATE',
    content TEXT NOT NULL COMMENT '规则内容',
    priority INT NOT NULL DEFAULT 0 COMMENT '优先级(越大越高)',
    applicable_content_types JSON COMMENT '适用的内容类型(null=全部)',
    applicable_doc_types JSON COMMENT '适用的文档类型(null=全部)',
    applicable_contract_types JSON COMMENT '适用的合同类型(null=全部)',
    match_mode VARCHAR(20) DEFAULT 'exact' COMMENT 'exact/semantic',
    severity_if_triggered VARCHAR(20) DEFAULT 'major' COMMENT '触发后的严重程度',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id),
    INDEX idx_type (rule_type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业自定义规则表';

-- 13. 合规自检清单表
CREATE TABLE IF NOT EXISTS compliance_checklist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_type VARCHAR(50) COMMENT '文档类型',
    content_type VARCHAR(50) NOT NULL COMMENT '内容类型',
    contract_type VARCHAR(50) COMMENT '合同类型',
    product_type VARCHAR(50) COMMENT '产品类型(null=适用所有)',
    check_item VARCHAR(500) NOT NULL COMMENT '检查项描述',
    check_method VARCHAR(20) NOT NULL COMMENT 'keyword/semantic/layout/conditional',
    law_article_id BIGINT COMMENT '关联法条ID',
    condition_desc VARCHAR(500) COMMENT '触发条件描述',
    severity_if_missing VARCHAR(20) NOT NULL DEFAULT 'major' COMMENT 'critical/major/minor',
    tenant_id BIGINT COMMENT '租户级覆盖(null=全局)',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_content_type (content_type),
    INDEX idx_document_type (document_type),
    INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合规自检清单表';

-- 14. LLM调用日志表
CREATE TABLE IF NOT EXISTS llm_call_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_task_id BIGINT COMMENT '关联审查任务ID',
    call_type VARCHAR(30) NOT NULL COMMENT 'review/parse/summary/clause_parse/template_match',
    agent_name VARCHAR(100) COMMENT 'Agent名称',
    card_category INT COMMENT '审查卡片编号',
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称',
    raw_prompt LONGTEXT COMMENT '完整Prompt',
    raw_response LONGTEXT COMMENT '原始响应',
    prompt_tokens INT COMMENT 'Prompt Token数',
    completion_tokens INT COMMENT '输出Token数',
    latency_ms INT COMMENT '调用耗时(毫秒)',
    success TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功',
    error_message TEXT COMMENT '错误信息',
    model_fallback TINYINT NOT NULL DEFAULT 0 COMMENT '是否使用了备用模型',
    cache_hit TINYINT NOT NULL DEFAULT 0 COMMENT '是否命中缓存',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task (review_task_id),
    INDEX idx_type (call_type),
    INDEX idx_agent (agent_name),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM调用日志表';

-- 15. 审查卡片结果表 (v2: 按卡片分组的汇总)
CREATE TABLE IF NOT EXISTS review_card_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '审查任务ID',
    card_category INT NOT NULL COMMENT '审查卡片: 1=文本基础 2=红线 3=要素 4=语义合规 5=逻辑条款',
    card_name VARCHAR(100) NOT NULL COMMENT '卡片名称',
    issue_count INT NOT NULL DEFAULT 0 COMMENT '问题数量',
    max_severity VARCHAR(20) COMMENT '最高严重程度',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/COMPLETED/SKIPPED/FAILED',
    agent_name VARCHAR(100) COMMENT 'Agent名称',
    model_used VARCHAR(100) COMMENT '使用的模型',
    latency_ms INT COMMENT '卡片耗时(毫秒)',
    token_consumed INT COMMENT '消耗Token数',
    error_message TEXT COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_task (task_id),
    INDEX idx_card (card_category),
    UNIQUE KEY uk_task_card (task_id, card_category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审查卡片结果表';

-- ============================================================
-- Default Data
-- ============================================================

INSERT INTO tenant (name, code) VALUES ('默认租户', 'default')
    ON DUPLICATE KEY UPDATE name = name;

INSERT INTO sys_user (tenant_id, username, password, real_name, role)
VALUES (1, 'admin', '$2a$10$EqKcp1WFKVQIShMozy/Bj.cMGSIfTNFXJGxKVJcBJA1YzLb.c.Biq', '系统管理员', 'SUPER_ADMIN')
    ON DUPLICATE KEY UPDATE username = username;
-- Default password: admin123
