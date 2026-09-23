# Knowledge Lake Architecture

## Current decision

**Textus Knowledge Lake (TKL) itself is the Knowledge Lake.**

Google Workspace, Slack, Gmail, Web and future stores are providers/resources used by TKL. TKL defines the provider-neutral resource identity, evidence model, preparation model, lifecycle, provenance and processing boundary.

The previous assumption that Google Workspace itself is the Knowledge Lake is superseded.

## Core pipeline

```text
External Resources
Drive / Gmail / Slack / Web / ...
        |
        v
TKL Resource Registry
        |
        v
Evidence
        |
        v
Preparation
        |
        v
PreparedMaterial Entity
        |
        +--> KnowledgeHub Processor -> KnowledgeCandidate -> Admission
        +--> LLM Context -> ChatGPT / Gemini / Codex / local LLM
        +--> Presentation -> Gemini Notebook / other producers
```

Preparation is a core TKL capability. Downstream processing and presentation are purpose-specific and replaceable.

## Federated resource model

External resources are referenced by default rather than copied.

Design principle:

> Reference by default, materialize when necessary.

TKL assigns a stable provider-independent resource identity, for example:

```text
textus:klake:kr:<id>
```

A KnowledgeResource Entity resolves that identity to one or more provider representations.

```text
KnowledgeResource
  id: textus:klake:kr:...
  representations:
    - provider: google-drive
      externalId: ...
    - provider: slack
      externalId: ...
    - provider: local/materialized
      ...
```

Provider-specific IDs and locations must not leak into PreparedMaterial as canonical identity.

Materialization is used when an immutable snapshot is required, a target workspace cannot access the original provider, a KAR must embed a resource, or policy requires local retention.

## Provider roles

### Google Drive

File/resource provider and a convenient preparation environment.

NICT pilot physical structure:

```text
NICT KnowledgeHub/
├── 00_Inbox
├── 10_Evidence
├── 20_Artifacts
└── 90_Archive
```

This structure represents storage role/resource lifecycle, not knowledge-processing workflow state.

### Gmail

Mail Evidence provider. Relevant communications are identified with labels. Mail remains canonical in Gmail unless materialization is required.

### Slack

Conversation Evidence provider. Channel/thread/message data remain canonical in Slack. Relevant scopes may be projected/materialized into Drive for Gemini processing.

### Gemini in Drive / Drive Project

Gemini in Drive is used primarily for **Preparation**.

Drive Project is a saved/persistent Drive context for Gemini in Drive: files and folders that should normally be in reasoning scope can be registered in advance. Additional resources such as Gmail can be introduced during reasoning.

For Slack or other unsupported providers, TKL imports a normalized representation into Drive first.

### Gemini Notebook

Gemini Notebook is optional and is not part of the mandatory preparation pipeline.

Its strength is production/presentation from an explicit file-level Source Set: reports, Mind Maps, Slide Decks, Infographics, Audio, Video, etc.

TKL may synchronize selected files into a Notebook when such output is useful. KnowledgeHub-oriented processing does not need to pass through Gemini Notebook.

## Communication model

Communication data from Gmail, Slack and similar providers is normalized into provider-independent Value Objects before import or processing.

Initial model direction:

```text
Communication
├─ Kind
├─ Subject
├─ Context
├─ Participants
│  └─ Participant
├─ Messages
│  └─ Message
│     ├─ Sender
│     ├─ Recipients
│     ├─ Timestamp
│     ├─ Content
│     ├─ ReplyTo
│     └─ AttachmentReference*
├─ SourceReference
└─ Provenance
```

Provider-specific fields such as Gmail thread IDs and Slack channel/thread IDs belong in provider references/metadata.

The VO can be serialized losslessly to JSON. For Google preparation, the Google Workspace Adapter may render it as Google Docs instead when that gives Gemini better reasoning quality.

The choice JSON vs Google Docs is an adapter/policy decision and should be evaluated with fixtures.

## PreparedMaterial

PreparedMaterial is a TKL-managed **Entity**, not merely an external document.

It has a stable ID and lifecycle and is persisted in the TKL database.

Conceptual structure:

