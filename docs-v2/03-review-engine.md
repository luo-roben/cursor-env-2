# 审查执行层 — 多 Agent 流水线详细设计 (v2)

> 所属系统: 通用智能审查系统 (Universal Intelligent Review System)
> 对应主文档章节: 第二层 — 审查执行层 (在线)
> 版本: v2.0 | 升级自 v1 `03-review-engine.md`
>
> v2 核心升级：
> - 多 Agent 编排（StateGraph/DAG）替代 v1 的线性流水线
> - 5 张审查卡片映射到独立 Agent
> - 字符级 span 定位
> - Prompt Caching 与模型路由
> - 修订建议分层（纯提示 / 可采纳修订 / 协商点）

---

## 目录

- [1. 多 Agent 编排架构](#1-多-agent-编排架构)
- [2. 五张审查卡片与 Agent 映射](#2-五张审查卡片与-agent-映射)
- [3. ACE 上下文组装（增强版）](#3-ace-上下文组装增强版)
- [4. 字符级 span 定位](#4-字符级-span-定位)
- [5. LLM 输出格式（按卡片结构化 JSON）](#5-llm-输出格式按卡片结构化-json)
- [6. 长文档审查策略](#6-长文档审查策略)
- [7. Prompt Caching 策略](#7-prompt-caching-策略)
- [8. 模型路由](#8-模型路由)
- [9. 性能与并发设计](#9-性能与并发设计)

---

## 1. 多 Agent 编排架构

### 1.1 设计动机

v1 采用线性 6 阶段流水线（快速过滤 → 内容解析 → ACE 组装 → LLM 调用 → 后处理 → 返回），存在以下问题：

1. **所有审查维度挤在一次 LLM 调用中**——prompt 同时包含格式检查、禁用词、合规审查、逻辑分析的指令，LLM 注意力分散
2. **无法按维度差异化模型选择**——简单检查（禁用词）和复杂推理（跨条款逻辑）用同一个模型
3. **长文档的三智能体方案过于简略**——缺少显式编排和状态传递协议
4. **合同审查场景缺少条款解析和跨条款一致性检查环节**

v2 采用 **StateGraph / DAG 编排**，将审查流程拆分为 12 个独立 Agent，每个 Agent 有明确的输入/输出 Schema、独立的 LLM 选择和 token 预算。

### 1.2 StateGraph 全景图

```
┌──────────────────────────────────────────────────────────────────────────┐
│  审查执行层 — StateGraph / DAG 编排                                        │
│                                                                          │
│  ┌────────────────┐                                                     │
│  │ ClassificationAgent │ ← 文档类型分类（合同/营销/说明书/通用）            │
│  └────────┬───────┘                                                     │
│           │                                                              │
│           ▼                                                              │
│  ┌────────────────┐                                                     │
│  │  ParserAgent    │ ← 内容解析（OCR/文档解析/分段）                      │
│  └────────┬───────┘                                                     │
│           │                                                              │
│           ▼                                                              │
│  ┌────────────────┐                                                     │
│  │ QuickFilterAgent│ ← 快速过滤（禁用词 Aho-Corasick + 格式校验）         │
│  └────────┬───────┘                                                     │
│           │                                                              │
│     ┌─────┴─────┐                                                       │
│     │ 条件分支   │                                                       │
│     │ doc_type   │                                                       │
│     │ == "合同"? │                                                       │
│     └──┬─────┬──┘                                                       │
│        │     │                                                           │
│    YES │     │ NO                                                        │
│        ▼     │                                                           │
│  ┌───────────┐│                                                          │
│  │ ClauseParser││ ← 条款解析 (仅合同)                                     │
│  │ Agent     ││   识别条款层级结构                                        │
│  └─────┬─────┘│   构建条款内部引用图                                      │
│        │      │                                                          │
│        └──┬───┘                                                          │
│           │                                                              │
│           ▼                                                              │
│  ┌────────────────────┐                                                 │
│  │ ACE ContextAssembler│ ← 上下文组装 (六路检索, v2 新增路径 F)            │
│  └────────┬───────────┘                                                 │
│           │                                                              │
│           ▼  ──── 并行扇出 (fan-out) ────                                │
│   ┌───────┼───────┬───────┬───────┬───────┐                             │
│   ▼       ▼       ▼       ▼       ▼       │                             │
│ ┌─────┐ ┌─────┐ ┌─────┐ ┌──────┐ ┌──────┐│                             │
│ │Text │ │Red- │ │Fmt  │ │Seman-│ │Logic ││                             │
│ │Basic│ │line │ │Elem │ │tic   │ │Clause││                             │
│ │Agent│ │Agent│ │Agent│ │Agent │ │Agent ││                             │
│ │ ①  │ │ ②  │ │ ③  │ │ ④   │ │ ⑤   ││                             │
│ └──┬──┘ └──┬──┘ └──┬──┘ └──┬───┘ └──┬───┘│                             │
│    └───────┴───────┴───────┴────────┘    │                             │
│                    │                      │                             │
│                    ▼  ── 汇合 (fan-in) ── │                             │
│           ┌────────┴───────┐              │                             │
│           │ 条件分支:       │              │                             │
│           │ doc_type=="合同"│              │                             │
│           │ && 文档>5000字? │              │                             │
│           └──┬──────────┬──┘              │                             │
│          YES │          │ NO              │                             │
│              ▼          │                 │                             │
│    ┌─────────────┐      │                 │                             │
│    │ CrossClause  │      │                 │                             │
│    │ Agent        │      │                 │                             │
│    │ (跨条款一致性)│      │                 │                             │
│    └──────┬──────┘      │                 │                             │
│           └──────┬──────┘                 │                             │
│                  ▼                        │                             │
│         ┌────────────────┐                │                             │
│         │ PostProcessAgent│ ← 法条 ID 回填 + 风险评分                     │
│         └────────┬───────┘                                              │
│                  ▼                                                      │
│         ┌────────────────┐                                              │
│         │ AssemblyAgent   │ ← 合并所有卡片结果 + 生成报告                  │
│         └────────────────┘                                              │
└──────────────────────────────────────────────────────────────────────────┘
```

### 1.3 Agent 定义总览

| Agent | 功能 | 输入 | 输出 | LLM | Token 预算 |
|-------|------|------|------|-----|-----------|
| ClassificationAgent | 文档类型自动分类 | 原始文件 + 用户元数据 | `doc_type`, `content_type` | 4o-mini | 500 |
| ParserAgent | 内容解析与分段 | 原始文件 | `UnifiedContent` (段落列表+元数据) | 无(纯代码) | — |
| QuickFilterAgent | 禁用词+格式校验 | 段落列表 | 命中的禁用词列表 + 格式校验结果 | 无(Aho-Corasick) | — |
| ClauseParserAgent | 合同条款解析 | 段落列表 | 条款树 + 条款引用图 | 4o-mini | 2000 |
| ACE ContextAssembler | 上下文组装 | 段落列表+元数据+条款树 | 组装后的上下文 | 无(检索) | — |
| TextBasicsAgent | ① 文本基础核对 | 段落列表 | 错别字/标点/格式问题列表 | 4o-mini | 2000 |
| RedlineAgent | ② 红线与敏感词 | 段落列表+禁用词表 | 命中的敏感词列表 | 无(字典) | — |
| FormatElementsAgent | ③ 形式与要素审查 | 段落列表+checklist | 缺失要素列表 | 4o-mini / 4o | 3000 |
| SemanticComplianceAgent | ④ 语义与合规审查 | 段落列表+ACE上下文 | 违规判定列表 | 4o / Sonnet | 15000 |
| LogicClauseAgent | ⑤ 逻辑与条款审查 | 条款树+ACE上下文 | 逻辑问题列表 | 4o / Sonnet | 12000 |
| CrossClauseAgent | 跨条款一致性检查 | 条款树+Agent⑤结果 | 跨条款矛盾列表 | 4o | 8000 |
| PostProcessAgent | 法条 ID 回填+评分 | 所有 Agent 输出 | 校验后结果+风险评分 | 无(代码) | — |
| AssemblyAgent | 结果合并+报告生成 | 所有处理后结果 | 最终 ReviewResult | 无(代码) | — |

### 1.4 状态传递协议

StateGraph 中 Agent 之间通过 `ReviewState` 对象传递状态：

```java
public record ReviewState(
    // 输入阶段
    String reviewId,
    RawFile rawFile,
    ContentMeta userMeta,

    // ClassificationAgent 输出
    String docType,
    String contentType,

    // ParserAgent 输出
    UnifiedContent unifiedContent,

    // QuickFilterAgent 输出
    List<RedlineHit> quickFilterHits,

    // ClauseParserAgent 输出 (仅合同)
    ClauseTree clauseTree,
    Map<String, List<String>> clauseRefGraph,

    // ACE ContextAssembler 输出
    AssembledContext aceContext,

    // 各卡片 Agent 输出
    List<CardResult> textBasicsResults,     // ①
    List<CardResult> redlineResults,        // ②
    List<CardResult> formatElementResults,  // ③
    List<CardResult> semanticResults,       // ④
    List<CardResult> logicClauseResults,    // ⑤

    // CrossClauseAgent 输出
    List<CardResult> crossClauseResults,

    // PostProcessAgent 输出
    List<CardResult> verifiedResults,
    int overallRiskScore,
    String riskLevel,

    // AssemblyAgent 输出
    ReviewResult finalResult
) {}
```

### 1.5 Agent 依赖与条件分支

```
Agent 依赖关系（显式 DAG 边）:

  ClassificationAgent → ParserAgent → QuickFilterAgent
                                        │
                                  ┌─────┴──────┐
                                  │   条件分支   │
                                  │ doc_type=合同│
                                  └──┬───────┬──┘
                              YES   │       │ NO
                                    ▼       │
                             ClauseParserAgent
                                    │       │
                                    └───┬───┘
                                        │
                                        ▼
                              ACE ContextAssembler
                                        │
                            ┌───────────┼───────────┐
                            ▼           ▼           ▼
                    TextBasicsAgent  RedlineAgent  FormatElementsAgent
                    SemanticComplianceAgent  LogicClauseAgent
                            │           │           │
                            └───────────┼───────────┘
                                        │
                                  ┌─────┴──────┐
                                  │   条件分支   │
                                  │ 合同&&>5000字│
                                  └──┬───────┬──┘
                              YES   │       │ NO
                                    ▼       │
                             CrossClauseAgent│
                                    │       │
                                    └───┬───┘
                                        │
                                        ▼
                                PostProcessAgent
                                        │
                                        ▼
                                 AssemblyAgent
```

---

## 2. 五张审查卡片与 Agent 映射

### 2.1 卡片总览

```
┌──────────────────────────────────────────────────────────────────────┐
│  五张审查卡片 — Agent 映射                                             │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ ① 文本基础核对 (Text & Basics)                                 │ │
│  │   Agent: TextBasicsAgent                                      │ │
│  │   技术: NLP 文本纠错 + LLM 轻量级校验                          │ │
│  │   模型: 4o-mini (低成本, 高速)                                 │ │
│  │   检查项: 错别字, 标点错误, 数字格式, 日期格式, 人名/机构名一致性 │ │
│  │   适用: 所有文档类型                                           │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ ② 红线与敏感词 (Redlines)                                      │ │
│  │   Agent: RedlineAgent                                         │ │
│  │   技术: Aho-Corasick 多模式匹配 + 租户级词典                    │ │
│  │   模型: 无 LLM (纯代码)                                       │ │
│  │   检查项: 禁用词, 敏感词, 合规红线表达                          │ │
│  │   适用: 所有文档类型                                           │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ ③ 形式与要素审查 (Format & Elements)                           │ │
│  │   Agent: FormatElementsAgent                                  │ │
│  │   技术: Checklist 规则引擎 + LLM 辅助 (语义判断缺失项)          │ │
│  │   模型: 4o-mini (简单checklist) / 4o (复杂语义判断)             │ │
│  │   检查项: 必备条款/章节是否齐全, 签署要素完整性, 格式合规性      │ │
│  │   适用: 所有文档类型（checklist 按 doc_type 差异化）            │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ ④ 语义与合规审查 (Semantic Compliance)                         │ │
│  │   Agent: SemanticComplianceAgent                              │ │
│  │   技术: RAG 检索 + LLM 深度推理                                │ │
│  │   模型: 4o / Claude Sonnet (重型推理)                          │ │
│  │   检查项: 法规违反, 监管红线, 行业惯例偏离                      │ │
│  │   适用: 所有文档类型                                           │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ ⑤ 逻辑与条款审查 (Logic & Clauses)                             │ │
│  │   Agent: LogicClauseAgent + CrossClauseAgent                  │ │
│  │   技术: LLM 推理 + 条款引用图分析                               │ │
│  │   模型: 4o / Claude Sonnet (重型推理)                          │ │
│  │   检查项: 跨条款矛盾, 权责对等性, 定义一致性, 时间线合理性       │ │
│  │   适用: 主要面向合同; 其他长文档简化版                          │ │
│  └────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
```

### 2.2 各 Agent 详细设计

#### 2.2.1 TextBasicsAgent（① 文本基础核对）

```
输入:
  segments: List<ContentSegment>   (分段文本)
  doc_type: String                 (文档类型)

处理流程:
  ┌────────────────────────────────────────────────────┐
  │  Step 1: NLP 预处理 (不用 LLM)                      │
  │    · 基于词典的错别字检测 (pycorrector / hunspell)   │
  │    · 标点符号规范检查 (中英文标点混用等)              │
  │    · 数字/日期格式统一性检查                         │
  │    → 输出: 候选错误列表                              │
  │                                                    │
  │  Step 2: LLM 确认 (4o-mini)                         │
  │    · 将候选错误列表 + 原文送给 LLM                   │
  │    · LLM 过滤误报 (如专有名词被误判为错别字)          │
  │    · LLM 补充 NLP 遗漏的上下文相关错误               │
  │    → 输出: 确认后的问题列表                          │
  └────────────────────────────────────────────────────┘

输出 Schema:
  List<CardResult> where CardResult:
    card: "① 文本基础核对"
    segment_index: int
    original_text: String
    char_span: {start, end}
    issue_type: "typo" / "punctuation" / "number_format" / "date_format"
    severity: "info" / "minor"
    description: String
    suggestion: String
    location_substring: String    ← 用于字符级定位

模型: gpt-4o-mini
Token 预算: ~2000 tokens (input + output)
延迟预算: < 2s
```

#### 2.2.2 RedlineAgent（② 红线与敏感词）

```
输入:
  segments: List<ContentSegment>
  redline_dict: AhoCorasickAutomaton   (租户禁用词自动机)

处理流程:
  ┌────────────────────────────────────────────────────┐
  │  纯代码实现，无 LLM                                 │
  │                                                    │
  │  Step 1: 加载租户级禁用词表                          │
  │    · 全局禁用词: "保本保收益", "稳赚不赔", ...       │
  │    · 租户自定义禁用词                                │
  │    · 行业特定禁用词                                  │
  │    → 构建 Aho-Corasick 自动机 (应用启动时预编译)      │
  │                                                    │
  │  Step 2: 全文扫描                                   │
  │    · 对每个段落执行 Aho-Corasick 多模式匹配           │
  │    · 记录匹配位置 (精确到字符偏移)                    │
  │    → 输出: 命中词列表 + 位置                         │
  │                                                    │
  │  Step 3: 上下文过滤 (可选)                           │
  │    · 某些词在特定上下文中可能不是敏感词                │
  │    · 例: "保本"在"不得保本宣传"中是引用而非违规        │
  │    · 基于窗口规则过滤 (如前后 10 字包含"不得""禁止")   │
  └────────────────────────────────────────────────────┘

输出 Schema:
  List<CardResult> where CardResult:
    card: "② 红线与敏感词"
    segment_index: int
    char_span: {start, end}
    issue_type: "banned_word"
    severity: "critical" / "major"
    matched_word: String
    description: "命中禁用词: {word}"
    location_substring: String

模型: 无 (纯 Aho-Corasick)
延迟: < 5ms
```

#### 2.2.3 FormatElementsAgent（③ 形式与要素审查）

```
输入:
  unifiedContent: UnifiedContent
  doc_type: String
  content_type: String
  checklist: List<CheckItem>    ← 从 compliance_checklists 表动态加载

处理流程:
  ┌────────────────────────────────────────────────────┐
  │  Step 1: 加载 checklist (从知识库)                   │
  │    SELECT * FROM compliance_checklists              │
  │    WHERE (content_type = :type OR content_type IS NULL)│
  │    AND enabled = true                               │
  │    AND (tenant_id = :tenant OR tenant_id IS NULL)    │
  │                                                    │
  │    示例 (合同/采购合同):                              │
  │    ┌───────────────────────────────────────────┐   │
  │    │ 检查项             检查方式    缺失严重度   │   │
  │    │ 合同主体信息        keyword    critical     │   │
  │    │ 合同标的条款        semantic   critical     │   │
  │    │ 价款/报酬条款       semantic   critical     │   │
  │    │ 履行期限条款        keyword    major        │   │
  │    │ 违约责任条款        semantic   major        │   │
  │    │ 争议解决条款        semantic   major        │   │
  │    │ 保密条款            keyword    minor        │   │
  │    │ 不可抗力条款        keyword    minor        │   │
  │    │ 双方签章            keyword    critical     │   │
  │    └───────────────────────────────────────────┘   │
  │                                                    │
  │  Step 2: 按检查方式分类执行                          │
  │    keyword 类: 关键词匹配 (不需要 LLM)               │
  │    semantic 类: 送 LLM 做语义判断                    │
  │    layout 类: 解析元数据判断 (如字号)                 │
  │    conditional 类: 条件检查 (如"有业绩数据时须标注来源")│
  │                                                    │
  │  Step 3: LLM 辅助判断 (仅 semantic 类)               │
  │    将 semantic 类检查项 + 文档内容送给 LLM            │
  │    Prompt: "以下文档是否包含{检查项}？如缺失请说明。" │
  └────────────────────────────────────────────────────┘

输出 Schema:
  List<CardResult> where CardResult:
    card: "③ 形式与要素审查"
    issue_type: "missing_element"
    element_name: String
    severity: "critical" / "major" / "minor"
    law_reference: {article_id, law_name}
    suggestion: String

模型: 4o-mini (简单 keyword) / 4o (语义判断)
Token 预算: ~3000 tokens
延迟预算: < 3s
```

#### 2.2.4 SemanticComplianceAgent（④ 语义与合规审查）

这是整个系统的**核心 Agent**，执行 RAG + LLM 的深度合规推理。

```
输入:
  segments: List<ContentSegment>
  aceContext: AssembledContext      ← 包含检索到的法条+案例+规则
  doc_type: String
  content_type: String

处理流程:
  ┌────────────────────────────────────────────────────┐
  │  Step 1: 上下文注入                                 │
  │    将 ACE 组装的法条上下文、案例上下文、企业规则      │
  │    注入 System Prompt                               │
  │                                                    │
  │  Step 2: 逐段/逐条款审查                             │
  │    对每个段落/条款:                                   │
  │    · 组装 User Prompt (待审内容 + 元数据)             │
  │    · 调用 LLM 做合规判定                             │
  │    · LLM 输出: verdict + reason + law_ref(仅ID)      │
  │      + suggestion + location_substring              │
  │                                                    │
  │  Step 3: 结果收集                                   │
  │    · 每段/条款的审查结果汇入 CardResult 列表          │
  │    · 标记 confidence 分数                            │
  └────────────────────────────────────────────────────┘

Prompt 模板:
  [SYSTEM]
  你是一名资深合规审查专家，拥有丰富的法律法规知识和审查经验。

  你的任务是逐段审查用户提交的内容，对每段内容做合规判断。

  审查要求:
  1. 必须基于下方提供的【适用法规】和【参考案例】做出判断
  2. 每条违规判定必须引用具体法条编号(article_id)和法律简称(law_name)
  3. 不需要输出法条原文——系统会自动回填
  4. 对每条违规，在 location_substring 字段输出问题文本的精确子串
  5. 修改建议按类型分层:
     · suggestion_type="纯提示": 仅提醒风险，不给出修改文本
     · suggestion_type="可采纳修订": 给出可直接替换的修改文本
     · suggestion_type="协商点建议": 需要各方协商的要点

  === 适用法规 ===
  {assembled_law_context}

  === 参考案例 ===
  {assembled_case_context}

  === 企业特殊要求 ===
  {tenant_custom_rules}

  请严格按以下 JSON 格式输出:
  {output_schema}

  [USER]
  请审查以下{doc_type}内容（{content_type}，{product_type}）：

  {segmented_content}

输出 Schema:
  List<CardResult> where CardResult:
    card: "④ 语义与合规审查"
    segment_index: int
    original_text: String
    location_substring: String    ← LLM 输出的精确子串
    verdict: "violation" / "compliant" / "needs_review"
    confidence: float (0-1)
    issue_type: String
    severity: "critical" / "major" / "minor" / "info"
    description: String
    law_reference: {article_id, law_name}  ← 仅 ID，后处理回填原文
    suggestion: String
    suggestion_type: "纯提示" / "可采纳修订" / "协商点建议"

模型: gpt-4o / claude-3.5-sonnet (重型推理)
Token 预算: ~15000 tokens (input: ~12000 + output: ~3000)
延迟预算: 5-15s
```

#### 2.2.5 LogicClauseAgent + CrossClauseAgent（⑤ 逻辑与条款审查）

```
LogicClauseAgent — 单条款逻辑审查:

输入:
  clauseTree: ClauseTree           ← ClauseParserAgent 输出
  aceContext: AssembledContext
  segments: List<ContentSegment>   ← 非合同文档走段落

处理流程:
  ┌────────────────────────────────────────────────────┐
  │  合同文档:                                          │
  │    对 clauseTree 中每个条款:                         │
  │    · 检查权责对等性 (甲方义务 vs 乙方义务)            │
  │    · 检查时间线合理性 (履行期限 vs 违约认定期限)       │
  │    · 检查定义引用一致性                              │
  │    · 检查数值合理性 (违约金比例、赔偿上限)            │
  │                                                    │
  │  非合同文档:                                        │
  │    对分段内容做逻辑一致性简化检查:                     │
  │    · 前后表述矛盾检测                                │
  │    · 数据引用一致性                                  │
  │    · 承诺与条件的逻辑自洽                            │
  └────────────────────────────────────────────────────┘

模型: gpt-4o / claude-3.5-sonnet
Token 预算: ~12000 tokens

---

CrossClauseAgent — 跨条款一致性检查:

输入:
  clauseTree: ClauseTree
  clauseRefGraph: Map<String, List<String>>
  logicClauseResults: List<CardResult>   ← LogicClauseAgent 的输出

处理流程:
  ┌────────────────────────────────────────────────────┐
  │  仅合同文档 && 文档 > 5000 字时触发                  │
  │                                                    │
  │  Step 1: 构建条款摘要                               │
  │    将每个条款压缩为一行摘要                           │
  │    (由 ClauseParserAgent 或单独 LLM 调用完成)        │
  │                                                    │
  │  Step 2: LLM 全局一致性检查                          │
  │    输入: 条款摘要列表 + 条款引用图 + Agent⑤的发现     │
  │    Prompt: "检查以下条款之间是否存在矛盾、引用断裂、   │
  │            权责不对称、定义冲突等问题"                 │
  │                                                    │
  │  检查维度:                                           │
  │  · 交叉引用有效性: 第5条引用第8.3条 → 8.3条是否存在   │
  │  · 定义一致性: "关联方"在不同条款中定义是否矛盾        │
  │  · 权责对称: 甲方有单方解约权 → 乙方是否有对等权利    │
  │  · 时间线: 交付期30天 vs 逾期15天即根本违约 → 矛盾    │
  │  · 争议解决: 多条款约定不同的争议解决方式 → 矛盾       │
  └────────────────────────────────────────────────────┘

输出 Schema:
  List<CardResult> where CardResult:
    card: "⑤ 逻辑与条款审查"
    clause_ids: String[]    ← 涉及的多个条款编号
    issue_type: "cross_clause_contradiction" / "reference_broken"
               / "rights_imbalance" / "definition_inconsistency"
               / "timeline_conflict"
    severity: "critical" / "major"
    description: String
    suggestion: String
    suggestion_type: "协商点建议"

模型: gpt-4o
Token 预算: ~8000 tokens
触发条件: doc_type == "合同" AND total_chars > 5000
```

---

## 3. ACE 上下文组装（增强版）

### 3.1 六路检索策略

v2 在 v1 的 A-E 五路检索基础上，新增 **路径 F：合同模板条款 diff**。

```
┌──────────────────────────────────────────────────────────────────────┐
│  ACE 上下文组装器 v2 (ContextAssembler)                                │
│                                                                      │
│  输入:                                                                │
│  · 段落列表 / 条款树                                                  │
│  · 元数据: doc_type, content_type, product_type, channel             │
│  · 租户配置                                                          │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │  路径 A: 按内容类型 (结构化查询 → PostgreSQL)                   │ │
│  │  SELECT * FROM law_articles                                    │ │
│  │  WHERE :content_type IN applicable_content_types               │ │
│  │  AND status = 'published'                                      │ │
│  │  → 预期 ~15 条法条                                              │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │  路径 B: 按产品类型 (结构化查询 → PostgreSQL)                   │ │
│  │  → 预期 ~20 条法条                                              │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │  路径 C: 按内容语义 (向量检索 → Milvus)                        │ │
│  │  对每个段落/条款做 Embedding → Milvus top_k=10                  │ │
│  │  → 预期 ~25 条法条(去重后)                                      │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │  路径 D: 相似历史案例 (向量检索 → Milvus)                      │ │
│  │  对每个段落/条款做 Embedding → Milvus top_k=3~8                 │ │
│  │  过滤: human_confirmed=true AND status='active'                │ │
│  │  按 review_card 匹配加权                                       │ │
│  │  → 预期 ~5-10 条案例                                           │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │  路径 E: 企业自定义规则 (直接加载 → PostgreSQL)                 │ │
│  │  → 预期 ~5-20 条规则                                            │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │  路径 F: 合同模板条款 diff (v2 新增, 仅合同文档)                │ │
│  │                                                                │ │
│  │  激活条件: doc_type == "合同" AND 租户启用了标准合同模板库        │ │
│  │                                                                │ │
│  │  Step 1: 识别最匹配的标准模板                                   │ │
│  │    SELECT * FROM contract_templates                             │ │
│  │    WHERE contract_type = :contract_type                         │ │
│  │    AND tenant_id = :tenant_id                                   │ │
│  │    ORDER BY version DESC LIMIT 1                                │ │
│  │                                                                │ │
│  │  Step 2: 条款级 diff                                            │ │
│  │    对当前合同的每个条款，与模板中对应位置的标准条款做 diff         │ │
│  │    · 标准条款存在 → 计算文本差异                                 │ │
│  │    · 标准条款不存在 → 标记为"非标准条款"                         │ │
│  │    · 当前合同缺失标准条款 → 标记为"缺失标准条款"                 │ │
│  │                                                                │ │
│  │  Step 3: diff 结果注入上下文                                    │ │
│  │    将差异显著的条款（编辑距离 > 30%）注入 prompt:                 │ │
│  │    "=== 与标准模板的差异 ==="                                    │ │
│  │    "条款7.3: 标准模板规定赔偿上限30%, 当前合同无赔偿上限"        │ │
│  │                                                                │ │
│  │  → 预期输出: ~3-10 条显著差异                                   │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │  合并与去重 + Token 预算控制                                    │ │
│  │                                                                │ │
│  │  RRF 融合 → 效力层级重排 → Token 预算裁剪                      │ │
│  └────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
```

### 3.2 Token 预算按 norm_type 分桶

> v2 新增。回应 Gemini R4：全局 top-K 裁剪可能把某一类别的法条全部裁掉。

v2 改为**按 norm_type 分桶后再裁**，确保每个违规类别至少保留 top-N 条法条。

```
Token 预算分桶策略:

  总 token 预算 (法条部分): 6000 tokens

  Step 1: 按 norm_type 分桶
    禁止类法条 bucket:   按 RRF 分数排序
    义务类法条 bucket:   按 RRF 分数排序
    权利类法条 bucket:   按 RRF 分数排序
    定义类法条 bucket:   按 RRF 分数排序
    程序类法条 bucket:   按 RRF 分数排序

  Step 2: 每桶保底分配
    每个非空桶至少保留 top-3 条法条
    (每条法条约 40-80 tokens, 5 桶 × 3 条 × 60 tokens ≈ 900 tokens)

  Step 3: 剩余配额按全局 RRF 排名分配
    剩余 token 预算 = 6000 - 已分配
    在所有桶中按全局 RRF 排名选取未被选中的法条
    直到 token 预算用尽

  Step 4: 效力层级重排 (v1 已有)
    在每个桶内，按 authority_level × freshness_weight 微调排序
    法律 > 行政法规 > 部门规章 > 自律规则

  示例:
    检索到 60 条法条:
      禁止类: 25 条 (主要来自 RRF)
      义务类: 15 条
      权利类: 8 条
      定义类: 10 条
      程序类: 2 条

    分桶裁剪后:
      禁止类: 保留 12 条 (top-3 保底 + 剩余配额分配)
      义务类: 保留 8 条
      权利类: 保留 5 条
      定义类: 保留 5 条
      程序类: 保留 2 条 (桶内只有2条,全部保留)
    → 总计 32 条, ≈ 5800 tokens, 在预算内
```

### 3.3 案例上下文按审查卡片分发

v2 的案例检索结果不再统一注入单个 prompt，而是按 `review_card` 分发到对应的 Agent：

```
案例分发策略:

  检索到的全部案例 (去重后约 10-15 条)
        │
        ▼
  按 review_card 分组:
    card ① 的案例 → 注入 TextBasicsAgent 的 prompt
    card ② 的案例 → (RedlineAgent 不用案例)
    card ③ 的案例 → 注入 FormatElementsAgent 的 prompt
    card ④ 的案例 → 注入 SemanticComplianceAgent 的 prompt
    card ⑤ 的案例 → 注入 LogicClauseAgent 的 prompt

  每个 Agent 只看到与自己卡片相关的案例
  → 减少每个 Agent 的上下文长度
  → 提高案例与审查维度的匹配度
```

---

## 4. 字符级 span 定位

> v2 新增。回应 Gemini 评估报告"必须补 1"：精确定位是合同审查的用户体验第一杠杆。

### 4.1 定位策略总览

```
┌──────────────────────────────────────────────────────────────────────┐
│  字符级 span 定位 — 三层策略                                           │
│                                                                      │
│  Layer 1: LLM 输出精确子串                                            │
│    LLM 在审查结果中输出 location_substring 字段                        │
│    该字段的值是原文中的一个精确子串                                     │
│    例: location_substring = "亏损概率很小"                             │
│                                                                      │
│  Layer 2: 后端严格子串验证                                             │
│    PostProcessAgent 对 LLM 输出的 location_substring                  │
│    在原文中做严格子串匹配 (String.indexOf)                             │
│    · 匹配成功 → 计算 char_span = {start, end}                        │
│    · 匹配失败 → fallback 到 Layer 3                                  │
│                                                                      │
│  Layer 3: 降级为段落级定位                                             │
│    如果 LLM 输出的子串在原文中找不到（LLM 改写/截断了原文）             │
│    → 使用 segment_index 做段落级定位                                   │
│    → 在审查报告中标记 "定位精度: 段落级"                               │
│    → 统计降级率, 定期优化 prompt                                      │
│                                                                      │
│  预期定位精度:                                                        │
│    Layer 1+2 成功率: ~85-90%                                          │
│    Layer 3 降级率:   ~10-15%                                          │
│    目标: 优化 prompt 后降级率 < 5%                                    │
└──────────────────────────────────────────────────────────────────────┘
```

### 4.2 LLM Prompt 中的定位指令

在 SemanticComplianceAgent 和 LogicClauseAgent 的 System Prompt 中加入：

```
定位要求:
对每条发现的问题，在 location_substring 字段输出问题文本在原文中的
精确子串（必须是原文的逐字复制，不可改写、不可截断、不可添加省略号）。
系统会用这个子串做精确定位。

正确示例:
  原文: "乙方应在收到甲方书面通知后三十日内完成整改"
  location_substring: "三十日内完成整改"  ← 原文子串

错误示例:
  location_substring: "30日内完成整改"    ← 改写了"三十"为"30"
  location_substring: "三十日内..."       ← 截断了
```

### 4.3 后端子串验证实现

```java
public class SpanLocator {

    public CharSpan locate(String fullText, String locationSubstring,
                           int segmentIndex, List<ContentSegment> segments) {

        // Layer 2: 严格子串匹配
        int idx = fullText.indexOf(locationSubstring);
        if (idx >= 0) {
            return new CharSpan(idx, idx + locationSubstring.length(),
                                "character", 1.0);
        }

        // 尝试归一化后再匹配 (全角→半角, 空格归一化)
        String normalizedFull = normalize(fullText);
        String normalizedSub = normalize(locationSubstring);
        idx = normalizedFull.indexOf(normalizedSub);
        if (idx >= 0) {
            int originalStart = mapNormalizedIndexToOriginal(fullText, idx);
            int originalEnd = originalStart + locationSubstring.length();
            return new CharSpan(originalStart, originalEnd,
                                "character_normalized", 0.95);
        }

        // Layer 3: 降级为段落级
        ContentSegment seg = segments.get(segmentIndex);
        return new CharSpan(seg.getStartOffset(), seg.getEndOffset(),
                            "paragraph_fallback", 0.5);
    }
}
```

### 4.4 修订后的 span 重锚定（Re-anchoring）

当用户根据审查建议修改了文档后，原有的 char_span 可能失效。系统需要重新定位。

```
Re-anchoring 策略:

  场景: 用户修改了合同第 7.3 条，原有 span 偏移了

  Step 1: 检测文档变更
    用户上传修改后的文档重新审查时
    → 系统自动检测文档 MD5 变化
    → 标记为"修订后重审"

  Step 2: 重锚定流程
    对每条原有审查结果:
    · 用 location_substring 在新文档中重新做子串匹配
    · 匹配成功 → 更新 char_span
    · 匹配失败 → 标记为 "定位失效，可能已被修改"
      → 如果该条问题的 verdict 仍为 violation
        → 提示用户手动确认

  Step 3: 增量审查
    仅对 diff 变化的段落/条款重新执行 Agent 审查
    未变化的段落保留原有审查结果（char_span 已重锚定）
```

---

## 5. LLM 输出格式（按卡片结构化 JSON）

### 5.1 统一的 CardResult 结构

所有审查 Agent 的输出统一为 `CardResult` 结构，由 AssemblyAgent 合并为最终 `ReviewResult`：

```json
{
  "card": "④ 语义与合规审查",
  "segment_index": 3,
  "original_text": "现在买入，亏损概率很小",
  "location_substring": "亏损概率很小",
  "char_span": {"start": 2340, "end": 2346, "precision": "character"},
  "verdict": "violation",
  "confidence": 0.98,
  "issue_type": "implied_guarantee",
  "severity": "critical",
  "description": "'亏损概率很小'构成暗示保本",
  "law_reference": {
    "article_id": "第24条第1款第2项",
    "law_name": "基金销售管理办法"
  },
  "suggestion": "删除此表述，或修改为：'基金有风险，投资需谨慎。'",
  "suggestion_type": "可采纳修订"
}
```

### 5.2 最终 ReviewResult 结构（合并所有卡片）

```json
{
  "review_id": "REV-2026-00098765",
  "doc_type": "合同",
  "content_type": "采购合同",
  "overall_verdict": "violation",
  "overall_risk_score": 72,
  "risk_level": "high",

  "card_summaries": [
    {
      "card": "① 文本基础核对",
      "issue_count": 2,
      "max_severity": "minor",
      "status": "completed"
    },
    {
      "card": "② 红线与敏感词",
      "issue_count": 0,
      "max_severity": null,
      "status": "completed"
    },
    {
      "card": "③ 形式与要素审查",
      "issue_count": 1,
      "max_severity": "major",
      "status": "completed"
    },
    {
      "card": "④ 语义与合规审查",
      "issue_count": 3,
      "max_severity": "critical",
      "status": "completed"
    },
    {
      "card": "⑤ 逻辑与条款审查",
      "issue_count": 2,
      "max_severity": "major",
      "status": "completed"
    }
  ],

  "issues": [
    {
      "card": "④ 语义与合规审查",
      "segment_index": 15,
      "location_substring": "承担全部损失",
      "char_span": {"start": 2380, "end": 2386},
      "verdict": "violation",
      "severity": "major",
      "description": "违约赔偿无上限约定",
      "law_reference": {
        "article_id": "第584条",
        "law_name": "民法典",
        "original_text": "..."
      },
      "suggestion": "增加赔偿上限条款",
      "suggestion_type": "可采纳修订",
      "citation_status": "verified"
    }
  ],

  "missing_elements": [
    {
      "card": "③ 形式与要素审查",
      "element": "不可抗力条款",
      "severity": "minor",
      "suggestion": "建议增加不可抗力条款"
    }
  ],

  "cross_clause_issues": [
    {
      "card": "⑤ 逻辑与条款审查",
      "clause_ids": ["5.1", "12.3"],
      "issue_type": "timeline_conflict",
      "severity": "major",
      "description": "交付期限30天(5.1条) vs 逾期15天即根本违约(12.3条)，留给乙方的容错空间不合理"
    }
  ]
}
```

### 5.3 风险评分机制

与 v1 一致的两级评分，但 v2 按卡片分别统计：

```
issue 级评分:
  severity    分值
  critical    +30
  major       +20
  minor       +10
  info        +5

  缺失 critical 必备元素:  +25
  缺失 recommended 元素:   +10

  跨条款矛盾 (v2 新增):    +25 (critical级) / +15 (major级)

加权因子:
  citation_status = verified:    × 1.0
  citation_status = corrected:   × 0.8
  citation_status = unverified:  × 0.5

task 级评分:
  overall_risk_score = min(100, Σ(issue_score × citation_weight))

风险等级:
  0-20:   low
  21-50:  medium
  51-100: high
```

---

## 6. 长文档审查策略

v2 保留 v1 的分级策略，增加了 Agent 编排维度。

```
┌──────────────────────────────────────────────────────────────────┐
│  长文档审查策略 (v2)                                               │
│                                                                  │
│  文档 < 5000 字                                                   │
│  → 单次审查: 全文一次性送所有 Agent                                │
│  → 五张卡片 Agent 并行执行                                        │
│  → 不触发 CrossClauseAgent                                       │
│                                                                  │
│  文档 5000 - 30000 字                                             │
│  → 分段审查:                                                     │
│    1. ParserAgent 按章节/段落拆分为 chunks                         │
│    2. 每 chunk 独立经过 ACE 检索 + 卡片 Agent 审查                 │
│    3. chunks 间可并行                                             │
│    4. 审查完成后触发 CrossClauseAgent (合同)                       │
│       或终审一致性检查 (其他长文档)                                 │
│                                                                  │
│  文档 > 30000 字                                                  │
│  → 两阶段审查:                                                   │
│    阶段一 (快速扫描):                                             │
│      · 全文做 ① ② 卡片审查 (不需要 LLM, 速度快)                   │
│      · 全文做向量扫描，标记高风险段落                               │
│    阶段二 (深度审查):                                             │
│      · 仅对高风险段落做 ③ ④ ⑤ 卡片的完整审查                      │
│      · 章节目录级完整性检查 (③ 卡片全文做一次)                      │
│    阶段三 (全局一致性):                                           │
│      · CrossClauseAgent 做全局一致性检查                           │
│                                                                  │
│  分段规则:                                                        │
│    合同文档: 按条款边界切分 (ClauseParserAgent 完成)                │
│    其他文档: 按章节/段落切分, 保留 100 字 overlap                   │
│    单 chunk 上限: 2000 字                                         │
└──────────────────────────────────────────────────────────────────┘
```

### 分段审查的并行编排

```
长文档并行策略:

  文档分为 N 个 chunks

  ┌─────────────────────────────────────────────────┐
  │  chunk 1  chunk 2  chunk 3  ...  chunk N        │
  │     │        │        │               │         │
  │     ▼        ▼        ▼               ▼         │
  │  ┌──────┐ ┌──────┐ ┌──────┐      ┌──────┐     │
  │  │ACE   │ │ACE   │ │ACE   │      │ACE   │     │
  │  │检索  │ │检索  │ │检索  │      │检索  │     │
  │  └──┬───┘ └──┬───┘ └──┬───┘      └──┬───┘     │
  │     │        │        │               │         │
  │     ▼        ▼        ▼               ▼         │
  │  ┌──────┐ ┌──────┐ ┌──────┐      ┌──────┐     │
  │  │①②③④⑤│ │①②③④⑤│ │①②③④⑤│      │①②③④⑤│     │
  │  │Agent │ │Agent │ │Agent │      │Agent │     │
  │  │并行  │ │并行  │ │并行  │      │并行  │     │
  │  └──┬───┘ └──┬───┘ └──┬───┘      └──┬───┘     │
  │     │        │        │               │         │
  │     └────────┴────────┴───────────────┘         │
  │                       │                         │
  │                       ▼                         │
  │              ┌────────────────┐                 │
  │              │ CrossClauseAgent│                 │
  │              │ (全局一致性)    │                 │
  │              └────────┬───────┘                 │
  │                       ▼                         │
  │              ┌────────────────┐                 │
  │              │ PostProcess +  │                 │
  │              │ Assembly       │                 │
  │              └────────────────┘                 │
  └─────────────────────────────────────────────────┘

  每个 chunk 的五张卡片 Agent 并行执行
  N 个 chunks 之间也可并行执行 (受 LLM 并发限制)
  全部 chunk 完成后再执行 CrossClauseAgent
```

---

## 7. Prompt Caching 策略

> v2 新增。回应 Gemini R2：不做 Prompt Caching，100 QPS 场景下 LLM 成本约 200 万美金/年。

### 7.1 可缓存内容分析

```
审查 Prompt 的组成 (以 SemanticComplianceAgent 为例):

  ┌────────────────────────────────────────────────────────────────┐
  │                                                                │
  │  System Prompt (~500 tokens)           ← 极少变化，可缓存       │
  │  ├── 角色设定                                                   │
  │  ├── 审查要求 (1-5)                                             │
  │  └── JSON 输出格式约束                                          │
  │                                                                │
  │  === 适用法规 === (~6000 tokens)       ← 按类型有聚集性，可缓存  │
  │  ├── 路径 A 结果 (按 content_type)     ← 同类文档共享            │
  │  ├── 路径 B 结果 (按 product_type)     ← 同类产品共享            │
  │  └── 路径 C 结果 (按语义)              ← 每次不同，不可缓存      │
  │                                                                │
  │  === 参考案例 === (~1000 tokens)       ← 每次不同，不可缓存      │
  │                                                                │
  │  === 企业规则 === (~500 tokens)        ← 同租户共享，可缓存      │
  │                                                                │
  │  User Prompt (~2000 tokens)            ← 每次不同，不可缓存      │
  │                                                                │
  │  总计: ~10000 tokens input                                      │
  │  其中可缓存: ~4000-7000 tokens (40-70%)                         │
  └────────────────────────────────────────────────────────────────┘
```

### 7.2 缓存分层策略

```
┌──────────────────────────────────────────────────────────────────┐
│  Prompt Caching 三层策略                                          │
│                                                                  │
│  Layer 1: 静态 System Prompt 缓存                                │
│    内容: 角色设定 + 审查要求 + JSON Schema                        │
│    变化频率: 仅在 prompt 模板更新时变化（~每月一次）               │
│    缓存方式: OpenAI/Anthropic Prompt Caching                     │
│    命中率: ~99%                                                  │
│    节省: ~500 tokens × 100 QPS × $2.5/1M = $10.8/天              │
│                                                                  │
│  Layer 2: 结构化法条上下文缓存                                    │
│    内容: 路径 A (content_type) + 路径 B (product_type) 的法条      │
│    缓存 Key: hash(content_type + product_type + tenant_id)       │
│    变化频率: 仅在知识库更新时失效（~每周一次）                      │
│    缓存方式: Redis 缓存组装后的法条文本                             │
│    命中率: ~80% (同类文档高频复用)                                 │
│    节省: ~3000 tokens × 80% × 100 QPS × $2.5/1M = $51.8/天       │
│                                                                  │
│  Layer 3: 企业规则缓存                                            │
│    内容: 路径 E 的租户自定义规则                                   │
│    缓存 Key: hash(tenant_id + rules_version)                     │
│    变化频率: 仅在规则更新时失效（~每月一次）                        │
│    缓存方式: Redis                                                │
│    命中率: ~95%                                                  │
│    节省: ~500 tokens × 95% × 100 QPS × $2.5/1M = $10.3/天        │
│                                                                  │
│  总节省估算:                                                      │
│    不缓存时日成本: ~$576/天 (100QPS × 10K tokens × $2.5/1M × 86400)│
│    缓存后日成本:   ~$230-350/天                                    │
│    年节省:         ~$70,000 - $126,000                             │
│                                                                  │
│  Prompt Caching 技术选型:                                         │
│    OpenAI: 自动 Prompt Caching (相同前缀)                         │
│    Anthropic: 显式 cache_control 标记                             │
│    自建: Redis 缓存组装后的 system prompt 文本                     │
│                                                                  │
│  实现要点:                                                        │
│    · 组装 prompt 时，将可缓存部分放在 system prompt 前部            │
│    · 将每次变化的部分 (路径C/D语义检索结果) 放在后部                │
│    · 确保前缀 stable 以利用 OpenAI 的自动缓存机制                  │
└──────────────────────────────────────────────────────────────────┘
```

### 7.3 Prompt 组装顺序（缓存友好）

```
最终 Prompt 组装顺序 (缓存友好):

  ┌── 稳定前缀 (利用 Prompt Cache) ──────────────────────────┐
  │                                                          │
  │  [System Prompt 静态部分]                                 │
  │  角色设定 + 审查要求 + 输出格式                            │
  │                                                          │
  │  [企业规则 — 同租户稳定]                                   │
  │  === 企业特殊要求 ===                                     │
  │  {tenant_custom_rules}                                   │
  │                                                          │
  │  [结构化法条 — 同文档类型稳定]                              │
  │  === 适用法规 (类型匹配) ===                               │
  │  {path_a_results}                                        │
  │  {path_b_results}                                        │
  │                                                          │
  └──────────────────────────────────────────────────────────┘

  ┌── 动态后缀 (每次不同) ───────────────────────────────────┐
  │                                                          │
  │  [语义检索法条 — 每次变化]                                 │
  │  === 适用法规 (语义匹配) ===                               │
  │  {path_c_results}                                        │
  │                                                          │
  │  [模板 diff — 合同场景变化]                                │
  │  === 与标准模板的差异 ===                                  │
  │  {path_f_results}                                        │
  │                                                          │
  │  [相似案例 — 每次变化]                                     │
  │  === 参考案例 ===                                         │
  │  {path_d_results}                                        │
  │                                                          │
  │  [User Prompt — 每次变化]                                  │
  │  请审查以下内容:                                           │
  │  {segmented_content}                                     │
  │                                                          │
  └──────────────────────────────────────────────────────────┘
```

---

## 8. 模型路由

> v2 新增。回应 Gemini R2：简单内容和复杂内容使用相同模型，造成不必要的成本和延迟。

### 8.1 路由策略

```
┌──────────────────────────────────────────────────────────────────┐
│  模型路由策略                                                      │
│                                                                  │
│  原则: 简单任务用小模型(快+便宜), 复杂任务用大模型(慢+贵)          │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  按 Agent 固定路由 (静态)                                   │ │
│  │                                                            │ │
│  │  Agent              推荐模型        备用模型                │ │
│  │  ─────────────────────────────────────────────────         │ │
│  │  ClassificationAgent  4o-mini        Qwen-Max              │ │
│  │  ClauseParserAgent    4o-mini        Qwen-Max              │ │
│  │  TextBasicsAgent      4o-mini        Qwen-Max              │ │
│  │  FormatElementsAgent  4o-mini / 4o   Qwen-Max              │ │
│  │  SemanticCompliance   4o / Sonnet    Qwen-Max / DeepSeek   │ │
│  │  LogicClauseAgent     4o / Sonnet    Qwen-Max / DeepSeek   │ │
│  │  CrossClauseAgent     4o             Sonnet                │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  按内容复杂度动态路由 (仅 SemanticComplianceAgent)          │ │
│  │                                                            │ │
│  │  复杂度评估因子:                                            │ │
│  │  · 文档长度 (> 5000字 → 复杂)                               │ │
│  │  · 检索到的法条数量 (> 30条 → 复杂)                         │ │
│  │  · 是否为合同文档 (合同 → 复杂)                              │ │
│  │  · 历史同类文档的 LLM 置信度均值 (< 0.75 → 复杂)            │ │
│  │                                                            │ │
│  │  路由规则:                                                  │ │
│  │  complexity_score = 文档长度得分 + 法条数得分                │ │
│  │                     + 合同加分 + 历史置信度得分              │ │
│  │                                                            │ │
│  │  complexity_score < 3  → 4o-mini (快速, ~$0.015/次)         │ │
│  │  complexity_score 3-6  → 4o      (标准, ~$0.067/次)         │ │
│  │  complexity_score > 6  → Sonnet  (深度, ~$0.045/次)         │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  成本对比                                                   │ │
│  │                                                            │ │
│  │  假设流量分布: 简单60% / 标准30% / 复杂10%                  │ │
│  │                                                            │ │
│  │  不路由 (全用4o):                                           │ │
│  │    100% × $0.067 = $0.067/次                               │ │
│  │                                                            │ │
│  │  路由后:                                                    │ │
│  │    60% × $0.015 + 30% × $0.067 + 10% × $0.045              │ │
│  │    = $0.009 + $0.020 + $0.0045 = $0.034/次                 │ │
│  │                                                            │ │
│  │  节省: ~49%                                                 │ │
│  └────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

### 8.2 Fallback 策略

```
模型调用 Fallback 链:

  推荐模型调用
      │
      ├─ 成功 → 返回结果 ✅
      │
      └─ 失败 (3次重试)
            │
            ▼
      备用模型1调用
          │
          ├─ 成功 → 返回结果 ✅ (标记 model_fallback=true)
          │
          └─ 失败 (3次重试)
                │
                ▼
          备用模型2调用 (国内模型, 如 Qwen-Max)
              │
              ├─ 成功 → 返回结果 ✅ (标记 model_fallback=true)
              │
              └─ 失败 → 进入人工审查队列 ⚠️
```

---

## 9. 性能与并发设计

### 9.1 延迟预算（修正版）

> 修正 v1 过于乐观的延迟估计（Gemini R1）

```
单次审查延迟预算 (v2 修正版):

  ClassificationAgent:           100ms - 500ms
  ParserAgent (OCR/文档解析):    500ms - 3s
  QuickFilterAgent:              < 10ms
  ClauseParserAgent (仅合同):    500ms - 2s
  ACE 六路检索 (并行):
    p50:                         400ms - 800ms
    p95:                         800ms - 1500ms     ← v1 低估了
  五张卡片 Agent (并行):
    ① TextBasicsAgent:           1s - 2s
    ② RedlineAgent:              < 5ms
    ③ FormatElementsAgent:       1s - 3s
    ④ SemanticComplianceAgent:   5s - 15s           ← 主要瓶颈
    ⑤ LogicClauseAgent:          3s - 10s
    并行后取 max:                5s - 15s
  CrossClauseAgent (条件):       3s - 8s
  PostProcessAgent:              50ms - 200ms
  AssemblyAgent:                 50ms - 100ms
  ─────────────────────────────────────────
  总计:
    短内容 (< 1000字, 非合同):    8s - 18s
    中等内容 (1000-5000字):       10s - 25s
    合同 (5000-30000字):          15s - 45s  (并行审查多 chunk)
    超长文档 (> 30000字):         30s - 90s  (异步处理)
```

### 9.2 并发架构

```
┌────────────────────────────────────────────────────────────────┐
│  并发架构 (v2)                                                  │
│                                                                │
│  层级1: 请求级并发                                              │
│    Spring Boot 应用 (3+ 实例)                                  │
│    每实例处理并发请求: ~50                                       │
│    集群总并发: ~150 请求                                        │
│                                                                │
│  层级2: Agent 级并发 (单请求内)                                  │
│    五张卡片 Agent 并行执行                                      │
│    LLM 调用使用 Semaphore 控制:                                 │
│      每实例 LLM 并发上限: 80                                    │
│      其中 4o-mini: 50 slots (便宜, 多给)                        │
│      其中 4o:      20 slots                                    │
│      其中 Sonnet:  10 slots                                    │
│                                                                │
│  层级3: Chunk 级并发 (长文档)                                    │
│    长文档拆分后的多个 chunk 并行审查                              │
│    受层级2的 Semaphore 控制                                     │
│    最大并行 chunk 数: 10                                        │
│                                                                │
│  层级4: 异步处理 (超长文档)                                      │
│    文档 > 5000 字 → 提交到 RabbitMQ 异步队列                     │
│    Worker 实例拉取并处理                                        │
│    完成后通过 WebSocket 推送结果                                 │
│    前端: 轮询 + WebSocket 双通道                                │
│                                                                │
│  排队策略:                                                      │
│    LLM Semaphore 已满 → 排队等待 (最长 30s)                      │
│    排队超时 → 返回"系统繁忙, 请稍后重试"                          │
│    超时请求加入延迟队列, 5分钟后自动重试                          │
└────────────────────────────────────────────────────────────────┘
```

### 9.3 性能目标

| 指标 | 目标 | 备注 |
|------|------|------|
| 短内容审查延迟（< 1000字, 非合同） | < 20s | 同步返回 |
| 中等内容审查延迟（1000-5000字） | < 30s | 同步返回 |
| 合同审查延迟（5000-30000字） | < 60s | 异步处理 + WebSocket |
| 超长文档审查延迟（> 30000字） | < 120s | 异步处理 |
| 快速过滤延迟（①②卡片） | < 50ms | Aho-Corasick + NLP |
| ACE 六路检索 p95 | < 1500ms | 六路并行 |
| 系统并发能力 | 50+ QPS | LLM API 为主瓶颈 |
| 定位准确率（字符级） | > 85% | 降级率 < 15% |
| Prompt Cache 命中率 | > 70% | Layer 1 ~99%, Layer 2 ~80% |

### 9.4 监控指标

```
核心监控指标:

  业务指标:
  · 各卡片 Agent 的延迟 p50/p95/p99
  · 各卡片的 issue 发现率 (每次审查平均发现问题数)
  · LLM 调用成功率 (按模型分)
  · JSON 解析成功率
  · 定位降级率 (字符级 → 段落级)
  · Prompt Cache 命中率
  · 模型路由分布 (4o-mini / 4o / Sonnet 各占比)

  系统指标:
  · LLM Semaphore 使用率
  · RabbitMQ 队列深度
  · Milvus 检索延迟
  · 各 Agent 的 token 消耗统计
  · fallback 触发率 (模型切换频率)

  告警阈值:
  · LLM 调用失败率 > 5% → 红色告警
  · p95 延迟 > 60s (短内容) → 黄色告警
  · Prompt Cache 命中率 < 50% → 黄色告警 (可能缓存配置有问题)
  · 定位降级率 > 20% → 黄色告警 (需优化 prompt)
  · 模型 fallback 率 > 10% → 黄色告警 (主模型可能有问题)
```
