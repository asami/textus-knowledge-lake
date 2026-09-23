# TKL Application Model — Initial CML Draft

- Date: 2026-09-23
- Status: Initial model
- Scope: Application Use Cases + Phase 1 Application Workflow

## Modeling intent

TKL の Use Case と Workflow を分離して CML で扱う。

- Use Case: 利用者が TKL を使って達成したい目的
- Application Capability: Use Case を実現するアプリケーション能力
- Workflow: Capability を協調させて目的を遂行する実行モデル

Phase 1 では全 Use Case を実装しない。Application Model として将来用途まで定義し、最初の Use Case Slice として KnowledgeHub への Knowledge Candidate Proposal を end-to-end で実装する。

## Actors

- Knowledge Worker — Knowledge Candidate の提案、確認、修正、Admission を行う利用者
- KnowledgeHub — Candidate を受け取り Admission / Knowledge 管理を行う downstream system
- Resource Provider — Google Workspace、Slack 等の Evidence provider
- Knowledge Processor — LLM / deterministic processor / KnowledgeHub-oriented processor

TEAI / OpenClaw は原則として Actor ではなく integration mechanism とする。

## Application Use Cases

### Knowledge Acquisition

#### UC-KA-01 Propose Knowledge Candidate — Phase 1 Primary

Goal:
分散した Evidence から KnowledgeHub に登録する価値のある Knowledge Candidate を抽出し、Evidence とともに利用者へ提案する。

Primary Actor:
Knowledge Worker

Supporting Actors:
KnowledgeHub, Resource Provider, Knowledge Processor

Main scenario:

1. Knowledge Worker が知識候補の提案を要求する。
2. TKL が目的と Context に基づいて Knowledgeization Scope を構成する。
3. TKL が Drive / Gmail / Slack 等の Resource を解決する。
4. TKL が必要な Evidence を収集・正規化する。
5. TKL が PreparedMaterial を生成する。
6. Knowledge Processor が PreparedMaterial から Knowledge Candidate を抽出する。
7. TKL が既存 KnowledgeHub Knowledge との関係を確認する。
8. TKL が Candidate、Evidence、Context、差分/新規性、未解決事項を Proposal として提示する。
9. Knowledge Worker が Proposal を確認し、必要なら修正する。
10. Knowledge Worker が Candidate を KnowledgeHub Admission へ送る。

Postconditions:

- Knowledge Candidate が Evidence-grounded な Proposal として存在する。
- Candidate から PreparedMaterial と canonical Evidence まで provenance を追跡できる。
- 承認された Candidate を KnowledgeHub Admission 境界へ渡せる。

### Knowledge Acquisition supporting use cases

- UC-KA-02 Explore Candidates by Theme — テーマを指定して Knowledge Candidate を探索する
- UC-KA-03 Discover Candidates from New Evidence — 新着 Evidence から候補を発見する
- UC-KA-04 Inspect Candidate Evidence — Candidate の根拠を確認する
- UC-KA-05 Compare Candidate with Existing Knowledge — 既存 Knowledge と比較する
- UC-KA-06 Revise Candidate — Candidate を修正・補足する
- UC-KA-07 Submit Candidate for Admission — KnowledgeHub Admission へ送る
- UC-KA-08 Hold or Reject Candidate — Candidate を保留・棄却する

### Research / Exploration

- UC-RE-01 Research Theme Across Sources
- UC-RE-02 Trace Historical Context
- UC-RE-03 Compare Views
- UC-RE-04 Analyze Consensus
- UC-RE-05 Extract Open Questions
- UC-RE-06 Answer from Evidence

These may terminate at PreparedMaterial or research artifacts without producing KnowledgeHub candidates.

### Project Awareness

- UC-PA-01 Review Recent Changes
- UC-PA-02 Review New Evidence
- UC-PA-03 Identify Active Discussions
- UC-PA-04 Review Open Issues
- UC-PA-05 Generate Project Digest
- UC-PA-06 Notify Significant Change

### Content Production

- UC-CP-01 Generate Report
- UC-CP-02 Generate Slide Deck
- UC-CP-03 Generate Infographic
- UC-CP-04 Generate Mind Map
- UC-CP-05 Generate Audio / Video
- UC-CP-06 Generate LLM Context

