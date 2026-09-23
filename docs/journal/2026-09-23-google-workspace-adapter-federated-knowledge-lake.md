# Google Workspace Adapter and Federated Knowledge Lake

- Date: 2026-09-23
- Status: Architecture investigation / design direction

## Decision

Textus Knowledge Lake (TKL) は Google Workspace の wrapper ではない。

TKL が Textus における Knowledge Lake の論理モデル、operations、provenance、workflow を定義し、Google Workspace は provider / adapter の一つとして実装する。

Google Workspace 内の各 AI 機能は有用だが、Drive Project、Gemini in Drive、Gemini Notebook、Gmail 等は Knowledge 処理の観点で完全に統一されていない。また Slack 等の外部 Conversation Evidence も存在する。

TKL はこの差異を吸収する。

## Core logical concepts

初期候補として次を置く。

- Evidence
- EvidenceReference
- SourceSet
- PersistentReasoningContext
- DynamicContext
- KnowledgeWorkspace
- PreparationView
- KnowledgeizationRequest
- KnowledgeizationScope
- Artifact
- ContextArtifact
- KnowledgeCandidate
- Provenance / Lineage
- Admission

Google 製品名を core model に持ち込まない。

## Federated Evidence Store

Knowledge Lake は巨大な単一 physical storage ではない。

Canonical Evidence は元の system of record に保持する。

- Google Drive: file evidence
- Gmail: mail / attachment evidence
- Slack: channel / thread / message evidence
- Web / other systems: external evidence

必要な Workspace が canonical source を直接扱えない場合だけ materialized representation を生成する。

## Materialization

TKL の重要 operation として materialization を導入する。

例:

```text
Gmail Thread
  -> materializeForWorkspace()
  -> Google Doc
  -> Gemini Notebook Source

Slack Thread
  -> materializeForWorkspace()
  -> Context Artifact / Google Doc
  -> Drive Project and/or Gemini Notebook
```

materialized file は canonical evidence の replacement ではない。TKL は source identity、source location、generated time、purpose、transformation、provenance / lineage を保持する。

## Google Workspace mapping

### Google Drive folder

Physical file store として利用する。

NICT pilot:

```text
NICT KnowledgeHub/
├── 00_Inbox
├── 10_Evidence
├── 20_Artifacts
└── 90_Archive
```

directory は workflow state ではなく resource lifecycle / storage role を表す。

### Drive Project

PersistentReasoningContext の Google implementation 候補。

Drive files / folders から、Gemini が日常的に reasoning する際に通常参照すべき Source Set を curate する。

全 Evidence を入れるのではなく active reasoning context を選択する。

### Gemini in Drive

Reasoning / research execution environment として扱う。Drive Project とは別機能であり、必要に応じて Gmail 等の dynamic context を reasoning 時に追加できる。

### Gemini Notebook

KnowledgeWorkspace / PreparationView の Google implementation 候補。

Source を file 単位で登録する必要があるため、TKL が論理 SourceSet と Notebook Sources の同期を担当する。

Notebook は source-grounded reasoning に加え、Report、Mind Map、Slide Deck、Infographic、Audio、Video 等の Derived Artifact / Presentation を生成できる。

Drive Project と Notebook は単純な親子関係にしない。暫定的には:

- Drive Project: broad persistent conversational reasoning context
- Gemini Notebook: curated viewpoint-oriented synthesis / presentation workspace

として実運用で差を検証する。

### Gmail

MailEvidence provider。

KnowledgeHub 対象通信は label 等で識別する。直接利用可能な reasoning environment では native reference を優先し、Notebook 等で利用できない場合のみ materialize する。

### Slack

ConversationEvidence provider。

Slack は canonical conversation store とし、Google Workspaceへ全ログを複製しない。Knowledgeization / reasoning に必要な channel / thread / message scope を TKL が選択し Context Artifact へ materialize する。

## Adapter responsibility

GoogleWorkspaceAdapter は API wrapper だけでなく、Google 製品間の capability gap を吸収する。

想定 operations:

- captureFileEvidence
- identifyMailEvidence
- identifyConversationEvidence
- selectSourceSet
- admitToReasoningContext
- removeFromReasoningContext
- materializeForWorkspace
- syncKnowledgeWorkspaceSources
- collectDerivedArtifacts
- recordProvenance
- startKnowledgeization
- publishKnowledgeCandidate

operation names は provisional。

## Relation with TEAI and OpenClaw

TKL は Knowledge Lake semantics と orchestration を担当する。

TEAI / OpenClaw は physical integration / execution を担当する。

```text
TKL
  Knowledge Lake semantics
  Source / Scope / Candidate / Provenance
        |
        v
Google Workspace Adapter / Slack Adapter
        |
        v
TEAI + OpenClaw
        |
        v
Google APIs / Slack APIs / external systems
```

Google native capability が十分な場合は native reference / operation を利用し、欠落する capability のみ copy / conversion / automation で補完する。

## Knowledgeization operating model

```text
Evidence Arrival
      |
      v
Canonical Evidence Store
      |
      +--> persistent reasoning context (optional)
      |
Knowledgeization Request
      |
      v
Dynamic Scope Assembly
      |
      +--> native sources
      +--> materialized sources
      +--> prepared workspace sources
      |
      v
AI Knowledge Work
      |
      v
Knowledge Candidate
      |
   Review / Admission
      |
      v
Textus KnowledgeHub
```

Evidence Arrival と Knowledgeization Start は分離する。Knowledgeization Scope は physical directory / provider / notebook の構成から独立し、要求時に自由に構成できる。

## Architectural principle

TKL の目的は各 SaaS の機能を再実装することではない。

各 provider の native knowledge capability を最大限利用しながら、provider 間で異なる source model、reasoning context、workspace capability、materialization requirement を Textus の統一 Knowledge Lake model の下に収める。

Google Workspace は最初の reference implementation / pilot とするが、core model は Google 固有概念に依存させない。
