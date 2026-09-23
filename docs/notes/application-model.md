# TKL Application Model — Initial CML Draft

- Date: 2026-09-23
- Status: Initial model
- Scope: Application Use Cases + Phase 1 Application Workflow

## Modeling intent

TKL の Use Case と Workflow を分離して CML で扱う。

- Use Case: 利用者が TKL を使って達成したい目的
- Application Capability: Use Case を実現するアプリケーション能力
- Workflow: Capability を協調させて目的を遂行する実行モデル

Phase 1 では全 Use Case を実装しない。Application Model として将来用途まで定義し、最初の Use Case Slice として Evidence から Raw Knowledge Candidate を提案し、Textus Knowledge Workbench へ引き渡す経路を end-to-end で実装する。

## Actors

- Knowledge Worker — Knowledge Candidate の探索・提案を要求し、その根拠を確認する利用者
- Textus Knowledge Workbench — Raw Knowledge Candidate を受け取り、Formation / Edit / Review / Approval を行う downstream application
- Resource Provider — Google Workspace、Slack 等の Evidence provider
- Knowledge Processor — LLM / deterministic processor / KnowledgeHub-oriented processor

TEAI / OpenClaw は原則として Actor ではなく integration mechanism とする。

## Application Use Cases

### Knowledge Acquisition

#### UC-KA-01 Propose Raw Knowledge Candidate — Phase 1 Primary

Goal:
分散した Evidence から Knowledge として形成する価値のある Raw Knowledge Candidate を抽出し、Evidence とともに提案して Textus Knowledge Workbench へ引き渡す。

Primary Actor:
Knowledge Worker

Supporting Actors:
Textus Knowledge Workbench, Resource Provider, Knowledge Processor

Main scenario:

1. Knowledge Worker が知識候補の提案を要求する。
2. TKL が目的と Context に基づいて Knowledgeization Scope を構成する。
3. TKL が Drive / Gmail / Slack 等の Resource を解決する。
4. TKL が必要な Evidence を収集・正規化する。
5. TKL が PreparedMaterial を生成する。
6. Knowledge Processor が PreparedMaterial から Knowledge Candidate を抽出する。
7. TKL が Candidate、Evidence、Context、rationale、未解決事項を Raw Knowledge Candidate Proposal として構成する。
8. Knowledge Worker が Proposal と Evidence を確認する。
9. TKL が Raw Knowledge Candidate と PreparedMaterial / Evidence provenance を Textus Knowledge Workbench へ引き渡す。
10. Candidate の編集、Semantic Mapping、既存 Knowledge との比較、Human Review / Approval、KnowledgeHub Formation は Workbench 側で行う。

Postconditions:

- Knowledge Candidate が Evidence-grounded な Proposal として存在する。
- Candidate から PreparedMaterial と canonical Evidence まで provenance を追跡できる。
- Raw Knowledge Candidate を完全な PreparedMaterial / Evidence provenance とともに Textus Knowledge Workbench 境界へ渡せる。

### Knowledge Acquisition supporting use cases

- UC-KA-02 Explore Candidates by Theme — テーマを指定して Knowledge Candidate を探索する
- UC-KA-03 Discover Candidates from New Evidence — 新着 Evidence から候補を発見する
- UC-KA-04 Inspect Candidate Evidence — Candidate の根拠を確認する
- UC-KA-05 Deliver Candidate to Knowledge Workbench — Raw Candidate を Workbench へ引き渡す

Candidate の既存 Knowledge 比較、修正、Human Review、Approval、Admission は Textus Knowledge Workbench の Use Case とする。

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
- Raw Candidate Proposal
- Candidate Delivery to Knowledge Workbench
- Provenance / Lineage Resolution

## Phase 1 Application Workflow

### WF-KA-01 Raw Knowledge Candidate Proposal Workflow

Realizes:
UC-KA-01 Propose Raw Knowledge Candidate

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
Extract Raw Knowledge Candidate
  |
  v
Compose Raw Candidate Proposal
  |
  v
Present Candidate + Evidence
  |
  v
Deliver to Textus Knowledge Workbench
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

When:

- a Knowledge Worker requests a Knowledge Candidate Proposal for a theme

Then:

- provider resources are represented by stable TKL resource IDs;
- communication data is normalized into common Communication VOs;
- a PreparedMaterial Entity is created and persisted;
- PreparedMaterial can be exported losslessly as JSON;
- Markdown can be rendered from the canonical model without re-reading sources;
- an attachment-bearing PreparedMaterial can be exported as KAR;
- at least one Raw Knowledge Candidate Proposal is generated;
- the Proposal includes provenance to its Evidence and PreparedMaterial;
- the Raw Candidate can cross the Textus Knowledge Workbench boundary;
- Candidate formation/review/approval is explicitly outside TKL Phase 1.