```text
PreparedMaterial
├─ PreparedMaterialId
├─ Version
├─ Purpose
├─ Context
├─ Sources
├─ Sections / Content
├─ EvidenceReferences
├─ Resources
├─ Provenance
├─ PreparationHistory
└─ Status
```

PreparedMaterial references Evidence through TKL resource IDs such as `textus:klake:kr:...`.

The TKL domain model is authoritative. Serialization formats are mappings of that model.

## PreparedMaterial serialization

### JSON

JSON is the canonical lossless interchange/export representation.

It must contain all semantic and structural information needed to produce the LLM/human Markdown projection without re-reading external sources.

### Markdown

Markdown is generated from PreparedMaterial for LLM/human consumption. It is a projection, not the system of record.

### KAR — Knowledge Archive

When PreparedMaterial needs embedded resources, export it as a **KAR (Knowledge Archive)**.

KAR is a ZIP-compatible package, for example:

```text
example.kar
├── prepared-material.json
├── META-INF/
│   └── manifest.json
└── resources/
    ├── source.pdf
    └── image.png
```

If there are no embedded resources, plain `*.json` is sufficient.

JSON and KAR represent the same logical PreparedMaterial model; KAR adds packaged resources.

## Knowledge processing

PreparedMaterial is the provider-neutral intermediate representation between Preparation and purpose-specific processing.

```text
PreparedMaterial
    |
    +--> Markdown -> LLM reasoning
    |
    +--> KnowledgeHub Processor
    |       -> Information/Knowledge candidates
    |
    +--> Presentation Processor
            -> Gemini Notebook / Slide / Audio / Video / ...
```

KnowledgeHub processing should directly map PreparedMaterial into KnowledgeHub-oriented candidates; Gemini Notebook is not required.

## Integration boundary

TKL owns Knowledge Lake semantics and orchestration.

Provider adapters map TKL concepts to provider capabilities.

TEAI/OpenClaw may perform physical integration and external actions where appropriate.

```text
TKL Domain / Workflow
       |
Provider Adapters
       |
TEAI / OpenClaw where needed
       |
Google / Slack / other external APIs
```

Provider-native capabilities should be used when available; TKL supplies missing federation, synchronization, normalization, materialization and provenance semantics.

## Design principles

1. TKL is the Knowledge Lake.
2. External systems are providers, not the TKL domain model.
3. Reference external resources by default; copy/materialize only when necessary.
4. TKL issues stable provider-independent resource identities.
5. Preparation is mandatory/core; downstream processing is purpose-specific.
6. PreparedMaterial is the central provider-neutral IR and a persisted Entity.
7. TKL domain objects are authoritative; JSON/KAR/Markdown are mappings/projections.
8. Communication is normalized through provider-independent Value Objects.
9. JSON is lossless; Markdown is generated for LLM/human use.
10. KAR packages PreparedMaterial plus embedded resources.
11. Google Workspace is the initial reference provider, not an architectural dependency.
12. Provenance/lineage must remain traceable from downstream knowledge to canonical external evidence.

## Knowledge feedback loop

TKL must prepare candidates against the **current KnowledgeHub knowledge context**, not from new Evidence alone.

Introduce a provider-neutral Knowledge feedback/projection path:

```text
KnowledgeHub canonical Knowledge
        |
        v
TKL KnowledgeReference / KnowledgeProjection
        |
        +--> TKL preparation context
        |
        +--> Google Workspace projection
                  |
             Google Docs/files
                  |
             Drive Project
                  |
             Gemini in Drive
                  |
          Existing Knowledge + New Evidence
                  |
             Preparation
                  |
             PreparedMaterial
                  |
          Raw Knowledge Candidate
```

KnowledgeHub remains canonical. TKL/Google copies are projections, never authoritative Knowledge.

KnowledgeProjection should contain enough semantic context for preparation, for example knowledge identity/version, content/summary, concepts, relations, context and provenance summary. Exact schema remains provisional.

Synchronization should be incremental/version-aware where practical. Changed or superseded Knowledge updates its TKL projection and any Google Workspace representation.

Design goal: candidate discovery becomes a comparison/interaction between **Existing Knowledge and New Evidence**, enabling new/update/support/conflict/relation proposals rather than repeated rediscovery of existing Knowledge.
