# 案例管理 — 详细设计文档 (v2)

> 所属系统: 通用智能审查系统 (Universal Intelligent Review System)
> 对应主文档章节: 案例管理与生命周期
> 版本: v2.0 | 升级自 v1 `02-case-management.md`

---

## 目录

- [1. 案例数据结构](#1-案例数据结构)
- [2. 案例向量化策略](#2-案例向量化策略)
- [3. 案例导入渠道](#3-案例导入渠道)
- [4. 案例质量与去重](#4-案例质量与去重)
- [5. 案例生命周期管理](#5-案例生命周期管理)
- [6. 案例检索排序与学习价值分](#6-案例检索排序与学习价值分)

---

## 1. 案例数据结构

### 1.1 通用案例结构（所有文档类型）

每条历史审核案例以 JSON 结构存储。v2 对数据结构做了两项关键扩展：

1. **引入 `review_card` 字段**——标记该案例属于 5 张审查卡片中的哪一张
2. **引入 `contract_context` 可选块**——合同类文档携带条款级上下文

完整示例（合同类文档）：

```json
{
  "case_id": "CASE-2026-00045678",
  "source": "internal_review",
  "created_at": "2026-04-10T09:15:00Z",
  "status": "active",
  "version": 1,

  "context": {
    "doc_type": "合同",
    "content_type": "采购合同",
    "product_type": "工程服务",
    "product_name": "XX数据中心建设项目",
    "channel": "线下签署",
    "tenant_id": "tenant-construction-a"
  },

  "contract_context": {
    "contract_type": "采购合同",
    "clause_id": "7.3.2",
    "clause_text": "乙方应在收到甲方书面通知后三十日内完成整改，逾期未整改的，甲方有权单方解除合同并要求乙方承担全部损失。",
    "clause_category": "违约责任",
    "parties": ["甲方(采购方)", "乙方(供应商)"],
    "related_clause_ids": ["5.1", "8.2", "12.1"]
  },

  "reviewed_content": {
    "original_text": "乙方应在收到甲方书面通知后三十日内完成整改，逾期未整改的，甲方有权单方解除合同并要求乙方承担全部损失。",
    "content_segment_index": 15,
    "full_content_ref": "content-2026-contract-xxx",
    "char_span": {
      "start": 2340,
      "end": 2410
    }
  },

  "review_card": "⑤ 逻辑与条款审查",

  "review_result": {
    "verdict": "violation",
    "severity": "major",
    "reason": "违约责任条款权责不对等：仅约定乙方违约后果（'承担全部损失'），未约定甲方在相同情形下的对等责任。且'全部损失'表述过于宽泛，缺少损失范围限定和上限约定。",
    "law_references": [
      {
        "article_id": "第584条",
        "law_name": "中华人民共和国民法典",
        "relevance": "直接相关"
      }
    ],
    "suggestion": "建议修改为：'乙方应在收到甲方书面通知后三十日内完成整改，逾期未整改的，甲方有权解除合同。因乙方原因导致的直接损失，由乙方承担赔偿责任，赔偿总额不超过合同总金额的30%。'同时增加甲方对等违约条款。",
    "suggestion_type": "可采纳修订"
  },

  "review_meta": {
    "initial_reviewer": "ai",
    "final_reviewer": "human",
    "human_reviewer_id": "reviewer-wangwu",
    "human_action": "modified",
    "human_comment": "LLM 发现了权责不对等问题，但建议的赔偿上限比例不合理，应改为20%",
    "review_time_seconds": 120
  }
}
```

### 1.2 字段详细说明

#### 1.2.1 通用字段

| 字段路径 | 类型 | 必填 | 说明 |
|---------|------|------|------|
| case_id | String | 是(自动) | 全局唯一案例ID，格式 `CASE-{年份}-{8位序号}`。系统自动生成。 |
| source | Enum | 是 | 案例来源：`internal_review` / `external_penalty` / `manual_import` / `template_diff`(v2新增)。 |
| created_at | DateTime | 是(自动) | 案例创建时间，ISO 8601 UTC。 |
| status | Enum | 是 | v2 新增。案例当前状态：`active`（活跃）/ `decayed`（衰减）/ `archived`（归档）/ `superseded`（被替代）。详见第 5 节。 |
| version | Integer | 是 | v2 新增。案例版本号，初始为 1。案例被更新时递增。 |
| context.doc_type | String | 是 | v2 新增。文档大类：`合同` / `营销物料` / `招股说明书` / `产品说明书` / `内部制度` / `通用`。决定是否激活 `contract_context` 等专用字段块。 |
| context.content_type | String | 是 | 具体内容类型。营销类：`营销海报`、`微信推文`等；合同类：`采购合同`、`服务合同`、`劳动合同`等。 |
| context.product_type | String | 否 | 产品类型。金融场景：`公募基金`、`ETF`等；通用场景：`工程服务`、`IT服务`等。 |
| context.product_name | String | 否 | 具体产品/项目名称。 |
| context.channel | String | 否 | 渠道：`微信朋友圈`、`官网`、`线下签署`、`电子签章`等。 |
| context.tenant_id | String | 是 | 所属租户ID，格式 `tenant-{机构标识}`。 |
| review_card | Enum | 是 | v2 新增。该案例对应的审查卡片：`① 文本基础核对` / `② 红线与敏感词` / `③ 形式与要素审查` / `④ 语义与合规审查` / `⑤ 逻辑与条款审查`。 |
| reviewed_content.original_text | String | 是 | 被审查的内容原文片段。营销场景典型 10-100 字；合同场景可达 50-500 字（一个完整条款）。 |
| reviewed_content.content_segment_index | Integer | 否 | 段落在原始文档中的位置索引（从 0 开始）。 |
| reviewed_content.full_content_ref | String | 否 | 原始完整文档的对象存储引用ID。 |
| reviewed_content.char_span | Object | 否 | v2 新增。字符级定位：`start`（起始字符偏移）、`end`（结束字符偏移）。用于在文档中精确高亮。 |
| review_result.verdict | Enum | 是 | 最终判定：`violation` / `compliant` / `needs_review`。 |
| review_result.severity | Enum | 条件必填 | 严重程度（仅 violation 时）：`critical` / `major` / `minor` / `info`。 |
| review_result.reason | String | 是 | 判定理由。 |
| review_result.law_references | Object[] | 条件必填 | 引用的法条/规范列表（仅 violation 时至少一条）。 |
| review_result.suggestion | String | 否 | 修改建议文本。 |
| review_result.suggestion_type | Enum | 否 | v2 新增。建议类型：`纯提示` / `可采纳修订` / `协商点建议`。合同场景必填。 |
| review_meta.initial_reviewer | Enum | 是 | 初审方：`ai` / `human`。 |
| review_meta.final_reviewer | Enum | 是 | 终审方：`human` / `ai`。 |
| review_meta.human_reviewer_id | String | 否 | 人工复核人ID。 |
| review_meta.human_action | Enum | 否 | 人工操作：`confirmed` / `rejected` / `modified` / `supplemented`。 |
| review_meta.human_comment | String | 否 | 人工复核备注。 |
| review_meta.review_time_seconds | Integer | 否 | 人工复核耗时（秒）。 |

#### 1.2.2 合同专用字段块（contract_context）

当 `context.doc_type = "合同"` 时，`contract_context` 块为必填：

| 字段路径 | 类型 | 必填 | 说明 |
|---------|------|------|------|
| contract_context.contract_type | String | 是 | 合同类型：`采购合同` / `服务合同` / `销售合同` / `劳动合同` / `保密协议` / `股权合同` / `租赁合同` / `技术合同`。 |
| contract_context.clause_id | String | 是 | 条款编号。采用合同原文中的编号体系，如 `7.3.2`、`第八条第三款`。 |
| contract_context.clause_text | String | 是 | 条款完整原文（与 `reviewed_content.original_text` 可能相同，也可能是更大范围的条款上下文）。 |
| contract_context.clause_category | String | 是 | 条款所属类别：`定义条款` / `标的条款` / `价款条款` / `履行条款` / `违约责任` / `争议解决` / `保密条款` / `生效终止` / `附则`。 |
| contract_context.parties | String[] | 否 | 涉及的合同各方，如 `["甲方(采购方)", "乙方(供应商)"]`。 |
| contract_context.related_clause_ids | String[] | 否 | 关联条款编号列表。存储与当前条款有引用、对等、冲突关系的其他条款编号。 |

### 1.3 关于 reviewed_content 长度的说明

v2 维持 v1 的核心设计决策：**案例存储的是片段级文本，不是整篇文档。**

但 v2 区分了不同文档类型的典型片段长度：

| 文档类型 | 典型 original_text 长度 | 示例 |
|----------|----------------------|------|
| 营销物料 | 10-100 字 | `"亏损概率很小"` |
| 合同条款 | 50-500 字 | `"乙方应在收到甲方书面通知后三十日内完成整改..."` |
| 招股说明书 | 30-300 字 | `"本公司不存在对外担保或可能引致重大损失的诉讼..."` |
| 通用文档 | 10-200 字 | 视内容类型而定 |

合同场景的单条案例稍长（一个完整条款），但仍远小于整篇合同。Token 消耗估算：

```
合同案例检索上下文:
  单条案例压缩后长度: 约 80-150 tokens (比营销案例稍长)
  每次检索案例数(top_k): 约 5-8 条
  案例上下文总 token 量: 约 400-1200 tokens
  系统 token 预算中案例部分: 最多 ~3000 tokens

结论: 合同案例不会撑爆上下文窗口。
```

### 1.4 按审查卡片分类的案例示例

5 张审查卡片各自沉淀不同类型的案例：

| 卡片 | 案例 original_text 示例 | verdict | 典型 reason |
|------|----------------------|---------|------------|
| ① 文本基础核对 | `"甲方应在签署之日其三十日内付款"` | violation | 存在错别字："其"应为"起" |
| ② 红线与敏感词 | `"本产品保本保收益"` | violation | 命中禁用词"保本保收益" |
| ③ 形式与要素审查 | (整个合同) | violation | 缺少争议解决条款（必备要素） |
| ④ 语义与合规审查 | `"未经对方书面同意，任何一方不得将合同权利义务转让给第三方"` | compliant | 符合合同转让的法定要求 |
| ⑤ 逻辑与条款审查 | `"第5条约定交付期限为30天，第12条约定逾期超过15天即视为根本违约"` | violation | 交付期限与违约认定条款存在逻辑矛盾 |

---

## 2. 案例向量化策略

### 2.1 向量化架构总览

v2 的案例向量化在 v1 的 3 种 Embedding 基础上，新增了**第 4 种：合同条款 Embedding**，专用于合同类文档的条款级语义检索。

```
┌──────────────────────────────────────────────────────────────┐
│  案例向量化策略 (v2)                                           │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  所有文档类型通用 — 3 种 Embedding                       │ │
│  │                                                         │ │
│  │  ① 审查内容 Embedding                                   │ │
│  │     输入: reviewed_content.original_text                 │ │
│  │     用途: 待审查内容 → 检索相似案例                      │ │
│  │                                                         │ │
│  │  ② 违规理由 Embedding                                   │ │
│  │     输入: review_result.reason                           │ │
│  │     用途: 按违规模式检索（如"权责不对等"类案例）          │ │
│  │                                                         │ │
│  │  ③ 组合 Embedding                                       │ │
│  │     输入: original_text + verdict + reason 拼接          │ │
│  │     用途: 综合语义检索                                   │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  合同类文档专用 — 第 4 种 Embedding (v2 新增)            │ │
│  │                                                         │ │
│  │  ④ 条款 Embedding                                       │ │
│  │     输入: contract_context.clause_category               │ │
│  │            + " " + clause_text                           │ │
│  │     用途: 按条款类别+语义检索相似条款案例                 │ │
│  │           例: 检索所有"违约责任"类条款的审查案例           │ │
│  │                                                         │ │
│  │  激活条件: context.doc_type == "合同"                     │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
│  Embedding 模型: bge-large-zh-v1.5 (1024 维)                 │
│  存储: Milvus review_cases_vectors Collection                │
│  Partition Key: tenant_id                                    │
└──────────────────────────────────────────────────────────────┘
```

### 2.2 四种 Embedding 类型详细说明

| 编号 | Embedding 类型 | 输入文本 | 适用文档 | 检索场景 |
|------|---------------|----------|---------|---------|
| ① | 审查内容 Embedding | `reviewed_content.original_text` | 所有 | ACE 检索路径 D：用待审查段落匹配相似案例 |
| ② | 违规理由 Embedding | `review_result.reason` | 所有 | 查找同类违规模式，如"保本暗示"、"权责不对等" |
| ③ | 组合 Embedding | `original_text + " " + verdict + " " + reason` | 所有 | 综合语义检索，模糊查询某类内容的某种判定 |
| ④ | 条款 Embedding | `clause_category + " " + clause_text` | 合同 | 合同审查时按条款类别检索相似条款判例 |

### 2.3 向量存储结构（Milvus Collection Schema）

```
Collection: review_cases_vectors
├── Partition Key: tenant_id (租户隔离)
│
├── Fields:
│   ├── case_id          (VARCHAR, Primary Key)
│   ├── tenant_id        (VARCHAR, Partition Key)
│   ├── embedding_type   (VARCHAR: "content" / "reason" / "combined" / "clause")
│   ├── vector           (FLOAT_VECTOR, dim=1024)
│   ├── doc_type         (VARCHAR: "合同" / "营销物料" / ...)
│   ├── review_card      (VARCHAR: "①" / "②" / "③" / "④" / "⑤")
│   ├── verdict          (VARCHAR: "violation" / "compliant" / "needs_review")
│   ├── severity         (VARCHAR: "critical" / "major" / "minor" / "info" / null)
│   ├── human_confirmed  (BOOL)
│   ├── learning_value   (INT32)
│   ├── status           (VARCHAR: "active" / "decayed" / "archived")  ← v2 新增
│   ├── decay_score      (FLOAT: 0.0-1.0)                             ← v2 新增
│   └── created_at       (INT64: 时间戳)
│
├── Index:
│   └── IVF_SQ8 on vector field (nlist=1024)
│
└── 典型查询:
    top_k = 3~8
    filter = "tenant_id == '{当前租户}'
              AND human_confirmed == true
              AND status == 'active'"
    metric_type = COSINE
```

### 2.4 实现伪代码

```java
@Service
public class CaseVectorService {

    private final EmbeddingModel embeddingModel;
    private final MilvusServiceClient milvusClient;

    public void indexCase(ReviewCase reviewCase) {
        String originalText = reviewCase.getReviewedContent().getOriginalText();
        String reason = reviewCase.getReviewResult().getReason();
        String verdict = reviewCase.getReviewResult().getVerdict();

        // ① 审查内容 Embedding
        float[] contentVector = embeddingModel.embed(originalText);
        upsertVector(reviewCase, "content", contentVector);

        // ② 违规理由 Embedding
        float[] reasonVector = embeddingModel.embed(reason);
        upsertVector(reviewCase, "reason", reasonVector);

        // ③ 组合 Embedding
        String combinedText = originalText + " " + verdict + " " + reason;
        float[] combinedVector = embeddingModel.embed(combinedText);
        upsertVector(reviewCase, "combined", combinedVector);

        // ④ 条款 Embedding (仅合同类文档)
        if ("合同".equals(reviewCase.getContext().getDocType())
                && reviewCase.getContractContext() != null) {
            ContractContext cc = reviewCase.getContractContext();
            String clauseInput = cc.getClauseCategory() + " " + cc.getClauseText();
            float[] clauseVector = embeddingModel.embed(clauseInput);
            upsertVector(reviewCase, "clause", clauseVector);
        }
    }

    public List<ReviewCase> searchSimilarCases(
            String queryText,
            String tenantId,
            String docType,
            String reviewCard,
            int topK) {

        float[] queryVector = embeddingModel.embed(queryText);

        StringBuilder filterExpr = new StringBuilder();
        filterExpr.append("embedding_type == 'content'");
        filterExpr.append(" AND human_confirmed == true");
        filterExpr.append(" AND status == 'active'");

        if (docType != null) {
            filterExpr.append(" AND doc_type == '").append(docType).append("'");
        }
        if (reviewCard != null) {
            filterExpr.append(" AND review_card == '").append(reviewCard).append("'");
        }

        SearchParam searchParam = SearchParam.newBuilder()
            .withCollectionName("review_cases_vectors")
            .withPartitionNames(List.of(tenantId))
            .withMetricType(MetricType.COSINE)
            .withTopK(topK)
            .withVectors(List.of(queryVector))
            .withVectorFieldName("vector")
            .withExpr(filterExpr.toString())
            .build();

        R<SearchResults> response = milvusClient.search(searchParam);
        return loadCasesByIds(extractCaseIds(response));
    }
}
```

### 2.5 检索时的排序与权重

检索到的案例在组装进 LLM 上下文之前，按以下规则排序：

```
最终得分 = cosine_similarity
           × learning_value_weight
           × decay_score               ← v2: 替代 v1 的离散 recency_weight
           × card_match_bonus

其中:
  learning_value_weight:
    学习价值分 >= 3 (人工补充的漏检):  × 1.3
    学习价值分 == 2 (人工驳回):        × 1.2
    学习价值分 == 1 (人工修改):        × 1.1
    学习价值分 == 0 (人工确认):        × 1.0

  decay_score:  (v2: 连续衰减函数，替代 v1 的离散分段)
    decay_score = max(0.5, 1.0 - 0.02 × months_since_creation)
    6个月内:  0.88 ~ 1.0
    12个月:   0.76
    24个月:   0.52
    25个月+:  0.50 (下限，不再继续衰减)

  card_match_bonus:
    案例 review_card 与当前审查 Agent 的卡片类型一致: × 1.15
    不一致: × 1.0
```

---

## 3. 案例导入渠道

v2 在 v1 的三大导入渠道（内部审核沉淀、外部处罚、人工导入）基础上，新增**第四渠道：合同模板差异导入**。

```
┌─────────────────────────────────────────────────────────────────────┐
│                     案例导入渠道总览 (v2)                               │
│                                                                      │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌─────────────┐│
│  │ ① 内部审核    │ │ ② 外部处罚    │ │ ③ 人工导入    │ │ ④ 模板差异  ││
│  │ (internal_   │ │ (external_   │ │ (manual_     │ │ (template_  ││
│  │  review)     │ │  penalty)    │ │  import)     │ │  diff) NEW  ││
│  │              │ │              │ │              │ │             ││
│  │ 审查→反馈    │ │ 爬虫+LLM    │ │ Excel/JSON   │ │ 合同 vs     ││
│  │ →自动生成    │ │ →人工确认    │ │ 批量上传     │ │ 标准模板    ││
│  │              │ │              │ │              │ │ 条款级diff  ││
│  │ 持续产生     │ │ 定时采集     │ │ 一次性/按需  │ │ 模板更新时  ││
│  │ ~85%案例量   │ │ ~5%案例量    │ │ ~5%案例量    │ │ ~5%案例量   ││
│  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘ └──────┬──────┘│
│         └────────────────┼────────────────┼────────────────┘       │
│                          ▼                ▼                        │
│                ┌──────────────────────────────┐                   │
│                │  统一案例入库流水线              │                   │
│                │  质量评估 → 去重 → 向量化 → 入库 │                   │
│                └──────────────────────────────┘                   │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.1 渠道一：内部审核沉淀

与 v1 逻辑一致，是案例的主要来源。每一次完整的"Agent 审查 → 人工复核"流程自动沉淀案例。

v2 增强点：
- 每条案例自动标记所属的 `review_card`（由产生该案例的 Agent 决定）
- 合同类文档的案例自动填充 `contract_context` 块

#### 各复核操作对应的案例生成行为

| 人工操作 | 生成的案例 verdict | 学习价值基础分 | 说明 |
|---------|-------------------|-------------|------|
| confirmed | 与 LLM 判定一致 | +0 | LLM 判对了 |
| rejected | 与 LLM 判定相反 | +2 | LLM 判错了，高价值纠错案例 |
| modified | 使用人工修改后版本 | +1 | LLM 部分正确 |
| supplemented | 人工新增的违规项 | +3 | LLM 遗漏了，最高价值 |

### 3.2 渠道二：外部处罚案例

与 v1 逻辑一致。v2 扩展了数据来源覆盖面，不再局限于金融监管：

| 来源类别 | 具体来源 | 采集频率 |
|---------|---------|---------|
| 金融监管 | 证监会、银保监会、基金业协会处罚公告 | 每日 |
| 市场监管 | 各地市场监管局行政处罚公示 | 每周 |
| 司法裁判 | 裁判文书网合同纠纷裁判 | 每周 |
| 行业协会 | 各行业协会纪律处分公告 | 每周 |

### 3.3 渠道三：人工导入

与 v1 一致，支持 Excel/JSON/CSV 批量上传。v2 增加了合同专用字段的校验：

| 校验项 | 规则 | 错误处理 |
|--------|------|---------|
| contract_context.clause_id | doc_type="合同"时必填 | 标记为错误行 |
| contract_context.contract_type | doc_type="合同"时必填 | 标记为错误行 |
| contract_context.clause_category | doc_type="合同"时必填，须匹配预设枚举 | 标记为错误行 |
| review_card | 必填，须为 ①~⑤ 之一 | 标记为错误行 |
| reviewed_content.original_text | 非空，长度 1-2000 字符（v2 放宽合同场景上限） | 标记为错误行 |

### 3.4 渠道四：合同模板差异导入（v2 新增）

**核心场景：** 企业维护了一套"标准合同模板库"（如标准采购合同、标准服务合同）。当合同模板更新时，新旧模板之间的条款差异本身就是高价值的审查案例——它告诉系统"同一类条款，过去怎么写是合规的，现在怎么写才合规"。

#### 流程详情

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│ 模板库    │ →   │ 条款级    │ →   │ LLM 分析 │ →   │ 人工确认  │ →   │ 案例入库  │
│ 检测到新  │     │ diff 计算│     │ diff 含义│     │ diff 案例│     │          │
│ 版本上传  │     │          │     │ 与合规影响│     │          │     │          │
└──────────┘     └──────────┘     └──────────┘     └──────────┘     └──────────┘
```

**步骤详解：**

**Step 1: 条款级 diff 计算**

当标准合同模板上传新版本时，系统自动执行条款级对比：

```
旧模板条款 7.3.2:
  "乙方违约的，应承担违约金，违约金为合同总金额的10%。"

新模板条款 7.3.2:
  "乙方违约的，应承担违约金，违约金为合同总金额的10%。
   违约金不足以弥补甲方实际损失的，乙方应补足差额，
   但赔偿总额不超过合同总金额的30%。"

Diff 结果:
  类型: 条款修改(MODIFIED)
  新增内容: "违约金不足以弥补...不超过合同总金额的30%"
  变更意图: 增加了损失赔偿上限约定
```

**Step 2: LLM 分析 diff 含义**

```
Prompt:
以下是标准合同模板的一处条款变更。请分析：
1. 变更的合规/风险含义
2. 为什么要做这个变更（推断）
3. 如果审查到类似的"旧版"表述，应该给出什么建议

旧版: {old_clause}
新版: {new_clause}
```

**Step 3: 生成案例**

每处 diff 自动生成两条对偶案例：

| 案例 | original_text | verdict | reason |
|------|--------------|---------|--------|
| 案例 A（旧版） | 旧模板条款原文 | violation | 缺少赔偿上限约定（基于模板更新推断） |
| 案例 B（新版） | 新模板条款原文 | compliant | 包含了赔偿上限约定，符合最新模板标准 |

**激活条件：** 仅当 `context.doc_type = "合同"` 且租户启用了"标准合同模板库"功能时可用。

---

## 4. 案例质量与去重

### 4.1 去重策略

与 v1 逻辑一致，但 v2 增加了 `review_card` 维度的去重考量。

#### 4.1.1 去重判定规则

```
┌──────────────────────────────────────────────────────────────────┐
│  案例去重判定流程 (v2)                                              │
│                                                                   │
│  新案例 original_text                                             │
│       │                                                           │
│       ▼                                                           │
│  Step 1: 向量相似度检查                                           │
│                                                                   │
│    new_vector = embed(new_case.original_text)                     │
│    existing = milvus.search(                                      │
│      collection = "review_cases_vectors",                         │
│      vector = new_vector,                                         │
│      top_k = 5,                                                   │
│      filter = "embedding_type == 'content'                        │
│               AND tenant_id == '{当前租户}'                        │
│               AND status == 'active'"                             │
│    )                                                              │
│                                                                   │
│    cosine_similarity > 0.95 → 候选重复                            │
│    cosine_similarity ≤ 0.95 → 非重复                              │
│                                                                   │
│       │ 存在候选重复                                               │
│       ▼                                                           │
│  Step 2: 多维一致性检查                                           │
│                                                                   │
│    verdict 相同 AND review_card 相同?                              │
│      → 是: 确认为重复 ⛔                                          │
│      → 否: 不视为重复 ✅                                          │
│                                                                   │
│    例外: 新案例 learning_value_score 更高                          │
│      → 替换旧案例（旧案例标记 superseded）                         │
│                                                                   │
│  阈值说明:                                                        │
│    > 0.95  重复（几乎相同的表述+相同判定+相同卡片）                 │
│    0.85-0.95  相似但不重复（保留两条）                              │
│    ≤ 0.85  明显不同                                                │
└──────────────────────────────────────────────────────────────────┘
```

#### 4.1.2 去重的例外情况

| 场景 | 原因 |
|------|------|
| verdict 不同 | 同一文本的正反判定都有参考价值 |
| review_card 不同 | 同一文本可能在不同审查维度有不同判定 |
| 引用不同法条 | 同一文本可能违反多条法规 |
| 不同租户 | 租户间数据隔离 |
| 新案例学习价值更高 | 高价值案例替换低价值案例 |

### 4.2 案例质量评估

#### 学习价值分计算

```
学习价值分 = 基础分 + 额外加分

基础分 (按 human_action):
  supplemented (人工补充的漏检):     +3
  rejected (人工驳回的误判):         +2
  modified (人工修改):               +1
  confirmed (人工确认):              +0

额外加分:
  被标记为"典型案例":                +1
  涉及新型违规模式(库中无相似案例):   +1
  涉及新发布法规(近 30 天生效):      +1
  外部处罚案例(有监管背书):          +1
  合同模板 diff 案例(模板权威性):     +1  ← v2 新增
```

---

## 5. 案例生命周期管理

> **v2 新增章节**。回应 Gemini 评估报告 R3 指出的"越用越准"闭环退化风险：confirmed 案例过多导致检索结果冗余，高价值案例被挤出。

### 5.1 生命周期状态机

```
┌──────────────────────────────────────────────────────────────────┐
│  案例生命周期状态机                                                 │
│                                                                   │
│       ┌──────────┐                                                │
│       │  active   │ ← 新案例入库默认状态                            │
│       └────┬─────┘                                                │
│            │                                                      │
│    ┌───────┼───────────────┐                                      │
│    │       │               │                                      │
│    ▼       ▼               ▼                                      │
│ 被更高价值  时间衰减       定期质量审计                              │
│ 案例替代    触发阈值       不合格                                   │
│    │       │               │                                      │
│    ▼       ▼               ▼                                      │
│ ┌──────┐ ┌──────────┐  ┌──────────┐                             │
│ │super-│ │ decayed   │  │ archived │                             │
│ │seded │ │           │  │          │                             │
│ └──────┘ └─────┬────┘  └──────────┘                             │
│                │                                                  │
│                ▼                                                  │
│          12个月无检索命中                                          │
│                │                                                  │
│                ▼                                                  │
│          ┌──────────┐                                            │
│          │ archived │                                            │
│          └──────────┘                                            │
│                                                                   │
│  状态说明:                                                        │
│  active     — 正常参与检索，decay_score 实时计算                   │
│  decayed    — decay_score < 0.6，仍参与检索但权重低                │
│  archived   — 不再参与检索，仅供审计和数据分析                     │
│  superseded — 被更高学习价值的相似案例替代                         │
│                                                                   │
│  状态可逆:                                                        │
│  decayed → active:  人工标记为"长期有效"                           │
│  archived → active: 人工手动恢复                                  │
└──────────────────────────────────────────────────────────────────┘
```

### 5.2 时间衰减机制

案例的检索权重随时间衰减，使新鲜的、近期产生的案例在检索中有更高优先级。v2 采用**连续衰减函数**替代 v1 的离散分段权重。

#### 衰减公式

```
decay_score(t) = max(0.50, 1.0 - 0.02 × t)

其中 t = months_since_creation（案例创建至今的月数）

  创建时:     decay_score = 1.00
  6 个月:     decay_score = 0.88
  12 个月:    decay_score = 0.76
  18 个月:    decay_score = 0.64
  24 个月:    decay_score = 0.52
  25 个月+:   decay_score = 0.50 (下限，不再继续衰减)
```

#### 衰减豁免条件

以下案例不参与时间衰减（decay_score 恒定为 1.0）：

| 豁免条件 | 原因 |
|---------|------|
| 被标记为"典型案例" | 典型案例具有长期教学价值 |
| learning_value_score ≥ 3 | 高价值案例（人工补充/驳回）不应因时间被淘汰 |
| 近 6 个月内有检索命中 | 仍在被使用的案例不应衰减 |
| 关联的法条近 12 个月内有更新 | 法条更新可能赋予旧案例新的参考价值 |

#### 衰减计算时机

```
衰减分数更新策略:

  实时计算 (检索时):
    每次检索到案例时，实时计算 decay_score
    不需要预计算，因为 decay_score 只是简单的时间差公式
    Milvus 过滤条件不直接使用 decay_score，
    而是在 RRF 合并阶段的应用层做加权

  批量更新 (每月一次):
    定时任务扫描所有 active 案例
    decay_score < 0.60 → 状态变更为 decayed
    decayed 状态且 12 个月无检索命中 → 变更为 archived
```

### 5.3 同 Embedding 邻域采样（防膨胀机制）

**问题：** 大量 confirmed 案例集中在同一语义邻域（如"保本暗示"相关的数百条几乎相同的案例），导致检索 top_k 被同质案例占满，挤出其他有价值但不同主题的案例。

**解决方案：** 同 Embedding 邻域采样——当同一语义邻域内的 active 案例超过阈值时，只保留代表性案例，其余标记为 `archived`。

```
┌──────────────────────────────────────────────────────────────────┐
│  同邻域采样流程 (每月执行)                                          │
│                                                                   │
│  Step 1: 邻域聚类                                                 │
│    对所有 active 案例的 content Embedding 做聚类                    │
│    (HDBSCAN, min_cluster_size=5)                                  │
│    → 每个聚类 = 一个"语义邻域"                                     │
│                                                                   │
│  Step 2: 检查邻域密度                                              │
│    对每个聚类，统计 active 案例数量                                 │
│    如果 count > max_per_cluster (默认=20):                         │
│      → 触发采样                                                   │
│                                                                   │
│  Step 3: 代表性采样                                                │
│    保留策略 (按优先级，共保留 max_per_cluster 条):                   │
│    ① learning_value_score 最高的 top-5                             │
│    ② 各 verdict 至少保留 2 条 (违规+合规对照)                       │
│    ③ 各 review_card 至少保留 1 条                                  │
│    ④ 创建时间最近的 5 条                                           │
│    ⑤ 剩余名额按 decay_score 降序选取                               │
│                                                                   │
│  Step 4: 标记非代表性案例                                          │
│    未被选中的案例 → status = 'archived'                            │
│    archived_reason = 'neighborhood_sampling'                      │
│    保留数据不物理删除                                               │
│                                                                   │
│  示例:                                                            │
│    "保本暗示"邻域有 87 条 active 案例                               │
│    → 采样后保留 20 条代表性案例                                     │
│    → 67 条标记为 archived                                         │
│    → 检索 top_k 时不再被同质案例占满                                │
│                                                                   │
│  参数可配置:                                                       │
│    max_per_cluster: 默认 20, 可按租户/文档类型调整                   │
│    min_cluster_size: 默认 5, HDBSCAN 参数                         │
│    采样频率: 默认每月 1 日执行                                      │
└──────────────────────────────────────────────────────────────────┘
```

### 5.4 定期案例质量审计

**目的：** 发现和处理案例库中的质量问题，确保"越用越准"的闭环不退化为"噪声叠加环"。

```
┌──────────────────────────────────────────────────────────────────┐
│  案例质量审计 (每季度执行)                                          │
│                                                                   │
│  审计项 1: 引用法条有效性检查                                      │
│    扫描所有 active 案例的 law_references                           │
│    如果引用的法条已被标记为 deprecated:                             │
│      → 案例 decay_score 减半 (不直接归档，可能仍有参考价值)         │
│      → 推送合规团队复核: "以下案例引用了已废止法条"                 │
│                                                                   │
│  审计项 2: 同内容冲突检测                                          │
│    查找 cosine_similarity > 0.90 但 verdict 不同的案例对           │
│    推送合规团队: "以下案例存在判定争议，请确认哪条正确"              │
│    如果争议解决:                                                   │
│      → 错误的案例标记为 archived, archived_reason='conflict_resolved'│
│                                                                   │
│  审计项 3: 复核员一致性检查                                        │
│    统计每位复核员在同一 review_card 下的 reject/modify 率           │
│    如果某复核员的 reject 率显著偏离团队均值 (>2σ):                  │
│      → 预警: "该复核员可能存在标准偏差"                             │
│      → 推送给合规主管复核该复核员近期的案例                         │
│                                                                   │
│  审计项 4: 案例使用率统计                                          │
│    统计每条案例在过去 6 个月中被检索命中的次数                      │
│    如果命中次数 = 0 且 decay_score < 0.60:                        │
│      → 标记为 archived, archived_reason='unused'                 │
│    如果命中次数 > 50:                                              │
│      → 标记为"高频案例"，推荐升级为"典型案例"                      │
│                                                                   │
│  审计项 5: 案例总量监控                                            │
│    按租户统计 active 案例总量                                      │
│    如果 active 案例 > 50,000:                                     │
│      → 预警: "案例库规模较大，建议执行邻域采样"                     │
│      → 自动触发一次邻域采样任务                                    │
│                                                                   │
│  审计报告输出:                                                     │
│    ┌────────────────────────────────────────────────────┐        │
│    │ 本季度审计摘要                                      │        │
│    │                                                    │        │
│    │ active 案例总量:   12,456                           │        │
│    │ 本季度新增:        342                              │        │
│    │ 本季度归档:        67 (邻域采样) + 23 (过期淘汰)     │        │
│    │ 法条失效关联:      5 条案例需复核                    │        │
│    │ 判定冲突:          3 对案例需确认                    │        │
│    │ 复核员偏差预警:    1 位复核员                        │        │
│    │ 高频案例推荐:      8 条建议升级为典型案例            │        │
│    └────────────────────────────────────────────────────┘        │
└──────────────────────────────────────────────────────────────────┘
```

### 5.5 案例归档规则

案例归档是将不再需要参与实时检索的案例从 active 检索范围中移出。归档不等于删除——归档案例仍然保留在数据库中，可用于审计、分析和手动恢复。

| 归档触发条件 | 归档原因码 | 可恢复 |
|-------------|-----------|--------|
| 被更高价值案例替代 (去重流程触发) | `superseded` | 否（被新案例覆盖） |
| 邻域采样淘汰 (月度采样) | `neighborhood_sampling` | 是 |
| 引用法条全部失效 (质量审计) | `law_deprecated` | 是（新法出台可能重新适用） |
| 长期未被检索命中 + 衰减到位 | `unused` | 是 |
| 人工判定冲突后淘汰 | `conflict_resolved` | 否（已确认为错误） |
| 人工主动归档 | `manual` | 是 |

#### 归档后的数据处理

```
案例归档后:

  PostgreSQL:
    status 字段更新为 'archived'
    archived_at 字段记录归档时间
    archived_reason 字段记录原因
    其余字段保留不变

  Milvus:
    status 字段更新为 'archived'
    检索过滤条件 status == 'active' 自动排除归档案例
    向量数据保留（不删除），避免重新向量化的成本

  Elasticsearch:
    文档标记 archived=true
    检索时默认排除（可手动查询归档案例）

  归档案例占总存储的比例预估:
    系统运行 12 个月后: ~15% 归档
    系统运行 24 个月后: ~30% 归档
    系统运行 36 个月后: ~40% 归档
```

---

## 6. 案例检索排序与学习价值分

### 6.1 检索排序的完整公式

当某条审查内容需要检索相似案例时，系统从 Milvus 拿到 top_k 候选后，按以下公式重排：

```
final_score = cosine_similarity
              × learning_value_weight(learning_value_score)
              × decay_score(months_since_creation)
              × card_match_bonus(review_card)
              × source_quality_weight(source)

  source_quality_weight:
    external_penalty (有监管背书):    × 1.15
    template_diff (有模板权威性):     × 1.10
    internal_review:                  × 1.00
    manual_import:                    × 0.95

  最终排序: 按 final_score 降序
  截断: 取 top-N (N 根据 token 预算动态计算, 通常 5-10 条)
```

### 6.2 案例在 LLM 上下文中的呈现格式

案例被检索后，按以下压缩格式注入 LLM prompt：

**营销物料场景：**

```
=== 参考案例 ===
案例#12345[④语义]: "亏损概率很小" → 违规(保本暗示) | 法条:第24条①②
案例#12400[④语义]: "本基金属于中低风险" → 合规(客观描述) 
案例#12388[②红线]: "保本保收益" → 违规(命中禁用词) | 法条:第24条①②
```

**合同审查场景：**

```
=== 参考案例 ===
案例#45678[⑤逻辑](违约责任/采购合同):
  "乙方应承担全部损失" → 违规(权责不对等,缺赔偿上限)
  | 法条:民法典584条 | 建议:增加赔偿上限条款
案例#45690[④语义](保密条款/服务合同):
  "保密期限为合同终止后永久" → 违规(保密期限不合理)
  | 建议:修改为3-5年
案例#45700[⑤逻辑](争议解决/采购合同):
  "由甲方所在地法院管辖" → 合规(约定管辖,符合法律规定)
```

### 6.3 检索参数按审查卡片差异化

不同审查卡片对案例检索的需求不同：

| 审查卡片 | top_k | 过滤条件 | 说明 |
|---------|-------|---------|------|
| ① 文本基础核对 | 3 | review_card='①' | 较少依赖案例，主要靠 NLP |
| ② 红线与敏感词 | 0 | — | 不检索案例，使用词典匹配 |
| ③ 形式与要素审查 | 5 | review_card='③' | 检索同类文档的必备要素案例 |
| ④ 语义与合规审查 | 8 | review_card='④' | 核心依赖案例，检索量最大 |
| ⑤ 逻辑与条款审查 | 8 | review_card='⑤' AND doc_type='合同' | 合同专用，检索条款级案例 |

### 6.4 案例在审查中的实际作用示例

```
合同审查场景:

待审查条款 8.2:
  "甲方应在合同签订后七个工作日内支付合同总金额的100%。"

→ 向量检索案例库 (review_card='⑤', doc_type='合同'):

  命中案例 CASE-2026-00045700:
    条款类型: 价款条款 / 采购合同
    原文: "甲方应在验收合格后十五日内支付合同总金额的95%，
           保留5%作为质保金。"
    判定: compliant
    理由: 付款节奏合理，有质保金安排

  命中案例 CASE-2026-00045800:
    条款类型: 价款条款 / 工程合同
    原文: "甲方应在合同签订后三日内支付合同总金额的100%。"
    判定: violation
    理由: 一次性全额预付风险极高，无验收环节保障
    建议: 分期付款，设置验收节点

→ 两条案例注入 LLM 的 prompt:
  LLM 参照先例判断:
  "七个工作日内支付100%"接近"三日内支付100%"的高风险模式，
  应判定为 needs_review，建议分期付款并设置验收节点。
```