## Provider mapping for the NICT pilot

This mapping is implementation-specific and not part of the CML semantic model.

```text
Google Drive        -> File Resource Provider
Gmail               -> Mail Communication Provider
Slack               -> Conversation Provider
Drive Project       -> Saved context for Gemini in Drive preparation
Gemini in Drive     -> Preparation processor
Gemini Notebook     -> Optional content/presentation producer
Textus Knowledge Workbench -> Raw Candidate formation/review/approval consumer
KnowledgeHub        -> downstream Knowledge runtime reached through Workbench
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

## Existing Knowledge feedback use case

Raw Candidate discovery must use existing Knowledge context when relevant.

Add supporting use case:

- **UC-KA-06 Explore Candidates Against Existing Knowledge** — synchronize/resolve relevant KnowledgeHub Knowledge and use it as preparation context when extracting Raw Knowledge Candidates.

Workflow refinement:

```text
Determine Scope
 -> Resolve Evidence
 -> Resolve Existing Knowledge Context
 -> Normalize / Project Context
 -> Prepare (Existing Knowledge + New Evidence)
 -> PreparedMaterial
 -> Raw Candidate Proposal
 -> Knowledge Workbench
```

The Google reference implementation projects relevant existing Knowledge into Drive/Drive Project so Gemini in Drive can perform preparation with both the current Knowledge baseline and new Drive/Gmail/Slack Evidence.


## Knowledge Feedback application use cases

Knowledge Feedback is a first-class application-use-case group, not merely an internal synchronization workflow.

### Existing use-case impact

Existing TKL use cases must reason against current Knowledge when relevant.

- **UC-KA-01 Propose Raw Knowledge Candidate** — preparation uses Existing Knowledge Context + New Evidence. Proposal should identify related/baseline Knowledge and characterize the observed difference where possible: new, update, support, conflict, or relation.
- **UC-RE-01 Research Theme Across Sources** — include relevant current Knowledge as a research context/source.
- **UC-RE-02 Trace Historical Context** — trace current Knowledge back through Candidate/PreparedMaterial/Evidence where available.
- **UC-RE-03 Compare Views** — compare Evidence-derived views with current Knowledge as well as with each other.
- **UC-RE-04 Analyze Consensus** — compare current admitted Knowledge with consensus visible in recent Evidence.
- **UC-RE-05 Extract Open Questions** — check whether an apparent open question is already resolved by current Knowledge.
- **UC-PA-01 Review Recent Changes** — distinguish ordinary resource change from change that may affect current Knowledge.
- **UC-PA-06 Notify Significant Change** — prioritize Evidence that supports, updates, conflicts with, or creates relations to current Knowledge.

### UC-KF-01 Synchronize Knowledge Context

Goal:
Keep TKL's preparation context aligned with current canonical KnowledgeHub Knowledge.

Main scenario:

1. Detect or request changed/relevant Knowledge.
2. Resolve Knowledge identity and version.
3. Create/update provider-neutral KnowledgeReference / KnowledgeProjection.
4. Store/update the projection in TKL.
5. Mark superseded/stale projections as appropriate.
6. Make the current projection available to Preparation.

### UC-KF-02 Project Knowledge to Preparation Workspace

Goal:
Make relevant KnowledgeProjection content usable by an external preparation processor.

Google reference scenario:

```text
TKL KnowledgeProjection
 -> Google Workspace Adapter
 -> Google Docs/files
 -> Drive Project
 -> Gemini in Drive
```

The external representation is not authoritative Knowledge.

### UC-KF-03 Refresh Knowledge Context after Formation

Goal:
Close the acquisition loop after a candidate is formed/approved.

```text
TKL Candidate
 -> TKW
 -> KnowledgeHub Formation
 -> formed Knowledge identity/version
 -> TKL KnowledgeProjection refresh
 -> next Preparation
```

### UC-KF-04 Trace Knowledge Formation

Goal:
Allow a Knowledge Worker to trace formed Knowledge back through formation/candidate/preparation to canonical Evidence where available.

Expected trace:

```text
Knowledge
 -> FormationProposal / Candidate
 -> PreparedMaterial
 -> EvidenceReference
 -> canonical provider resource
```

### Feedback capabilities

Initial capabilities:

- Knowledge Change Detection
- Knowledge Reference Resolution
- Knowledge Projection
- Knowledge Context Synchronization
- Preparation Workspace Projection
- Projection Version/Staleness Management
- Formation Trace Resolution