Gemini Notebook is one optional provider for these production use cases; it is not required for KnowledgeHub acquisition.

## Application Capabilities

Initial capabilities derived from the primary use case:

- Resource Resolution
- Evidence Discovery
- Scope Assembly
- Evidence Normalization
- Communication Normalization
- Resource Materialization
- Knowledge Preparation
- Prepared Material Management
- Candidate Extraction
- Existing Knowledge Comparison
- Candidate Proposal
- Candidate Review
- Candidate Submission
- Provenance / Lineage Resolution

## Phase 1 Application Workflow

### WF-KA-01 Knowledge Candidate Proposal Workflow

Realizes:
UC-KA-01 Propose Knowledge Candidate

```text
Start
  |
  v
Determine Knowledgeization Scope
  |
  v
Resolve Knowledge Resources
  |
  v
Collect Evidence
  |
  v
Normalize Evidence
  |
  +--> Communication VO for Gmail / Slack
  |
  v
Prepare Knowledge Material
  |
  v
Persist PreparedMaterial
  |
  v
Extract Knowledge Candidate
  |
  v
Compare Existing Knowledge
  |
  v
Compose Candidate Proposal
  |
  v
Human Review
  |
  +--> revise --> Compose Candidate Proposal
  |
  +--> hold/reject --> End
  |
  v
Submit to KnowledgeHub Admission
  |
  v
End
```

## Phase 1 Use Case Slice

The first implementation slice closes UC-KA-01 with deterministic fixtures before full provider automation.

Given:

- Drive-like file Evidence
- Gmail-like Communication Evidence
- Slack-like Communication Evidence
- existing KnowledgeHub Knowledge fixture

When:

- a Knowledge Worker requests a Knowledge Candidate Proposal for a theme

Then:

- provider resources are represented by stable TKL resource IDs;
- communication data is normalized into common Communication VOs;
- a PreparedMaterial Entity is created and persisted;
- PreparedMaterial can be exported losslessly as JSON;
- Markdown can be rendered from the canonical model without re-reading sources;
- an attachment-bearing PreparedMaterial can be exported as KAR;
- at least one Knowledge Candidate Proposal is generated;
- the Proposal includes provenance to its Evidence;
- existing Knowledge can be compared;
- the accepted Candidate can cross the KnowledgeHub Admission boundary.

## Provider mapping for the NICT pilot

This mapping is implementation-specific and not part of the CML semantic model.

```text
Google Drive        -> File Resource Provider
Gmail               -> Mail Communication Provider
Slack               -> Conversation Provider
Drive Project       -> Saved context for Gemini in Drive preparation
Gemini in Drive     -> Preparation processor
Gemini Notebook     -> Optional content/presentation producer
KnowledgeHub        -> Candidate admission consumer
```

## CML implementation direction

The next step is to encode the above as native CML model elements rather than keeping only this textual draft.

Expected relationships:

```text
UseCase
  realizes goal
  requires ApplicationCapability*

ApplicationWorkflow
  realizes UseCase
  invokes ApplicationCapability*

ApplicationCapability
  is provided by Component/Operation/Workflow capability

UseCaseSlice
  selects scenario path + executable specification
```

The exact CML syntax should follow the current SimpleModeling/CML definitions and should not be invented locally in TKL.


## Knowledge Workbench boundary (2026-09-23)

The previous Phase 1 wording that TKL sends a reviewed Candidate directly to KnowledgeHub is superseded.

TKL performs discovery and preparation and produces a **raw knowledge candidate / candidate proposal**. Human formation, editing and approval belong to a reusable Knowledge Workbench layer.

```text
Evidence
  -> TKL
  -> PreparedMaterial
  -> Raw Knowledge Candidate
  -> Knowledge Workbench
  -> KnowledgeFormationProposal
  -> Human Approval
  -> KnowledgeHub
```

For the NICT project, TKL therefore integrates primarily with the Knowledge Workbench candidate boundary rather than directly with KnowledgeHub Admission.

The Phase 1 primary use case remains candidate proposal, but its postcondition is now: the proposal can be handed to Knowledge Workbench with complete evidence/provenance. Direct KnowledgeHub admission is not a TKL responsibility.
