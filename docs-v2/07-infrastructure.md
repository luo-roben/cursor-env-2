# 基础设施与部署（v2 — 通用智能审查系统）

> 版本: v2.0 | 系统名称: 通用智能审查系统（Universal Intelligent Review System）
>
> 本文档涵盖多租户与权限模型、快速过滤器、完整数据模型、非功能性需求、技术选型与部署架构、6 阶段 MVP 划分及术语表。
> 相比 v1 的主要变更：新增合同审查数据模型（模板库、条款解析、内部条款图谱）、5 类审查卡片结果表、修订后的 MVP 分期、技术栈调整（Neo4j 延迟至 Phase 3、新增 StateGraph / Prompt 缓存 / 模型路由层）。

---

## 目录

- [一、多租户与权限模型](#一多租户与权限模型)
- [二、快速过滤器（原 Drools 层降级）](#二快速过滤器原-drools-层降级)
- [三、数据模型总览](#三数据模型总览)
- [四、非功能性需求](#四非功能性需求)
- [五、技术选型与部署架构](#五技术选型与部署架构)
- [六、MVP 阶段划分（6 期）](#六mvp-阶段划分6-期)
- [附录：术语表](#附录术语表)

---

## 一、多租户与权限模型

### 1.1 租户隔离

```
┌─────────────────────────────────────────────────────────────────┐
│  多租户数据隔离                                                   │
│                                                                  │
│  共享层 (所有租户共用):                                            │
│  · 法律法规知识库 — 法律是公共的，所有租户看到同一份法条            │
│  · 处罚案例库 — 公开案例对所有租户可见                             │
│  · 合同模板库 (公共部分) — 行业标准合同模板共享                    │
│                                                                  │
│  隔离层 (每个租户独立):                                            │
│  · 审查记录 — 租户只能看到自己的审查历史                           │
│  · 案例库 — 人工复核沉淀的案例仅对本租户可见                       │
│    (可选: 脱敏后跨租户共享，需租户授权)                            │
│  · 自定义规则 — 禁用词/必备声明/产品信息/自然语言规则              │
│  · 合同模板库 (私有部分) — 企业自建合同模板仅本租户可见            │
│  · 合同数据 — 合同原文、条款解析结果、审查定位数据严格隔离         │
│  · 用户与角色 — 租户内部的用户管理                                │
│                                                                  │
│  隔离实现:                                                        │
│  · PostgreSQL: 每张表加 tenant_id 字段 + 行级安全策略(RLS)        │
│  · Milvus: 按 tenant_id 分 Partition                             │
│  · Elasticsearch: 按 tenant_id 做文档级过滤                      │
└─────────────────────────────────────────────────────────────────┘
```

> **v2 新增：合同数据隔离。** 合同正文、条款解析结果、定位数据（字符 span）属于高度敏感的商业信息，必须做到租户间零泄漏。除 RLS 外，合同相关表的 API 层额外增加 tenant_id 断言校验。

### 1.2 角色与权限

| 角色 | 权限 | 说明 |
|------|------|------|
| 超级管理员 (平台方) | 管理法律知识库、管理租户、查看平台统计 | 平台运营团队 |
| 租户管理员 | 管理租户内用户、配置自定义规则、管理合同模板库、查看审计日志 | 企业合规负责人 |
| 审查员 | 提交审查请求、查看审查结果 | 营销/内容团队、法务团队 |
| 复核员 | 人工复核、确认/驳回/修改审查结果、编辑修订条款 | 合规审核人员 |
| 只读观察者 | 查看审查统计、导出报告 | 管理层 |

---

## 二、快速过滤器（原 Drools 层降级）

### 2.1 定位

快速过滤器是 LLM 审查之前的**轻量级预处理层**，用于拦截最明显的违规，减少 LLM 调用量。
在 v2 通用审查系统中，快速过滤器同时服务于营销材料和合同文档。

### 2.2 实现

```
┌─────────────────────────────────────────────────────────────────┐
│  QuickFilter — 快速过滤器                                        │
│                                                                  │
│  位置: LLM 审查之前                                               │
│  耗时: < 10ms                                                    │
│  实现: 纯 Java 代码, 无外部引擎依赖                                │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  检查1: 禁用词匹配 (→ 审查卡片②)                         │   │
│  │  引擎: Aho-Corasick 自动机                                │   │
│  │  输入: 内容全文 + 租户禁用词表 + 全局敏感词表              │   │
│  │  输出: 命中的禁用词列表 + 字符偏移量                       │   │
│  │                                                          │   │
│  │  命中 → 标记 issue (红线/敏感词违规), 继续后续检查          │   │
│  │  未命中 → 跳过                                            │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  检查2: 必备声明/必备元素检查 (→ 审查卡片③)               │   │
│  │  引擎: String.contains (精确) / Embedding 相似度 (语义)   │   │
│  │  输入: 内容全文 + 合规自检清单                             │   │
│  │  输出: 缺失的声明/元素列表                                │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  检查3: 格式/元信息校验                                   │   │
│  │  · 文件大小是否超限                                       │   │
│  │  · 文件格式是否支持                                       │   │
│  │  · 内容是否为空                                           │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  检查4: 文本基础核对 (→ 审查卡片①) [v2新增]              │   │
│  │  · 标点符号配对检查（引号、括号）                         │   │
│  │  · 常见错别字词典匹配                                     │   │
│  │  · 数字格式一致性（金额千分位、日期格式）                 │   │
│  │  耗时: < 2ms                                              │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  快速过滤结果:                                                    │
│  · 发现的问题归入对应审查卡片，合并到最终审查结果中                │
│  · 无论是否发现问题，都继续进入 LLM 深度审查                       │
│    (除非内容为空/格式不支持等前置错误)                              │
│                                                                  │
│  注意: Drools 不用。                                               │
│  禁用词匹配 → Aho-Corasick (O(n), 比 Drools 更快更简单)          │
│  必备声明 → String.contains / Embedding                           │
│  不存在复杂的条件推理链需求                                        │
└─────────────────────────────────────────────────────────────────┘
```

---

## 三、数据模型总览

### 3.1 核心实体关系

```
┌──────────────┐     ┌──────────────┐     ┌──────────────────┐
│   Tenant     │     │   SysUser    │     │   Role           │
│   (租户)     │────<│   (用户)     │>────│   (角色)         │
└──────┬───────┘     └──────────────┘     └──────────────────┘
       │
       │ 1:N
       ├───────────────────────────────────────────────┐
       │                                               │
       ▼                                               ▼
┌──────────────┐     ┌──────────────┐     ┌───────────────────────┐
│ CustomRule   │     │ ReviewTask   │     │ ContractTemplate      │
│ (自定义规则)  │     │ (审查任务)   │     │ (合同模板)            │
└──────────────┘     └──────┬───────┘     └──────────┬────────────┘
                            │                        │ 1:N
                            │ 1:N                    ▼
                            │              ┌───────────────────────┐
                            │              │ ContractTemplateClause│
                            │              │ (模板条款)            │
                            │              └───────────────────────┘
                            │
                     ┌──────┴───────┐
                     │ ReviewResult │     ┌──────────────────┐
                     │ (审查结果)   │────>│ HumanFeedback    │
                     └──────┬───────┘     │ (人工反馈)       │
                            │             └──────────────────┘
                            │
                     ┌──────┴───────┐
                     │ReviewCardResult│
                     │ (卡片汇总)     │
                     └───────────────┘
                            │
                            │ N:N
                            ▼
                     ┌──────────────┐
                     │ LawArticle   │     ┌──────────────────┐
                     │ (法条知识)   │────>│ LawSource        │
                     └──────────────┘     │ (法规来源)       │
                                          └──────────────────┘
```

### 3.2 全部表设计

---

#### 3.2.1 tenant（租户表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| tenant_name | VARCHAR(200) | 租户名称 |
| tenant_code | VARCHAR(50) UNIQUE | 租户编码 |
| industry | VARCHAR(100) | 所属行业（金融/法律/通用） |
| contact_email | VARCHAR(200) | 联系人邮箱 |
| plan_type | ENUM | free/standard/enterprise |
| max_users | INT | 最大用户数 |
| enabled | BOOLEAN | 是否启用 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

---

#### 3.2.2 sys_user（系统用户表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| tenant_id | BIGINT FK | 租户ID |
| username | VARCHAR(100) | 用户名 |
| email | VARCHAR(200) | 邮箱 |
| password_hash | VARCHAR(255) | 密码哈希 |
| role | ENUM | super_admin/tenant_admin/reviewer/auditor/observer |
| status | ENUM | active/disabled |
| last_login_at | TIMESTAMP | 最后登录时间 |
| created_at | TIMESTAMP | 创建时间 |

---

#### 3.2.3 law_source（法规来源表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| source_id | VARCHAR(50) UNIQUE | 全局唯一标识，如 `csrc-2024-001` |
| title | VARCHAR(500) | 法规全称 |
| issuer | VARCHAR(200) | 发布机构 |
| issue_date | DATE | 发布日期 |
| effective_date | DATE | 生效日期 |
| doc_type | ENUM | 法律/行政法规/部门规章/规范性文件/自律规则/监管问答 |
| status | ENUM | 现行有效/已修改/已废止 |
| supersedes | JSONB | 本法规替代的旧法规 source_id 列表 |
| full_text | TEXT | 法规全文 |
| source_url | VARCHAR(1000) | 原始来源URL |
| crawl_time | TIMESTAMP | 采集时间 |
| parse_status | ENUM | pending/parsing/parsed/confirmed/failed |

---

#### 3.2.4 law_article（法条知识表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| source_id | VARCHAR(50) FK | 法规来源ID |
| law_name | VARCHAR(500) | 法律名称 |
| article_id | VARCHAR(100) | 法条编号（如"第24条第1款第1项"） |
| original_text | TEXT | 法条原文 |
| norm_type | ENUM | 禁止/义务/权利/定义/程序 |
| subject | JSONB | 规范主体列表 |
| behavior | TEXT | 规范行为描述 |
| applicable_scenarios | JSONB | 适用场景 |
| applicable_content_types | JSONB | 适用内容类型 |
| applicable_product_types | JSONB | 适用产品类型 |
| key_phrases | JSONB | 关键词列表 |
| semantic_extensions | JSONB | 语义延伸 |
| violation_examples | JSONB | 违规示例 |
| compliant_examples | JSONB | 合规示例 |
| penalty | TEXT | 违反后果 |
| related_articles | JSONB | 关联法条 |
| authority_level | INT | 效力层级: 1=法律 2=行政法规 3=部门规章 4=规范性文件 5=自律规则 6=监管问答 |
| effective_date | DATE | 生效日期 |
| expiry_date | DATE | 失效日期（null=永久有效） |
| is_current | BOOLEAN | 是否现行有效版本 |
| status | ENUM | draft/pending_review/published/deprecated |
| confirmed_by | BIGINT FK | 确认人 |
| confirmed_at | TIMESTAMP | 确认时间 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

---

#### 3.2.5 contract_template（合同模板表）[v2 新增]

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| tenant_id | BIGINT FK | 租户ID（null=全局公共模板） |
| template_name | VARCHAR(300) | 模板名称，如"标准采购合同模板" |
| contract_type | VARCHAR(100) | 合同类型：purchase/sales/service/nda/employment/equity/lease/general |
| version | VARCHAR(20) | 版本号，如"v2.1" |
| clause_count | INT | 条款总数 |
| industry | VARCHAR(100) | 适用行业，如"金融/制造/通用" |
| full_text | TEXT | 模板合同全文 |
| status | ENUM | draft/published/deprecated |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

---

#### 3.2.6 contract_template_clause（合同模板条款表）[v2 新增]

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| template_id | BIGINT FK | 所属模板ID |
| clause_number | VARCHAR(50) | 条款编号，如"8.3"、"第八条第三款" |
| clause_title | VARCHAR(200) | 条款标题，如"违约责任" |
| clause_text | TEXT | 条款全文 |
| clause_type | ENUM | definition/obligation/right/liability/termination/dispute/general |
| is_required | BOOLEAN | 是否为必备条款（缺失时报告） |
| risk_level | ENUM | high/medium/low/info |
| annotation | TEXT | 标注说明（审查要点、常见风险提示） |
| sort_order | INT | 排序序号 |

---

#### 3.2.7 review_task（审查任务表）[v2 增强]

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| tenant_id | BIGINT FK | 租户ID |
| submitted_by | BIGINT FK | 提交人 |
| content_type | VARCHAR(100) | 内容类型（营销海报/微信推文/...） |
| **document_type** | VARCHAR(50) | **[v2新增]** 文档大类：marketing/contract/prospectus/report/other |
| product_type | VARCHAR(100) | 产品类型 |
| **contract_type** | VARCHAR(100) | **[v2新增]** 合同类型（仅 document_type=contract 时有值） |
| channel | VARCHAR(100) | 渠道 |
| original_content | TEXT | 原始内容（纯文本） |
| original_file_ref | VARCHAR(500) | 原始文件在对象存储中的引用 |
| parsed_segments | JSONB | 解析后的分段内容 |
| **clause_count** | INT | **[v2新增]** 合同条款数（仅合同） |
| **positioning_data** | JSONB | **[v2新增]** 字符级定位数据，结构见下方 |
| overall_verdict | ENUM | violation/compliant/needs_review |
| risk_score | INT | 风险评分 0-100 |
| risk_level | ENUM | high/medium/low |
| review_status | ENUM | pending/reviewing/completed/human_reviewed |
| llm_model | VARCHAR(100) | 使用的 LLM 模型 |
| llm_latency_ms | INT | LLM 推理耗时 |
| total_latency_ms | INT | 总耗时 |
| created_at | TIMESTAMP | 创建时间 |
| completed_at | TIMESTAMP | 完成时间 |

**positioning_data JSON 结构示例：**

```json
{
  "clauses": [
    {
      "clause_number": "8.3",
      "char_offset": 12450,
      "char_length": 356,
      "clause_title": "违约责任"
    }
  ],
  "definitions": [
    {
      "term": "关联方",
      "char_offset": 890,
      "char_length": 120,
      "definition_clause": "1.2"
    }
  ],
  "cross_references": [
    {
      "source_clause": "5.1",
      "target_clause": "8.3",
      "reference_text": "依照本合同第8.3条"
    }
  ]
}
```

---

#### 3.2.8 review_result（审查结果明细表）[v2 增强]

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| task_id | BIGINT FK | 审查任务ID |
| segment_index | INT | 段落序号 |
| original_text | TEXT | 段落原文 |
| verdict | ENUM | violation/compliant/needs_review |
| confidence | DECIMAL | LLM 置信度 |
| issue_type | VARCHAR(100) | 问题类型 |
| severity | ENUM | critical/major/minor/info |
| description | TEXT | 问题描述 |
| law_article_id | BIGINT FK | 引用的法条ID |
| citation_status | ENUM | verified/corrected/unverified |
| suggestion | TEXT | 修改建议 |
| **card_category** | INT | **[v2新增]** 审查卡片类别: 1=文本基础核对, 2=红线与敏感词, 3=形式与要素审查, 4=语义与合规审查, 5=逻辑与条款审查 |
| **location_text** | TEXT | **[v2新增]** 定位原文片段（exact_substring） |
| **char_offset** | INT | **[v2新增]** 在原始全文中的字符偏移 |
| **char_length** | INT | **[v2新增]** 定位文本长度 |
| **suggestion_type** | ENUM | **[v2新增]** tip/revision/negotiation_point |

---

#### 3.2.9 review_missing_element（缺失要素表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| task_id | BIGINT FK | 审查任务ID |
| element | VARCHAR(200) | 缺失元素名称 |
| requirement | TEXT | 法定要求描述 |
| law_article_id | BIGINT FK | 对应法条ID |
| severity_if_missing | ENUM | critical/major/minor |
| suggestion | TEXT | 补充建议 |

---

#### 3.2.10 human_feedback（人工反馈表）[v2 增强]

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| result_id | BIGINT FK | 审查结果明细ID |
| task_id | BIGINT FK | 审查任务ID |
| reviewer_id | BIGINT FK | 复核人 |
| action | ENUM | confirmed/rejected/modified/supplemented |
| original_verdict | ENUM | LLM 原始判定 |
| final_verdict | ENUM | 人工最终判定 |
| modified_severity | ENUM | 修改后的严重程度 |
| modified_reason | TEXT | 修改后的理由 |
| reject_reason | TEXT | 驳回理由 |
| supplement_issue | JSONB | 补充的问题 |
| **revised_clause_text** | TEXT | **[v2新增]** 人工修订后的条款全文（仅合同审查） |
| comment | TEXT | 复核备注 |
| is_typical_case | BOOLEAN | 是否标记为典型案例 |
| case_id | BIGINT FK | 沉淀的案例ID |
| reviewed_at | TIMESTAMP | 复核时间 |

---

#### 3.2.11 review_case（审核案例表）[v2 增强]

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| tenant_id | BIGINT FK | 租户ID |
| source | ENUM | internal_review/external_penalty/manual_import |
| content_type | VARCHAR(100) | 内容类型 |
| product_type | VARCHAR(100) | 产品类型 |
| **contract_type** | VARCHAR(100) | **[v2新增]** 合同类型 |
| channel | VARCHAR(100) | 渠道 |
| reviewed_content | TEXT | 被审查的内容 |
| **clause_id** | VARCHAR(50) | **[v2新增]** 关联条款编号（如"8.3"） |
| **clause_text** | TEXT | **[v2新增]** 关联条款全文 |
| verdict | ENUM | violation/compliant |
| severity | ENUM | critical/major/minor |
| reason | TEXT | 判定理由 |
| law_references | JSONB | 法条引用列表 |
| suggestion | TEXT | 修改建议 |
| ai_original_verdict | ENUM | LLM 原始判定 |
| human_action | ENUM | confirmed/rejected/modified/supplemented |
| human_reviewer_id | BIGINT FK | 人工复核人 |
| learning_value_score | INT | 学习价值分 |
| is_typical | BOOLEAN | 是否为典型案例 |
| is_shared | BOOLEAN | 是否跨租户共享 |
| created_at | TIMESTAMP | 创建时间 |

---

#### 3.2.12 tenant_custom_rule（租户自定义规则表）[v2 增强]

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| tenant_id | BIGINT FK | 租户ID |
| rule_type | ENUM | banned_word/required_statement/product_info/natural_language/**contract_clause_template** |
| rule_content | TEXT | 规则内容（禁用词/声明文本/自然语言规则/条款模板引用） |
| match_mode | ENUM | exact/fuzzy/semantic |
| applicable_content_types | JSONB | 适用内容类型 |
| applicable_contract_types | JSONB | **[v2新增]** 适用合同类型（仅 rule_type=contract_clause_template） |
| priority | INT | 优先级 |
| severity_if_violated | ENUM | critical/major/minor/info |
| enabled | BOOLEAN | 是否启用 |
| created_at | TIMESTAMP | 创建时间 |

---

#### 3.2.13 compliance_checklist（合规自检清单表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| content_type | VARCHAR(100) | 内容类型 |
| product_type | VARCHAR(100) | 产品类型（null=适用所有产品） |
| contract_type | VARCHAR(100) | 合同类型（null=不限） |
| check_item | VARCHAR(500) | 检查项描述 |
| check_method | ENUM | keyword/semantic/layout/conditional |
| law_article_id | BIGINT FK | 关联法条依据 |
| condition | VARCHAR(500) | 触发条件 |
| severity_if_missing | ENUM | critical/major/minor |
| enabled | BOOLEAN | 是否启用 |
| tenant_id | BIGINT FK | 租户级覆盖（null=全局） |

---

#### 3.2.14 llm_call_log（LLM 调用日志表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| task_id | BIGINT FK | 关联审查任务ID |
| call_type | ENUM | review/knowledge_parse/clause_parse/consistency_check |
| model_name | VARCHAR(100) | 使用的模型名称 |
| prompt_tokens | INT | 输入 token 数 |
| completion_tokens | INT | 输出 token 数 |
| total_tokens | INT | 总 token 数 |
| latency_ms | INT | 调用耗时(ms) |
| cache_hit | BOOLEAN | 是否命中 prompt 缓存 |
| status | ENUM | success/retry/failed |
| error_message | TEXT | 错误信息（如失败） |
| created_at | TIMESTAMP | 调用时间 |

---

#### 3.2.15 review_card_result（审查卡片汇总表）[v2 新增]

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| task_id | BIGINT FK | 审查任务ID |
| card_category | INT | 卡片类别: 1/2/3/4/5 |
| card_title | VARCHAR(100) | 卡片标题，如"文本基础核对"、"红线与敏感词" |
| total_issues | INT | 该卡片下的问题总数 |
| severity_summary | JSONB | 严重程度分布，如 `{"critical":1,"major":2,"minor":3,"info":0}` |
| status | ENUM | clean/has_issues/not_applicable |

**5 类审查卡片说明：**

```
┌──────┬──────────────────┬─────────────────────────────────────────┐
│ 类别 │ 名称              │ 说明                                    │
├──────┼──────────────────┼─────────────────────────────────────────┤
│  ①   │ 文本基础核对      │ 错别字、标点、格式、数字一致性           │
│  ②   │ 红线与敏感词      │ 禁用词、敏感词、行业黑名单               │
│  ③   │ 形式与要素审查    │ 必备元素、必备章节、声明缺失             │
│  ④   │ 语义与合规审查    │ 法规合规性审查（RAG+LLM）               │
│  ⑤   │ 逻辑与条款审查    │ 跨条款矛盾、权责不对称、一致性（合同）   │
└──────┴──────────────────┴─────────────────────────────────────────┘
```

> 卡片 ⑤ 仅在 document_type=contract 且文档包含多条款时启用。
> 卡片 ①②③ 对所有文档类型均适用。
> 卡片 ④ 对所有文档类型适用，但合同类型时会额外加载合同专属法条和案例。

---

## 四、非功能性需求

### 4.1 性能要求

> v2 依据 Gemini 评审建议，将延迟指标拆分为 p50/p95 两档，并基于 Gemini 2.5 Pro / GPT-4o 的实测数据给出更务实的预算。

| 指标 | p50 目标 | p95 目标 | 说明 |
|------|---------|---------|------|
| 单次审查延迟（短内容 <1000字） | < 8s | < 15s | 含 LLM 推理 |
| 单次审查延迟（中等内容 1000-5000字） | < 15s | < 25s | 分段并行 |
| 单次审查延迟（长文档/合同 >5000字） | < 30s | < 60s | 分 chunk 并行 + 一致性检查 |
| 快速过滤延迟 | < 5ms | < 10ms | 禁用词 + 必备声明 + 基础核对 |
| 五路检索延迟（并行） | < 400ms | < 1000ms | p95 含图谱查询（Phase 3+） |
| 合同条款解析延迟 | < 3s | < 8s | 单次 LLM 调用解析条款结构 |
| 并发审查能力 | 100+ QPS | — | LLM API 并发 |
| 知识库更新延迟 | < 5min | — | 从确认到可检索 |

### 4.2 可用性

| 指标 | 目标 |
|------|------|
| 系统可用性 | 99.9% |
| LLM 降级策略 | 主模型不可用时自动切换备用模型 |
| 数据备份 | 每日全量 + 实时增量（WAL 归档） |
| 灾难恢复 | RPO < 1h, RTO < 4h |

### 4.3 安全

| 维度 | 措施 |
|------|------|
| 数据传输 | HTTPS/TLS 1.3 |
| 数据存储 | 加密存储敏感内容（合同正文 AES-256） |
| 访问控制 | RBAC + 行级安全（RLS） |
| 审计日志 | 全操作审计日志, 保留 2 年 |
| LLM 数据安全 | 私有化部署或使用数据不出域的 API |
| 合同数据 | 合同正文和条款解析数据额外增加 tenant_id 断言校验 |

---

## 五、技术选型与部署架构

### 5.1 技术栈

| 层级 | 技术选型 | 说明 |
|------|----------|------|
| 后端框架 | Spring Boot 3.x | Java 21, 主业务框架 |
| LLM 集成 | Spring AI | 统一的 LLM 调用抽象 |
| **Agent 编排** | **Spring AI StateGraph / 自研 DAG 引擎** | **[v2新增]** 合同审查多步骤编排：条款解析→逐条审查→一致性检查→修订生成 |
| **Prompt 缓存层** | **Redis + 语义指纹** | **[v2新增]** 对高频相同/相似上下文的 LLM 调用做 prompt 级缓存，降低成本和延迟 |
| **模型路由层** | **自研 ModelRouter** | **[v2新增]** 按文档类型/复杂度/token预算动态选择模型：简单内容→4o-mini，复杂合同→Claude Sonnet/Gemini Pro |
| 向量数据库 | Milvus | 法条+案例+合同模板向量存储与检索 |
| 关系数据库 | PostgreSQL | 结构化数据存储, JSONB 支持, **递归 CTE 替代 Neo4j（Phase 1-2）** |
| 知识图谱 | **Neo4j（Phase 3 起）** | **[v2调整]** 法律实体关系存储，Phase 1-2 使用 PG 递归 CTE 替代 |
| 全文搜索 | Elasticsearch | 案例全文检索 + 法条关键词检索 |
| 消息队列 | RabbitMQ / Kafka | 异步任务处理 |
| 缓存 | Redis | 热点知识缓存, 会话管理, **prompt 缓存指纹** |
| 对象存储 | MinIO / OSS | 上传文件存储 |
| OCR | PaddleOCR / Tesseract | 图片文字识别 |
| 文档解析 | Apache Tika + 自定义解析器 | PDF/Word 解析 |
| Embedding | bge-large-zh-v1.5 / bge-m3 | 中文向量化模型 |
| LLM | Gemini 2.5 Pro / GPT-4o / Claude Sonnet / DeepSeek-V3 / Qwen-Max | 可配置多模型，通过 ModelRouter 动态选择 |
| 前端 | React + Ant Design | 管理后台 & 审查工作台 |

### 5.2 PG 递归 CTE 替代 Neo4j 的设计（Phase 1-2）

在 Phase 1-2 阶段，法条间的关联关系（REFERENCES、SUPERSEDED_BY）和合同内部条款图谱均使用 PostgreSQL 递归 CTE 实现：

```sql
-- 法条关联查询：给定法条，找所有关联法条（2跳以内）
WITH RECURSIVE related AS (
    SELECT id, article_id, law_name, related_articles, 0 AS depth
    FROM law_article
    WHERE id = :start_id
  UNION ALL
    SELECT la.id, la.article_id, la.law_name, la.related_articles, r.depth + 1
    FROM law_article la
    JOIN related r ON la.article_id = ANY(
        SELECT jsonb_array_elements_text(r.related_articles)
    )
    WHERE r.depth < 2
)
SELECT DISTINCT id, article_id, law_name FROM related;

-- 合同条款交叉引用图查询
WITH RECURSIVE ref_chain AS (
    SELECT source_clause, target_clause, 1 AS depth
    FROM contract_clause_reference
    WHERE source_clause = :clause_number AND task_id = :task_id
  UNION ALL
    SELECT cr.source_clause, cr.target_clause, rc.depth + 1
    FROM contract_clause_reference cr
    JOIN ref_chain rc ON cr.source_clause = rc.target_clause
    WHERE rc.depth < 3
)
SELECT * FROM ref_chain;
```

### 5.3 部署架构

```
┌─────────────────────────────────────────────────────────────┐
│  生产环境部署架构 (v2)                                        │
│                                                              │
│  ┌──────────┐    ┌──────────────────────────────┐           │
│  │  Nginx   │───>│  Spring Boot 集群 (3+ 实例)   │           │
│  │  网关    │    │  · 审查服务                    │           │
│  └──────────┘    │  · 知识管理服务                │           │
│                  │  · 用户/租户服务                │           │
│                  │  · 合同解析服务 [v2新增]        │           │
│                  │  · Agent 编排引擎 [v2新增]      │           │
│                  └──────────┬───────────────────┘           │
│                             │                                │
│         ┌───────────────────┼───────────────────┐           │
│         ▼                   ▼                   ▼           │
│  ┌──────────┐       ┌──────────┐       ┌──────────┐       │
│  │PostgreSQL│       │  Milvus  │       │  Neo4j   │       │
│  │(主从)    │       │  集群    │       │  (Phase3)│       │
│  └──────────┘       └──────────┘       └──────────┘       │
│                                                              │
│  ┌──────────┐       ┌──────────┐       ┌──────────┐       │
│  │  Redis   │       │   ES     │       │  MinIO   │       │
│  │  集群    │       │  集群    │       │  集群    │       │
│  └──────────┘       └──────────┘       └──────────┘       │
│                                                              │
│  ┌──────────┐       ┌────────────────────────────┐         │
│  │ RabbitMQ │       │ LLM API 网关 + 模型路由     │         │
│  │ 集群     │       │ · Prompt 缓存层              │         │
│  └──────────┘       │ · 模型选择器                  │         │
│                     │ · 成本/延迟监控               │         │
│                     └────────────────────────────┘         │
└─────────────────────────────────────────────────────────────┘
```

---

## 六、MVP 阶段划分（6 期）

### Phase 0: Manual RAG Test（2 周）

**目标: 在写任何代码前，手动验证 RAG + LLM 审查的可行性。**

| 任务 | 范围 |
|------|------|
| 准备测试集 | 50 条核心法条 + 50 条营销材料测试样本 + 20 份合同测试样本 |
| 手动模拟检索 | 对每个测试样本，人工挑选最相关的 5-10 条法条 |
| 组装 Prompt | 用设计文档的 prompt 模板手工组装 |
| 调用 LLM | 分别用 Gemini 2.5 Pro / GPT-4o 手动发 prompt |
| 评估 | 违规召回率 > 85%，法条引用正确率 > 80%，无严重误判 |
| 合同初探 | 对 20 份合同样本做条款解析 + 风险识别初步验证 |

> **通过标准：营销材料召回率 > 85%，合同风险点识别率 > 70%。不通过则优化法条格式/prompt 模板后重测。**

### Phase 1: 营销材料核心审查（卡片 ①②③④）

**目标: 跑通"上传营销内容 → LLM 审查 → 4 张卡片输出报告"的基本链路。**

| 模块 | 范围 |
|------|------|
| 知识构建 | 手动导入 100-200 条核心法条, LLM 解析 + 人工确认 |
| 内容解析 | 支持纯文本 + 图片(OCR) |
| ACE 上下文 | 实现向量检索(法条) + 结构化查询 + ES关键词检索 + 直接加载(企业规则) |
| LLM 审查 | 对接 Gemini 2.5 Pro 为主模型, GPT-4o 为备用; 结构化输出 |
| 审查卡片 | ① 文本基础核对 ② 红线与敏感词 ③ 形式与要素审查 ④ 语义与合规审查 |
| 后处理 | 法条 ID 回填（防幻觉）、风险评分、审查报告生成 |
| 前端 | 提交审查 + 4 卡片报告查看 + 基础复核工作台 |
| 自定义规则 | 禁用词 + 必备声明 |
| 黄金测试集 | 100 条样本，每周自动回归 |
| Prompt 缓存 | 基础缓存层上线，按法条上下文指纹缓存 |
| 模型路由 | 基础路由：短内容→4o-mini，长内容→主模型 |

### Phase 2: 合同审查 MVP（新增卡片 ⑤ + 条款解析 + Span 定位）

**目标: 跑通合同审查的完整链路，支持条款级审查和字符级定位。**

| 模块 | 范围 |
|------|------|
| 合同条款解析器 | 识别"第X条Y款Z项"层级、定义条款提取、交叉引用检测、甲乙方识别 |
| 合同内条款图谱 | PG 递归 CTE 实现：条款节点 + 引用/定义/依赖边 |
| 审查卡片⑤ | 逻辑与条款审查：权责对称性、争议解决自洽性、终止条款完整性 |
| 字符级定位 | LLM 输出 exact_substring → 后端严格子串搜索 → 模糊搜索 → 段落回退 |
| Agent 编排 | StateGraph：条款解析 → 逐条审查 → 一致性检查 → 修订建议 |
| 修订建议 | suggestion_type 三层分类：tip/revision/negotiation_point |
| PDF/Word 支持 | 内容解析支持 PDF/Word，合同条款结构化提取 |
| 合同模板库 | 预置 50-100 份行业标准合同模板 |

### Phase 3: 反馈闭环 + 案例沉淀

**目标: 跑通"审查 → 人工复核 → 案例沉淀 → 增强审查"闭环。**

| 模块 | 范围 |
|------|------|
| 人工复核 | 复核工作台（确认/驳回/修改/补充），合同支持修订条款文本编辑 |
| 案例沉淀 | 自动生成案例 + 向量化入库 + 学习价值分计算 |
| 案例生命周期 | 时间衰减 + 邻域采样 + 归档策略 |
| 案例检索 | ACE 上下文增加案例检索路径 |
| 进化监控 | 准确率统计仪表盘、黄金测试集自动回归 |
| Neo4j 上线 | 法律实体关系从 PG 迁移至 Neo4j，图谱检索路径上线 |

### Phase 4: 知识自动化 + 合同模板库

**目标: 法规自动采集解析、合同模板库规模化。**

| 模块 | 范围 |
|------|------|
| 法规采集 | 爬虫 + RSS + 商业 API，覆盖主要法规源 |
| 自动解析 | 批量 LLM 解析 + 人工确认工作台 |
| 法规更新 | 新旧法规关联 + 自动失效 + 关联案例降权 |
| 自然语言规则 | 企业自然语言规则支持 |
| 合同模板库扩展 | 500+ 标准合同模板，条款级标注 |
| 合同模板 Diff | 基于合同类型 + 向量相似度匹配最近模板 → 条款级 diff |

### Phase 5: 企业级增强（多租户、多模型、批量）

**目标: 完整多租户隔离、高可用、全内容类型支持。**

| 模块 | 范围 |
|------|------|
| 多租户 | 完整租户隔离 + 权限体系 + 数据加密 |
| 全内容类型 | 视频/直播/H5/音频等 |
| 多模型路由 | 完整 ModelRouter：成本感知、延迟感知、质量感知 |
| API 集成 | 开放审查 API，对接企业 CMS/OA/合同管理系统 |
| 批量审查 | 支持批量上传 + 异步处理 |
| Word/OnlyOffice 集成 | 合同审查结果直接标注在 Word 文档中 |

### Phase 6: 高级能力（知识图谱深化、多模态、API 市场）

**目标: 达到行业一梯队水平。**

| 模块 | 范围 |
|------|------|
| 知识图谱深化 | 法条-合同条款-案例的全链路图谱 |
| 多模态审查 | 视频/音频直接审查（不仅转文字） |
| API 市场 | 审查能力 API 化，第三方可接入 |
| 行业适配 | 保险、银行、证券各行业专属模板和法条库 |
| 智能问答 | 基于知识库的合规问答助手 |

---

## 附录：术语表

| 术语 | 说明 |
|------|------|
| ACE | Agentic Context Engineering, 每次审查动态组装最优上下文的架构模式 |
| RAG | Retrieval-Augmented Generation, 检索增强生成 |
| StateGraph | 有状态的 DAG 编排引擎，用于多步骤 Agent 协作（条款解析→审查→一致性检查→修订） |
| Prompt 缓存 | 对高频相同/相似 prompt 上下文做缓存，避免重复调用 LLM，降低成本和延迟 |
| 模型路由 | 根据任务复杂度、token 预算、延迟要求动态选择 LLM 模型的中间层 |
| Embedding | 将文本转换为高维向量表示，用于语义相似度计算 |
| 知识图谱 | 用图结构表示法律实体和关系的数据库 |
| 法条知识 | 从法律条文中结构化提取的知识单元 |
| 案例沉淀 | 将人工审核结果转化为可被系统学习的案例数据 |
| 防幻觉 | 防止 LLM 编造不存在的法律条文或虚假信息 |
| 快速过滤 | LLM 之前的轻量级规则匹配层 |
| 审查卡片 | 将审查结果按 5 个维度分类展示的 UI 组件 |
| exact_substring | LLM 输出的精确定位文本片段，用于字符级 span 定位 |
| 合同条款图谱 | 合同内部条款之间的引用、定义、依赖关系图 |
| 合同模板 Diff | 将待审合同与标准模板做条款级对齐和差异分析 |
| suggestion_type | 修改建议的三层分类：tip（纯提示）/ revision（可直接采纳的修订）/ negotiation_point（谈判要点） |
| RRF | Reciprocal Rank Fusion, 倒数排名融合算法，合并多路检索结果 |
| RLS | Row-Level Security, PostgreSQL 行级安全策略 |
| span | 文本中的字符级位置标记，由 char_offset + char_length 定义 |
