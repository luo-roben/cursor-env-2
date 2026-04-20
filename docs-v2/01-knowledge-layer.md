# 知识构建层详细设计文档 (v2)

> 模块: 第一层 — 知识构建层（"让机器读懂法律、理解模板、积累案例"）
>
> 版本: v2.0 | 所属系统: 通用智能审查平台
>
> 本文档在 v1（`docs/01-knowledge-layer.md`）基础上进行通用化重构，新增合同模板库、行业知识库、条款解析器、模板向量化策略等模块。v1 中已有且无变化的内容以引用方式标注，不再完整重复。

---

## 目录

- [1. 法律法规采集](#1-法律法规采集)
  - [1.1 数据来源（通用化扩展）](#11-数据来源通用化扩展)
  - [1.2 采集流水线](#12-采集流水线)
  - [1.3 法规来源元数据字段定义](#13-法规来源元数据字段定义)
- [2. LLM 结构化解析引擎](#2-llm-结构化解析引擎)
  - [2.1 四阶段解析流水线总览](#21-四阶段解析流水线总览)
  - [2.2 阶段 1：法条拆分](#22-阶段-1法条拆分)
  - [2.3 阶段 2：语义结构化](#23-阶段-2语义结构化)
  - [2.4 阶段 3：向量化与索引](#24-阶段-3向量化与索引)
  - [2.5 阶段 4：知识图谱构建（Phase 3 延后）](#25-阶段-4知识图谱构建phase-3-延后)
- [3. 合同模板库（v2 新增）](#3-合同模板库v2-新增)
  - [3.1 模板库定位与价值](#31-模板库定位与价值)
  - [3.2 模板来源](#32-模板来源)
  - [3.3 模板数据结构](#33-模板数据结构)
  - [3.4 条款级标注规范](#34-条款级标注规范)
  - [3.5 模板匹配策略](#35-模板匹配策略)
  - [3.6 模板管理工作台](#36-模板管理工作台)
- [4. 行业知识库（v2 新增）](#4-行业知识库v2-新增)
  - [4.1 行业知识的定义](#41-行业知识的定义)
  - [4.2 行业知识数据结构](#42-行业知识数据结构)
  - [4.3 行业知识的来源与维护](#43-行业知识的来源与维护)
- [5. 合同条款解析器（v2 新增）](#5-合同条款解析器v2-新增)
  - [5.1 条款层级识别](#51-条款层级识别)
  - [5.2 定义条款抽取](#52-定义条款抽取)
  - [5.3 交叉引用检测](#53-交叉引用检测)
  - [5.4 条款解析输出结构](#54-条款解析输出结构)
- [6. 多模态知识存储](#6-多模态知识存储)
  - [6.1 存储架构总览](#61-存储架构总览)
  - [6.2 PostgreSQL：结构化存储](#62-postgresql结构化存储)
  - [6.3 Milvus：向量存储](#63-milvus向量存储)
  - [6.4 Elasticsearch：全文检索](#64-elasticsearch全文检索)
  - [6.5 Neo4j：知识图谱（Phase 3）](#65-neo4jphase-3)
- [7. 模板向量化策略（v2 新增）](#7-模板向量化策略v2-新增)
  - [7.1 条款级 Embedding](#71-条款级-embedding)
  - [7.2 模板匹配的向量检索流程](#72-模板匹配的向量检索流程)
  - [7.3 Milvus Collection Schema（模板库）](#73-milvus-collection-schema模板库)
- [8. 人工确认工作台](#8-人工确认工作台)
  - [8.1 工作台 UI 原型（通用化）](#81-工作台-ui-原型通用化)
  - [8.2 确认操作与系统行为](#82-确认操作与系统行为)
- [9. 知识规模估算（扩展版）](#9-知识规模估算扩展版)
  - [9.1 法规知识规模](#91-法规知识规模)
  - [9.2 合同模板库规模](#92-合同模板库规模)
  - [9.3 行业知识库规模](#93-行业知识库规模)
  - [9.4 向量库总规模](#94-向量库总规模)
  - [9.5 解析工作量估算（扩展版）](#95-解析工作量估算扩展版)
- [10. LLM 调用方式详解](#10-llm-调用方式详解)

---

## 1. 法律法规采集

### 1.1 数据来源（通用化扩展）

v2 在 v1 的金融法规基础上，扩展了合同法、民商法等通用法律来源：

| 来源分类 | 具体来源 | 采集方式 | 更新频率 | v2 新增 |
|----------|----------|----------|----------|---------|
| 国家法律 | 全国人大法规库、国家法律法规数据库 | 爬虫 + RSS | 实时监控 | — |
| 部门规章 | 证监会、银保监会、人民银行、住建部、人社部 | 爬虫 + API | 每日检查 | 住建部/人社部 |
| 行业自律 | 基金业协会、证券业协会、律师协会 | 爬虫 | 每日检查 | 律师协会 |
| 监管问答 | 证监会监管问答、劳动仲裁解释 | 人工导入 + 爬虫 | 发布即采集 | 劳动仲裁 |
| **合同相关法律** | 《民法典》合同编、《劳动合同法》、《公司法》 | 商业数据库 API | 按修订更新 | **v2 新增** |
| **司法解释** | 最高法合同纠纷解释、劳动争议解释 | 爬虫 + 人工导入 | 发布即采集 | **v2 新增** |
| **行业示范合同** | 工商总局示范文本、行业协会范本 | 人工导入 | 按需 | **v2 新增** |
| 处罚公告 | 各地监管局 | 爬虫 | 每日检查 | — |
| 商业数据库 | 北大法宝、威科先行 | API 对接 | 按合同更新 | — |
| 内部制度 | 企业自有合规制度、合同模板 | 人工上传 | 按需 | — |

### 1.2 采集流水线

> 同 v1（`docs/01-knowledge-layer.md` 1.2 节），流水线结构不变：源站监控 → 增量抓取 → 去重判断 → 入库排队。

### 1.3 法规来源元数据字段定义

> 同 v1（`docs/01-knowledge-layer.md` 1.3 节），JSON 结构和字段定义不变。v2 增加一个字段：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `applicable_doc_types` | String[] | 否 | 该法规适用的文档类型列表，如 `["contract", "marketing", "prospectus"]`。由 LLM 在结构化解析阶段自动推断，人工可修改。用于在 ACE 检索时按文档类型预过滤。 |

---

## 2. LLM 结构化解析引擎

### 2.1 四阶段解析流水线总览

> 总体架构同 v1。v2 的变更体现在阶段 2（语义结构化）增加了合同相关的字段和 `applicable_doc_types`。

```
法规原文 / 合同模板 / 行业知识
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│  阶段1: 法条拆分 / 条款拆分                               │
│  法规按"条-款-项"拆分；合同模板按条款层级拆分               │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│  阶段2: 语义结构化                                        │
│  LLM 提取结构化信息 → 输出 JSON                           │
│  v2 增加: applicable_doc_types, clause_role, clause_id   │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│  阶段3: 向量化与索引                                      │
│  结构化知识 → Embedding → Milvus                          │
│  v2 增加: 条款级 Embedding（模板库专用）                    │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│  阶段4: 知识图谱构建 (Phase 3 延后)                        │
│  抽取实体与关系 → Neo4j                                    │
│  v2 增加: 合同内条款关系图谱                                │
└─────────────────────────────────────────────────────────┘
```

### 2.2 阶段 1：法条拆分

> 同 v1（`docs/01-knowledge-layer.md` 2.2 节），拆分规则不变。

### 2.3 阶段 2：语义结构化

> 核心字段同 v1（`docs/01-knowledge-layer.md` 2.3 节）。v2 在 `structured` 字段中新增以下字段：

#### 2.3.1 v2 新增字段

| 字段 | 类型 | 说明 | 是否 LLM 生成 |
|------|------|------|-------------|
| `structured.applicable_doc_types` | String[] | 适用的文档类型，如 `["contract", "marketing"]`。LLM 推断该法条在审查哪些文档类型时应被检索。 | 是 |
| `structured.contract_clause_role` | Enum | 仅当 `applicable_doc_types` 含 `contract` 时有效。标注该法条约束的条款角色：`definition` / `rights` / `obligations` / `liability` / `dispute_resolution` / `general` / `termination`。 | 是 |
| `structured.contract_types` | String[] | 适用的合同类型，如 `["labor_contract", "sales_contract"]`。 | 是 |

#### 2.3.2 v2 完整 JSON 输出示例（以民法典合同编为例）

```json
{
  "source_ref": {
    "law_name": "中华人民共和国民法典",
    "law_short_name": "民法典",
    "issuer": "全国人民代表大会",
    "article_id": "第五百七十七条",
    "original_text": "当事人一方不履行合同义务或者履行合同义务不符合约定的，应当承担继续履行、采取补救措施或者赔偿损失等违约责任。"
  },
  "structured": {
    "norm_type": "义务",
    "subject": ["合同当事人"],
    "behavior": "不履行合同义务或履行不符合约定时，承担违约责任",
    "object": "合同义务",
    "condition": "不履行或履行不当",
    "applicable_scenarios": ["合同违约", "违约责任认定", "合同纠纷"],
    "applicable_content_types": ["合同正文", "合同条款审查"],
    "applicable_product_types": ["通用"],
    "applicable_doc_types": ["contract"],
    "contract_clause_role": "liability",
    "contract_types": ["sales_contract", "service_contract", "labor_contract", "lease_contract"],
    "key_phrases": ["不履行", "不符合约定", "继续履行", "补救措施", "赔偿损失", "违约责任"],
    "semantic_extensions": [
      "部分履行也构成不完全履行",
      "迟延履行属于履行不符合约定",
      "违约责任不以过错为要件（严格责任）",
      "违约方可主张不可抗力免责"
    ],
    "violation_examples": [
      "合同约定'乙方不承担任何违约责任'（免除己方全部违约责任）",
      "合同约定'甲方违约仅需退还已收款项'（排除赔偿损失责任）",
      "合同约定'乙方迟延交付不视为违约'（排除迟延履行责任）"
    ],
    "compliant_examples": [
      "任何一方违反本合同约定的，应依据《民法典》第五百七十七条承担违约责任，包括但不限于继续履行、赔偿损失等",
      "乙方未按期交付的，每迟延一日应向甲方支付合同总价千分之一的违约金"
    ],
    "penalty": "按民法典第五百八十四条，赔偿损失额为因违约所造成的损失，包括合同履行后可以获得的利益",
    "related_articles": ["第五百七十八条", "第五百八十四条", "第五百八十五条"]
  }
}
```

### 2.4 阶段 3：向量化与索引

> 技术方案同 v1（`docs/01-knowledge-layer.md` 2.4 节）。v2 增加了合同模板条款的向量化（见第 7 节）。

法条知识的多粒度 Embedding 策略保持不变：

| 向量类型 | 向量化文本来源 | 用途 |
|---------|--------------|------|
| 法条原文 Embedding | `source_ref.original_text` | 精确法条检索 |
| 行为描述 Embedding | `structured.behavior` + `structured.condition` | 审查时语义匹配 |
| 违规示例 Embedding | `structured.violation_examples` 每条 | 与待审查内容匹配 |
| 语义延伸 Embedding | `structured.semantic_extensions` 每条 | 捕捉变体表述 |

### 2.5 阶段 4：知识图谱构建（Phase 3 延后）

> 按 Gemini 评估建议，Neo4j 延后到 Phase 3。Phase 1-2 用 PostgreSQL 递归 CTE 和 JSONB 表达法条/条款关系。

Phase 3 引入后的图谱实体/关系设计同 v1（`docs/01-knowledge-layer.md` 2.5 节），v2 新增以下实体和关系：

| 实体/关系 | 类型 | 说明 |
|----------|------|------|
| `ContractTemplate` | 实体 | 标准合同模板 |
| `TemplateClause` | 实体 | 模板中的一个条款 |
| `TEMPLATE_CONTAINS` | 关系 | 模板包含条款 |
| `CLAUSE_REFERENCES` | 关系 | 条款间交叉引用 |
| `GOVERNS` | 关系 | 法条约束合同条款角色 |

---

## 3. 合同模板库（v2 新增）

### 3.1 模板库定位与价值

```
┌──────────────────────────────────────────────────────────────────────┐
│  合同模板库 — 核心定位                                                  │
│                                                                      │
│  "模板库是合同审查的第二知识底座，与法规知识库并列。"                     │
│                                                                      │
│  法规知识库回答的问题:                                                  │
│  · "这个条款是否违反法律规定？"                                         │
│  · "这个表述是否构成违规？"                                             │
│                                                                      │
│  模板库回答的问题:                                                      │
│  · "这份合同缺了哪些标准条款？"                                         │
│  · "这个条款与行业标准相比偏离了多少？"                                  │
│  · "类似的合同通常怎么写这个条款？"                                      │
│                                                                      │
│  两者配合:                                                              │
│  · 法规 → 判定合法性（是否违法）                                        │
│  · 模板 → 判定合理性（是否偏离行业惯例）                                 │
│  · 组合审查 → 同时发现"违法"和"不合理"的条款                             │
└──────────────────────────────────────────────────────────────────────┘
```

### 3.2 模板来源

| 来源 | 说明 | 入库方式 | 模板数量（初始） |
|------|------|---------|--------------|
| 工商总局示范文本 | 国家市场监管总局发布的标准合同示范文本 | 人工导入 + LLM 解析 | ~50 份 |
| 行业协会范本 | 各行业协会（房地产、IT、金融等）发布的标准合同 | 人工导入 + LLM 解析 | ~80 份 |
| 企业标准模板 | 企业内部法务部门维护的标准合同模板 | 租户上传 + LLM 解析 | 每租户 20-50 份 |
| 审查沉淀模板 | 经过多次审查且质量高的合同脱敏后入库 | 系统自动推荐 + 人工确认 | 逐步积累 |
| 商业数据库 | 北大法宝合同范本库、威科先行 | API 对接 | ~100 份 |

### 3.3 模板数据结构

每份合同模板以 JSON 结构存储：

```json
{
  "template_id": "TPL-2026-SALES-001",
  "template_name": "商品销售合同（标准版）",
  "contract_type": "sales_contract",
  "industry": "通用",
  "source": "national_standard",
  "source_name": "国家市场监管总局合同示范文本",
  "version": "2024-01",
  "status": "published",
  "applicable_scenarios": ["商品买卖", "设备采购", "原材料采购"],

  "metadata": {
    "total_clauses": 28,
    "total_chars": 8500,
    "parties": ["甲方（买方）", "乙方（卖方）"],
    "created_at": "2026-01-15T10:00:00Z",
    "confirmed_by": "admin-legal-001",
    "tenant_id": null
  },

  "clauses": [
    {
      "clause_id": "1",
      "clause_title": "商品名称、规格、数量",
      "clause_role": "definition",
      "clause_level": 1,
      "parent_clause_id": null,
      "clause_text": "甲方向乙方购买以下商品：（详见附件一《商品清单》）",
      "is_required": true,
      "risk_level": "low",
      "annotations": []
    },
    {
      "clause_id": "7",
      "clause_title": "违约责任",
      "clause_role": "liability",
      "clause_level": 1,
      "parent_clause_id": null,
      "clause_text": "任何一方违反本合同约定的，应向守约方支付合同总价__%的违约金。违约金不足以弥补实际损失的，违约方应补足差额。",
      "is_required": true,
      "risk_level": "high",
      "annotations": [
        {
          "annotation_type": "risk_pattern",
          "description": "违约金比例留空需双方协商确定，审查时应检查是否已填写",
          "related_law": "民法典第五百八十五条"
        },
        {
          "annotation_type": "best_practice",
          "description": "违约金比例通常为合同总价的10%-30%，超过30%可能被法院调低",
          "source": "最高法合同纠纷案件解释"
        }
      ]
    },
    {
      "clause_id": "7.1",
      "clause_title": "甲方违约",
      "clause_role": "liability",
      "clause_level": 2,
      "parent_clause_id": "7",
      "clause_text": "甲方逾期付款的，每逾期一日应向乙方支付应付金额万分之__的逾期利息。",
      "is_required": true,
      "risk_level": "medium",
      "annotations": [
        {
          "annotation_type": "symmetry_check",
          "description": "需检查乙方迟延交货是否有对等的违约条款",
          "counterpart_clause": "7.2"
        }
      ]
    }
  ],

  "required_clause_roles": [
    "definition", "rights", "obligations", "liability",
    "dispute_resolution", "termination"
  ]
}
```

### 3.4 条款级标注规范

每个条款必须标注以下维度：

| 标注维度 | 字段 | 类型 | 说明 |
|---------|------|------|------|
| 条款编号 | `clause_id` | String | 按层级编号：`1`, `1.1`, `1.1.1` |
| 条款标题 | `clause_title` | String | 条款的标题或概括性名称 |
| 条款角色 | `clause_role` | Enum | 见下表 |
| 层级深度 | `clause_level` | Int | 1 = 条，2 = 款，3 = 项 |
| 父条款 | `parent_clause_id` | String | 父级条款编号，顶级为 null |
| 条款原文 | `clause_text` | String | 条款完整文本 |
| 是否必备 | `is_required` | Boolean | 该条款是否为该类型合同的必备条款 |
| 风险等级 | `risk_level` | Enum | `high` / `medium` / `low` |
| 标注信息 | `annotations` | Object[] | 风险模式、最佳实践、对称性检查等标注 |

#### 条款角色 (clause_role) 枚举

| 角色 | 英文标识 | 说明 | 典型条款 |
|------|---------|------|---------|
| 定义条款 | `definition` | 约定术语含义、适用范围 | "本合同所称'关联方'指……" |
| 权利条款 | `rights` | 约定当事人的权利 | "甲方有权检查乙方的生产过程" |
| 义务条款 | `obligations` | 约定当事人的义务 | "乙方应当在约定期限内交付商品" |
| 违约条款 | `liability` | 约定违约责任及赔偿 | "违约方应支付合同总价10%的违约金" |
| 争议解决 | `dispute_resolution` | 约定争议解决方式 | "因本合同引起的争议，提交XX仲裁委员会仲裁" |
| 终止条款 | `termination` | 约定合同解除/终止条件 | "任何一方有权提前30日书面通知解除本合同" |
| 一般条款 | `general` | 通知方式、保密、不可抗力等 | "不可抗力导致的迟延履行不视为违约" |
| 标的条款 | `subject_matter` | 约定交易标的物/服务内容 | "甲方向乙方购买以下商品" |
| 价款条款 | `payment` | 约定价款、支付方式和时间 | "合同总价为人民币__元" |

#### 标注类型 (annotation_type)

| 类型 | 说明 | 示例 |
|------|------|------|
| `risk_pattern` | 已知的风险模式 | "违约金留空需检查是否已填写" |
| `best_practice` | 行业最佳实践 | "违约金通常为合同总价10%-30%" |
| `symmetry_check` | 对称性检查标注 | "需检查对方是否有对等条款" |
| `legal_reference` | 关联的法律依据 | "参照民法典第585条" |
| `common_trap` | 常见陷阱 | "仲裁条款约定'双方协商不成可诉讼'将导致仲裁条款无效" |

### 3.5 模板匹配策略

审查合同时，系统需要找到与待审合同最匹配的标准模板，用于条款级对比。

```
┌──────────────────────────────────────────────────────────────────────┐
│  模板匹配流程                                                          │
│                                                                      │
│  Step 1: 按合同类型精确匹配                                            │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │  待审合同 doc_type = "sales_contract"                          │ │
│  │  → SELECT * FROM contract_templates                           │ │
│  │    WHERE contract_type = 'sales_contract'                     │ │
│  │    AND status = 'published'                                   │ │
│  │  → 候选模板: 5 份销售合同模板                                   │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  Step 2: 按行业细分过滤                                               │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │  待审合同 industry = "IT/互联网"                                │ │
│  │  → 过滤: industry IN ('IT/互联网', '通用')                     │ │
│  │  → 候选模板: 3 份                                              │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  Step 3: 按条款级向量相似度排序                                        │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │  待审合同全文 → Embedding → 与候选模板全文 Embedding 计算余弦    │ │
│  │  排序: 相似度最高的模板排第一                                    │ │
│  │  → 最佳匹配: TPL-2026-SALES-003 (IT设备采购合同, 相似度 0.89)  │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  Step 4: 条款级 Diff                                                  │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │  最佳匹配模板 vs 待审合同，按 clause_role 逐条比对:              │ │
│  │                                                                │ │
│  │  模板条款 7 (违约责任)  ←→ 待审合同第 9 条 (违约责任)           │ │
│  │    → 匹配方式: 条款标题语义相似度 + clause_role 相同             │ │
│  │    → 对比内容: 是否缺失关键要素、是否偏离行业惯例                 │ │
│  │                                                                │ │
│  │  模板条款 8 (争议解决)  ←→ 待审合同: 未找到对应条款 ❌           │ │
│  │    → 标记: 缺失必备条款 "争议解决"                               │ │
│  └────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
```

### 3.6 模板管理工作台

```
┌─────────────────────────────────────────────────────────────────────┐
│  合同模板管理工作台                                                    │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │  模板总数: 156  │  已发布: 142  │  待审核: 11  │  草稿: 3  │      │
│  └──────────────────────────────────────────────────────────┘      │
│                                                                     │
│  ┌──────────────────────┐  ┌─────────────────────────────────┐    │
│  │  模板列表 (按类型)    │  │  条款详情                        │    │
│  │                       │  │                                 │    │
│  │  📁 销售合同 (23)     │  │  模板: IT设备采购合同 v2024      │    │
│  │    └ IT设备采购  [✓]  │  │                                 │    │
│  │    └ 原材料采购  [✓]  │  │  条款 7: 违约责任 [liability]   │    │
│  │    └ 食品销售    [ ]  │  │  层级: 1 | 必备: 是 | 风险: 高   │    │
│  │  📁 服务合同 (18)     │  │                                 │    │
│  │  📁 劳动合同 (15)     │  │  条款原文:                       │    │
│  │  📁 保密协议 (12)     │  │  "任何一方违反本合同..."         │    │
│  │  📁 股权合同 (8)      │  │                                 │    │
│  │  📁 租赁合同 (10)     │  │  标注:                          │    │
│  │  📁 金融合同 (14)     │  │  [风险] 违约金比例需确认 [✓]    │    │
│  │                       │  │  [实践] 通常10%-30% [✓]         │    │
│  │                       │  │  [对称] 检查 7.2 对等条款 [✓]   │    │
│  │                       │  │  [+ 添加标注]                   │    │
│  └──────────────────────┘  └─────────────────────────────────┘    │
│                                                                     │
│  操作: [✅ 发布模板]  [📝 编辑条款]  [📋 复制模板]  [🗑 归档]       │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 4. 行业知识库（v2 新增）

### 4.1 行业知识的定义

行业知识是介于"法律法规"和"案例经验"之间的知识层，包含：

```
┌──────────────────────────────────────────────────────────────────────┐
│  行业知识的三个维度                                                     │
│                                                                      │
│  维度1: 行业合同规范                                                   │
│  · "IT外包合同中，知识产权归属条款必须明确区分项目成果和预有知产"         │
│  · "建筑工程合同中，工程变更条款应约定变更审批流程和费用调整机制"         │
│                                                                      │
│  维度2: 审查最佳实践                                                   │
│  · "违约金超过合同总价30%时应标记为高风险——法院大概率会调低"              │
│  · "仲裁条款不可同时约定仲裁和诉讼，否则仲裁条款无效"                    │
│  · "竞业限制条款必须约定经济补偿，否则劳动者不受竞业限制约束"             │
│                                                                      │
│  维度3: 常见风险模式                                                   │
│  · "格式合同中，加重对方责任的条款可能因未尽提示义务而被认定无效"         │
│  · "不可抗力条款中列举'政策变化'通常不被法院认定为不可抗力"               │
└──────────────────────────────────────────────────────────────────────┘
```

### 4.2 行业知识数据结构

```json
{
  "knowledge_id": "IK-2026-CONTRACT-001",
  "knowledge_type": "best_practice",
  "industry": "通用",
  "contract_types": ["sales_contract", "service_contract"],
  "applicable_clause_roles": ["liability"],
  "title": "违约金比例的合理区间",
  "content": "根据《民法典》第585条及最高法司法解释，约定的违约金超过造成损失的30%的，一般认定为'过分高于造成的损失'，当事人可请求法院或仲裁机构予以适当减少。因此，合同审查时如发现违约金比例超过合同总价30%，应标记为高风险并建议调整。",
  "risk_level": "high",
  "law_references": [
    {
      "article_id": "第五百八十五条",
      "law_name": "民法典"
    }
  ],
  "source": "最高人民法院关于适用《中华人民共和国合同法》若干问题的解释(二)",
  "tags": ["违约金", "损害赔偿", "司法调整"],
  "status": "published",
  "created_at": "2026-01-20T09:00:00Z",
  "confirmed_by": "admin-legal-002"
}
```

### 4.3 行业知识的来源与维护

| 来源 | 维护方式 | 说明 |
|------|---------|------|
| 司法解释与指导案例 | LLM 解析 + 人工确认 | 从司法解释中提取实操性的审查规则 |
| 法律实务文章 | 人工整理 + LLM 辅助结构化 | 从法律期刊、律所研报中提取最佳实践 |
| 审查经验沉淀 | 系统自动推荐 + 人工确认 | 当同一类问题被重复审出且人工确认率高，自动提议沉淀为行业知识 |
| 企业法务团队 | 人工上传 | 企业特有的审查经验（如"我司不接受仲裁条款指定对方所在地仲裁机构"） |

---

## 5. 合同条款解析器（v2 新增）

### 5.1 条款层级识别

合同条款解析器是审查执行层的前置模块，负责将合同原文解析为结构化的条款树。

```
┌──────────────────────────────────────────────────────────────────────┐
│  合同条款解析器 — 输入与输出                                            │
│                                                                      │
│  输入: 合同全文（纯文本）                                               │
│                                                                      │
│  "第一条 商品名称与规格                                                 │
│   甲方向乙方购买以下商品：（详见附件一）                                  │
│   第二条 质量标准                                                      │
│   2.1 商品应符合国家标准GB/T XXXXX的规定。                               │
│   2.2 乙方应提供商品的质量检验报告。                                     │
│   ..."                                                                │
│                                                                      │
│  处理: LLM 识别条款层级结构                                             │
│                                                                      │
│  输出: 条款树                                                          │
│  ┌─ 第一条 [subject_matter, level=1]                                  │
│  │   └─ (正文) [level=2]                                              │
│  ├─ 第二条 [obligations, level=1]                                     │
│  │   ├─ 2.1 [obligations, level=2]                                    │
│  │   └─ 2.2 [obligations, level=2]                                    │
│  ├─ 第三条 [payment, level=1]                                         │
│  │   ├─ 3.1 [payment, level=2]                                       │
│  │   └─ 3.2 [payment, level=2]                                       │
│  ...                                                                  │
│  └─ 第十二条 [dispute_resolution, level=1]                             │
└──────────────────────────────────────────────────────────────────────┘
```

**条款识别规则（LLM Prompt 要点）：**

```
请将以下合同文本解析为条款层级结构。

识别规则:
1. "第X条" / "第X章" 为一级条款 (level=1)
2. "X.Y" / "(X)" / "第Y款" 为二级条款 (level=2)
3. "X.Y.Z" / "(一)(二)(三)" / "第Z项" 为三级条款 (level=3)
4. 对每个条款判断其角色 (clause_role):
   definition / rights / obligations / liability /
   dispute_resolution / termination / general /
   subject_matter / payment
5. 保留每个条款的完整原文，不改写
6. 记录每个条款在原文中的字符偏移位置 (char_offset_start, char_offset_end)
```

### 5.2 定义条款抽取

定义条款是合同中约定术语含义的特殊条款，对跨条款一致性检查至关重要。

```
┌──────────────────────────────────────────────────────────────────────┐
│  定义条款抽取                                                          │
│                                                                      │
│  输入: 条款树中 clause_role = "definition" 的条款                       │
│                                                                      │
│  示例原文:                                                             │
│  "第一条 定义与解释                                                     │
│   1.1 '关联方'指直接或间接控制一方、受一方控制、                          │
│       或与一方受同一主体控制的任何法人或自然人。                           │
│   1.2 '保密信息'指一方向另一方披露的任何非公开信息，                      │
│       包括但不限于技术资料、商业计划、客户名单等。"                        │
│                                                                      │
│  输出: 定义表                                                          │
│  ┌───────────────┬──────────────────────────────────┬────────────┐  │
│  │ 术语           │ 定义                              │ 出现位置   │  │
│  ├───────────────┼──────────────────────────────────┼────────────┤  │
│  │ 关联方         │ 直接或间接控制一方、受一方控制...  │ 1.1       │  │
│  │ 保密信息       │ 一方向另一方披露的任何非公开信息... │ 1.2       │  │
│  └───────────────┴──────────────────────────────────┴────────────┘  │
│                                                                      │
│  用途: 在逻辑条款 Agent (卡片⑤) 中检查:                                │
│  · 后续条款使用的术语是否都有定义                                        │
│  · 术语的使用是否与定义一致                                              │
│  · 定义是否存在循环引用                                                  │
└──────────────────────────────────────────────────────────────────────┘
```

### 5.3 交叉引用检测

```
┌──────────────────────────────────────────────────────────────────────┐
│  交叉引用检测                                                          │
│                                                                      │
│  扫描所有条款文本，用正则 + LLM 识别引用关系:                             │
│                                                                      │
│  正则匹配模式:                                                         │
│  · "依照本合同第X条" / "按照第X.Y款的规定" / "参见第X条"                 │
│  · "根据上述第X条" / "如第X条所述"                                      │
│                                                                      │
│  示例:                                                                 │
│  第 9.2 条: "乙方违反第5条保密义务的，应按第7条违约条款承担违约责任"       │
│                                                                      │
│  检测输出:                                                              │
│  {                                                                    │
│    "source_clause": "9.2",                                            │
│    "references": [                                                    │
│      {"target_clause": "5", "ref_type": "condition"},                 │
│      {"target_clause": "7", "ref_type": "consequence"}                │
│    ]                                                                  │
│  }                                                                    │
│                                                                      │
│  用途:                                                                 │
│  · 检测悬挂引用: 引用了不存在的条款（如"第13条"但合同只有12条）           │
│  · 检测循环引用: A引用B, B引用A                                         │
│  · 为逻辑条款 Agent 提供条款关系图                                       │
└──────────────────────────────────────────────────────────────────────┘
```

### 5.4 条款解析输出结构

条款解析器的完整输出作为后续所有 Agent 的共享状态：

```json
{
  "parse_result": {
    "doc_type": "sales_contract",
    "total_clauses": 35,
    "total_chars": 12800,
    "parties": ["甲方（XX科技有限公司）", "乙方（YY设备有限公司）"],

    "clause_tree": [
      {
        "clause_id": "1",
        "clause_title": "定义与解释",
        "clause_role": "definition",
        "clause_level": 1,
        "clause_text": "...",
        "char_offset_start": 45,
        "char_offset_end": 892,
        "children": [
          {
            "clause_id": "1.1",
            "clause_title": null,
            "clause_role": "definition",
            "clause_level": 2,
            "clause_text": "'关联方'指...",
            "char_offset_start": 68,
            "char_offset_end": 215
          }
        ]
      }
    ],

    "definitions": [
      {
        "term": "关联方",
        "definition": "直接或间接控制一方、受一方控制...",
        "defined_in": "1.1",
        "used_in": ["5.3", "8.1", "9.2"]
      }
    ],

    "cross_references": [
      {
        "source_clause": "9.2",
        "target_clause": "5",
        "ref_type": "condition",
        "ref_text": "违反第5条保密义务",
        "target_exists": true
      },
      {
        "source_clause": "10.1",
        "target_clause": "13",
        "ref_type": "reference",
        "ref_text": "按照第13条的规定",
        "target_exists": false
      }
    ],

    "missing_required_roles": ["dispute_resolution"]
  }
}
```

---

## 6. 多模态知识存储

### 6.1 存储架构总览

```
┌──────────────────────────────────────────────────────────────────────┐
│  多模态知识存储架构 (v2)                                                │
│                                                                      │
│  ┌────────────────────┐  ┌────────────────────┐                     │
│  │  PostgreSQL         │  │  Milvus             │                     │
│  │  (结构化存储)       │  │  (向量存储)          │                     │
│  │                    │  │                     │                     │
│  │  · law_sources     │  │  · law_knowledge    │                     │
│  │  · law_articles    │  │    _vectors         │                     │
│  │  · contract        │  │  · template_clause  │                     │
│  │    _templates      │  │    _vectors         │                     │
│  │  · template_clauses│  │  · review_cases     │                     │
│  │  · industry        │  │    _vectors         │                     │
│  │    _knowledge      │  │  · industry         │                     │
│  │  · review_cases    │  │    _knowledge       │                     │
│  │  · doc_type        │  │    _vectors         │                     │
│  │    _registry       │  │                     │                     │
│  │  · compliance      │  └────────────────────┘                     │
│  │    _checklists     │                                              │
│  └────────────────────┘  ┌────────────────────┐                     │
│                          │  Elasticsearch      │                     │
│  ┌────────────────────┐  │  (全文检索)          │                     │
│  │  Neo4j (Phase 3)   │  │                     │                     │
│  │  (知识图谱)         │  │  · law_articles     │                     │
│  │                    │  │    _fulltext        │                     │
│  │  · 法条关系图谱    │  │  · template_clauses │                     │
│  │  · 合同条款图谱    │  │    _fulltext        │                     │
│  │  · 实体关系网络    │  │  · industry         │                     │
│  └────────────────────┘  │    _knowledge       │                     │
│                          │    _fulltext        │                     │
│                          └────────────────────┘                     │
└──────────────────────────────────────────────────────────────────────┘
```

### 6.2 PostgreSQL：结构化存储

v2 新增的表：

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `contract_templates` | 合同模板主表 | template_id, contract_type, industry, status, tenant_id |
| `template_clauses` | 模板条款表（与模板主表一对多） | clause_id, template_id, clause_role, clause_level, clause_text, is_required |
| `clause_annotations` | 条款标注表（与条款表一对多） | annotation_type, description, related_law |
| `industry_knowledge` | 行业知识表 | knowledge_type, industry, contract_types, content, law_references |
| `doc_type_registry` | 文档类型注册表 | doc_type_id, enabled_cards, agent_pipeline, token_budget |
| `compliance_checklists` | 合规自检清单表 | doc_type, check_item, check_method, severity_if_missing |

**contract_templates 表 DDL 示例：**

```sql
CREATE TABLE contract_templates (
    id              BIGSERIAL PRIMARY KEY,
    template_id     VARCHAR(64) UNIQUE NOT NULL,
    template_name   VARCHAR(256) NOT NULL,
    contract_type   VARCHAR(64) NOT NULL,
    industry        VARCHAR(64) DEFAULT '通用',
    source          VARCHAR(64) NOT NULL,
    source_name     VARCHAR(256),
    version         VARCHAR(32),
    status          VARCHAR(20) DEFAULT 'draft',
    tenant_id       BIGINT,
    total_clauses   INT,
    total_chars     INT,
    parties         JSONB,
    required_clause_roles JSONB,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    confirmed_by    VARCHAR(64),
    CONSTRAINT fk_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id)
);

CREATE INDEX idx_ct_contract_type ON contract_templates(contract_type);
CREATE INDEX idx_ct_industry ON contract_templates(industry);
CREATE INDEX idx_ct_status ON contract_templates(status);
CREATE INDEX idx_ct_tenant ON contract_templates(tenant_id);
```

**template_clauses 表 DDL 示例：**

```sql
CREATE TABLE template_clauses (
    id                  BIGSERIAL PRIMARY KEY,
    template_id         VARCHAR(64) NOT NULL,
    clause_id           VARCHAR(32) NOT NULL,
    clause_title        VARCHAR(256),
    clause_role         VARCHAR(32) NOT NULL,
    clause_level        INT NOT NULL DEFAULT 1,
    parent_clause_id    VARCHAR(32),
    clause_text         TEXT NOT NULL,
    is_required         BOOLEAN DEFAULT false,
    risk_level          VARCHAR(16) DEFAULT 'low',
    char_offset_start   INT,
    char_offset_end     INT,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT fk_template FOREIGN KEY (template_id)
        REFERENCES contract_templates(template_id),
    CONSTRAINT uq_template_clause UNIQUE (template_id, clause_id)
);

CREATE INDEX idx_tc_template ON template_clauses(template_id);
CREATE INDEX idx_tc_role ON template_clauses(clause_role);
CREATE INDEX idx_tc_required ON template_clauses(is_required);
```

### 6.3 Milvus：向量存储

v2 新增两个 Collection：

#### template_clause_vectors Collection Schema

```
Collection: template_clause_vectors
├── template_id     : VarChar    (模板 ID)
├── clause_id       : VarChar    (条款 ID，如 "7.1")
├── clause_role     : VarChar    (条款角色)
├── vector_type     : VarChar    (full_clause / clause_title / risk_pattern)
├── embedding       : FloatVector(1024)
├── contract_type   : VarChar    (合同类型，用于 filter)
├── status          : VarChar    (published / deprecated)
└── Partition Key   : contract_type

Index: IVF_FLAT on embedding, nlist=512
Metric: COSINE
```

#### industry_knowledge_vectors Collection Schema

```
Collection: industry_knowledge_vectors
├── knowledge_id    : VarChar    (行业知识 ID)
├── knowledge_type  : VarChar    (best_practice / risk_pattern / norm)
├── embedding       : FloatVector(1024)
├── industry        : VarChar    (行业, 用于 filter)
├── contract_type   : VarChar    (合同类型, 用于 filter)
├── status          : VarChar    (published / deprecated)
└── Partition Key   : industry

Index: IVF_FLAT on embedding, nlist=256
Metric: COSINE
```

### 6.4 Elasticsearch：全文检索

v2 新增的 ES 索引：

```json
{
  "template_clauses_fulltext": {
    "mappings": {
      "properties": {
        "template_id":    { "type": "keyword" },
        "clause_id":      { "type": "keyword" },
        "clause_role":    { "type": "keyword" },
        "clause_text":    { "type": "text", "analyzer": "ik_max_word" },
        "clause_title":   { "type": "text", "analyzer": "ik_smart" },
        "contract_type":  { "type": "keyword" },
        "annotations":    { "type": "text", "analyzer": "ik_max_word" },
        "risk_level":     { "type": "keyword" },
        "status":         { "type": "keyword" }
      }
    }
  },

  "industry_knowledge_fulltext": {
    "mappings": {
      "properties": {
        "knowledge_id":   { "type": "keyword" },
        "knowledge_type": { "type": "keyword" },
        "title":          { "type": "text", "analyzer": "ik_smart" },
        "content":        { "type": "text", "analyzer": "ik_max_word" },
        "industry":       { "type": "keyword" },
        "contract_types": { "type": "keyword" },
        "tags":           { "type": "keyword" },
        "status":         { "type": "keyword" }
      }
    }
  }
}
```

### 6.5 Neo4j：知识图谱（Phase 3）

> Phase 1-2 期间，用 PostgreSQL 递归 CTE + JSONB 存储法条/条款间的关系。Phase 3 迁移到 Neo4j。

v2 新增的图谱模型（Phase 3 设计预留）：

| 实体 | 标签 | 核心属性 | 说明 |
|------|------|---------|------|
| 合同模板 | `ContractTemplate` | template_id, template_name, contract_type | 一份标准合同模板 |
| 模板条款 | `TemplateClause` | clause_id, clause_role, clause_text | 模板中的一个条款 |
| 行业知识 | `IndustryKnowledge` | knowledge_id, title, knowledge_type | 一条行业知识 |

| 关系 | 起始 → 终止 | 说明 |
|------|-----------|------|
| `TEMPLATE_CONTAINS` | ContractTemplate → TemplateClause | 模板包含条款 |
| `CLAUSE_CHILD_OF` | TemplateClause → TemplateClause | 条款层级关系（款→条） |
| `CLAUSE_REFERENCES` | TemplateClause → TemplateClause | 条款间交叉引用 |
| `GOVERNS` | Article → TemplateClause(role) | 法条约束某角色的合同条款 |
| `BEST_PRACTICE_FOR` | IndustryKnowledge → TemplateClause(role) | 行业知识适用于某角色条款 |

---

## 7. 模板向量化策略（v2 新增）

### 7.1 条款级 Embedding

模板库的向量化粒度是**条款级**，而不是模板级。这是因为审查时需要将待审合同的**每个条款**与模板的**对应条款**做语义匹配。

```
┌──────────────────────────────────────────────────────────────────────┐
│  模板条款向量化流程                                                     │
│                                                                      │
│  模板发布 → 对每个条款生成多粒度 Embedding:                              │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  条款: 第7条 违约责任                                         │   │
│  │  clause_role: liability                                      │   │
│  │                                                              │   │
│  │  向量1: full_clause Embedding                                │   │
│  │    输入: clause_title + clause_text (完整条款文本)            │   │
│  │    用途: 与待审合同的对应条款做整体语义匹配                    │   │
│  │                                                              │   │
│  │  向量2: clause_title Embedding                               │   │
│  │    输入: clause_title (仅条款标题)                            │   │
│  │    用途: 快速识别条款角色（当条款没有显式标题时降级）           │   │
│  │                                                              │   │
│  │  向量3: risk_pattern Embedding (每条 annotation 独立)        │   │
│  │    输入: annotation.description (风险模式/最佳实践描述)       │   │
│  │    用途: 与待审合同的条款内容做风险模式匹配                    │   │
│  └──────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────┘
```

**向量化伪代码：**

```java
@Service
public class TemplateClauseVectorizationService {

    private final EmbeddingClient embeddingClient;
    private final MilvusServiceClient milvusClient;
    private static final String COLLECTION = "template_clause_vectors";

    public void vectorizeClause(TemplateClause clause, String contractType) {

        // 1. 完整条款 Embedding
        String fullText = (clause.getTitle() != null ? clause.getTitle() + " " : "")
                        + clause.getText();
        float[] fullVector = embeddingClient.embed(fullText).getOutput();
        insertToMilvus(clause, "full_clause", fullVector, contractType);

        // 2. 条款标题 Embedding（如有标题）
        if (clause.getTitle() != null && !clause.getTitle().isBlank()) {
            float[] titleVector = embeddingClient.embed(clause.getTitle()).getOutput();
            insertToMilvus(clause, "clause_title", titleVector, contractType);
        }

        // 3. 风险模式/最佳实践 Embedding（每条标注独立）
        for (ClauseAnnotation ann : clause.getAnnotations()) {
            float[] annVector = embeddingClient.embed(ann.getDescription()).getOutput();
            insertToMilvus(clause, "risk_pattern", annVector, contractType);
        }
    }

    private void insertToMilvus(TemplateClause clause, String vectorType,
                                float[] vector, String contractType) {
        List<InsertParam.Field> fields = List.of(
            new InsertParam.Field("template_id",   List.of(clause.getTemplateId())),
            new InsertParam.Field("clause_id",     List.of(clause.getClauseId())),
            new InsertParam.Field("clause_role",    List.of(clause.getRole())),
            new InsertParam.Field("vector_type",    List.of(vectorType)),
            new InsertParam.Field("embedding",      List.of(vector)),
            new InsertParam.Field("contract_type",  List.of(contractType)),
            new InsertParam.Field("status",         List.of("published"))
        );

        InsertParam param = InsertParam.newBuilder()
            .withCollectionName(COLLECTION)
            .withPartitionName(contractType)
            .withFields(fields)
            .build();

        milvusClient.insert(param);
    }
}
```

### 7.2 模板匹配的向量检索流程

审查合同时，条款级 Embedding 的检索流程：

```
待审合同条款: 第9条 违约责任
"甲方违约的，乙方有权解除合同并要求甲方赔偿一切损失"
         │
         ▼
  Embedding 模型 (bge-large-zh-v1.5)
         │
         ▼
  得到 1024 维向量
         │
         ├── Step 1: 搜索 template_clause_vectors
         │   filter: contract_type = 'sales_contract'
         │           AND clause_role = 'liability'
         │           AND vector_type = 'full_clause'
         │   top_k: 5
         │
         │   结果:
         │   ① TPL-001 条款7: "违约方支付合同总价10%..." (相似度 0.86)
         │   ② TPL-003 条款8: "任何一方违约应赔偿..." (相似度 0.83)
         │
         ├── Step 2: 搜索 risk_pattern 向量
         │   filter: contract_type = 'sales_contract'
         │           AND vector_type = 'risk_pattern'
         │   top_k: 3
         │
         │   结果:
         │   ① "赔偿一切损失的表述可能因'过分高于'被法院调低" (相似度 0.79)
         │
         └── Step 3: 注入到语义合规 Agent 的上下文
             "=== 标准模板参考 ===
              模板条款(违约责任): '违约方支付合同总价10%的违约金...'
              风险提示: '赔偿一切损失的表述可能被法院调低'

              === 待审查条款 ===
              '甲方违约的，乙方有权解除合同并要求甲方赔偿一切损失'"
```

### 7.3 Milvus Collection Schema（模板库）

> 已在 6.3 节定义，此处不重复。

---

## 8. 人工确认工作台

### 8.1 工作台 UI 原型（通用化）

v2 的人工确认工作台同时支持法条知识和模板条款的确认：

```
┌─────────────────────────────────────────────────────────────────────┐
│  知识确认工作台 (v2)                                                   │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │  Tab: [法条知识] [合同模板] [行业知识]                      │      │
│  └──────────────────────────────────────────────────────────┘      │
│                                                                     │
│  ── 当前 Tab: 合同模板 ──────────────────────────────────────       │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │  待确认: 3 份  │  已发布: 142 份  │  退回重解析: 1 份       │      │
│  └──────────────────────────────────────────────────────────┘      │
│                                                                     │
│  当前审核: IT设备采购合同 v2024                                       │
│                                                                     │
│  ┌──────────────────────┐  ┌─────────────────────────────────┐    │
│  │  【条款列表】          │  │  【条款详情 & LLM 解析】          │    │
│  │                       │  │                                 │    │
│  │  ▸ 第1条 定义 [def]   │  │  条款 7: 违约责任                │    │
│  │  ▸ 第2条 标的 [subj]  │  │  角色: liability     [✓]        │    │
│  │  ▸ 第3条 价款 [pay]   │  │  层级: 1             [✓]        │    │
│  │  ▸ 第4条 交付 [obli]  │  │  必备: 是            [✓]        │    │
│  │  ▸ 第5条 质量 [obli]  │  │  风险: 高            [✓]        │    │
│  │  ▸ 第6条 保密 [gen]   │  │                                 │    │
│  │  ▸ 第7条 违约 [liab] ←│  │  标注:                          │    │
│  │  ▸ 第8条 争议 [disp]  │  │  [risk] 违约金留空  [✓]         │    │
│  │  ▸ 第9条 终止 [term]  │  │  [best] 通常10-30% [✓]         │    │
│  │                       │  │  [sym] 检查7.2对等  [✓]         │    │
│  │                       │  │  [+ 添加标注]                   │    │
│  └──────────────────────┘  └─────────────────────────────────┘    │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │  操作: [✅ 确认发布]  [📝 修改后发布]  [↩️ 退回重新解析]    │      │
│  └──────────────────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────────────┘
```

### 8.2 确认操作与系统行为

| 操作 | 适用对象 | 系统行为 |
|------|---------|---------|
| ✅ 确认发布 | 法条/模板/行业知识 | 状态 → `published`；自动触发向量化与索引；法条/模板/知识进入可检索状态 |
| 📝 修改后发布 | 法条/模板/行业知识 | 人工修改内容覆盖 LLM 解析结果；记录修改日志（原始值→修改值）；执行与"确认发布"相同后续流程 |
| ↩️ 退回重新解析 | 法条/模板 | 附带修改意见重新调用 LLM；退回上限 3 次，超过转人工编辑 |

---

## 9. 知识规模估算（扩展版）

### 9.1 法规知识规模

v2 在 v1 基础上增加了合同相关法规：

| 法律法规来源 | 大约法条数 | 解析后知识条目 | v2 新增 |
|-------------|-----------|--------------|---------|
| v1 已有法规（证券法/基金法/广告法等） | ~1,175 条 | ~2,530 条 | — |
| **《民法典》合同编** | ~230 条 | ~500 条 | **v2 新增** |
| **《劳动合同法》** | ~98 条 | ~220 条 | **v2 新增** |
| **《公司法》** | ~210 条 | ~450 条 | **v2 新增** |
| **最高法合同纠纷解释** | ~50 条 | ~120 条 | **v2 新增** |
| **最高法劳动争议解释** | ~40 条 | ~100 条 | **v2 新增** |
| 其他合同相关法规 | ~200 条 | ~400 条 | **v2 新增** |
| 基金业协会/监管问答/处罚案例 | ~1,000 条 | ~2,000 条 | — |
| **合计** | ~3,003 条 | **~6,320 条结构化知识** | |

### 9.2 合同模板库规模

| 合同类型 | 初始模板数 | 条款总数（估） | Phase 2 目标 |
|---------|----------|-------------|-------------|
| 销售合同 | 15 | ~450 | 30 |
| 服务合同 | 12 | ~360 | 25 |
| 劳动合同 | 10 | ~250 | 20 |
| 保密协议 | 8 | ~160 | 15 |
| 股权合同 | 6 | ~240 | 12 |
| 租赁合同 | 8 | ~200 | 15 |
| 金融合同 | 10 | ~400 | 20 |
| 其他类型 | 6 | ~150 | 15 |
| **合计** | **75** | **~2,210 条款** | **152** |

### 9.3 行业知识库规模

| 行业 | 初始知识条目 | Phase 2 目标 |
|------|-----------|-------------|
| 通用（跨行业） | 100 | 300 |
| IT/互联网 | 50 | 150 |
| 金融 | 80 | 200 |
| 房地产/建筑 | 30 | 100 |
| 人力资源 | 40 | 120 |
| 其他行业 | 30 | 100 |
| **合计** | **330** | **970** |

### 9.4 向量库总规模

```
向量库总规模估算:

法条知识向量:
  6,320 条法条 × 4 种粒度（原文/行为/违规/语义）
  ≈ 平均每条 8 个向量（含多条违规示例和语义延伸）
  → ~50,000 个向量

模板条款向量:
  2,210 条款 × 3 种粒度（全文/标题/标注）
  ≈ 平均每条 4 个向量
  → ~8,800 个向量

行业知识向量:
  330 条知识 × 1 个向量（内容全文）
  → ~330 个向量

历史案例向量:
  企业历史案例 → 预估 10,000-50,000 条
  → ~10,000-50,000 个向量

─────────────────────────────────
总计: ~70,000-110,000 个向量 (1024 维)
Milvus 存储: ~300-500 MB（不含索引）
```

### 9.5 解析工作量估算（扩展版）

| 指标 | v1 估算 | v2 估算 | 说明 |
|------|---------|---------|------|
| 法条总数 | ~3,500 | ~6,320 | 新增合同相关法规 |
| 模板条款总数 | 0 | ~2,210 | v2 新增 |
| 行业知识条目 | 0 | ~330 | v2 新增 |
| 单条 LLM 解析耗时 | 10-20s | 10-20s | 不变 |
| 并发解析能力 | ~10 | ~20 | 提高并发 |
| 法条全量解析时间 | ~1-2h | ~1.5-3h | 线性增长 |
| 模板解析时间 | — | ~2-4h | 模板条款多，单条解析较复杂 |
| 人工确认（法条） | 30-60 人·小时 | 50-100 人·小时 | |
| 人工确认（模板） | — | 30-50 人·小时 | 条款标注需法务专家 |
| 人工确认（行业知识） | — | 10-20 人·小时 | |
| LLM 调用成本 | 数十元 | **~200-500 元** | 法条+模板+知识全量解析 |

---

## 10. LLM 调用方式详解

> 调用架构、Prompt 设计、结构化输出、重试策略与 v1（`docs/01-knowledge-layer.md` 第 3 节）基本一致。

v2 的关键变更：

### 10.1 模型选择策略变更

| 对象 | v1 推荐 | v2 推荐 | 变更原因 |
|------|---------|---------|---------|
| 法条解析 | DeepSeek / 通义千问 | DeepSeek-V3 / Qwen-Max | 模型升级 |
| 模板条款解析 | — | DeepSeek-V3 / GPT-4o-mini | 条款标注需要较好的结构化能力 |
| 行业知识提取 | — | GPT-4o | 需要更强的法律推理能力 |

### 10.2 Prompt 模板（合同模板解析专用）

```
[System]
你是一名资深的合同法律专家，擅长分析合同条款结构和风险。

你的任务是将合同模板中的每个条款解析为结构化 JSON。

解析规则:
1. 严格按原文拆分条款层级（条-款-项）
2. 对每个条款判断其角色 (clause_role)，仅限以下枚举值:
   definition / rights / obligations / liability /
   dispute_resolution / termination / general /
   subject_matter / payment
3. 标注已知的风险模式和最佳实践
4. 标注需要对称性检查的条款对
5. clause_text 必须是条款原文精确复制，不可改写
6. 记录字符偏移位置

请严格按指定的 JSON Schema 输出。

[User]
请解析以下合同模板的条款:

模板名称：{template_name}
合同类型：{contract_type}
行业：{industry}

合同原文:
{full_text}
```

### 10.3 新法规更新流程

> 同 v1（`docs/01-knowledge-layer.md` 第 7 节），但 v2 增加以下联动：

- 新法规影响合同模板时（如《民法典》修改了违约金规定），自动扫描模板库中关联该法条的条款标注，推送"模板标注可能需要更新"通知。
- 行业知识中引用了被修改法条的条目，自动标记为"需复核"。

---

## 附录：v1 → v2 知识构建层变更摘要

| 变更项 | v1 | v2 | 说明 |
|--------|----|----|------|
| 知识来源 | 法规 + 案例 | 法规 + **合同模板** + **行业知识** + 案例 | 三大知识底座 |
| 法规范围 | 金融法规为主 | 金融 + **合同法** + **民商法** + **劳动法** | 通用化 |
| 结构化字段 | 无 doc_type 区分 | 新增 `applicable_doc_types`, `contract_clause_role`, `contract_types` | 支持多文档类型 |
| 向量化 | 法条 4 粒度 | 法条 4 粒度 + **条款 3 粒度** + **行业知识 1 粒度** | 多知识源向量化 |
| 存储 | PG + Milvus + ES + Neo4j | PG + Milvus + ES + **Neo4j 延后到 Phase 3** | 降低初期复杂度 |
| 人工确认 | 仅法条 | 法条 + **模板** + **行业知识** | 三类知识统一确认 |
| 知识规模 | ~3,500 条 | **~8,860 条**（法条+条款+知识） | 约 2.5 倍 |
| 向量规模 | ~15,000-20,000 | **~70,000-110,000** | 约 5 倍 |
| LLM 成本 | 数十元 | **200-500 元** | 全量解析成本仍可控 |
