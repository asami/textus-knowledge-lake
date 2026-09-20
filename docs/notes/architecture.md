# Knowledge Lake Architecture

## Purpose

TKL provides the application semantics for turning heterogeneous project material into curated knowledge. It deliberately separates **where working material lives** from **how knowledge is represented and published**.

Google Workspace is initially used as the physical/operational Knowledge Lake. TKL supplies the logical acquisition and curation layer above it.

## Responsibility boundaries

### Source / Knowledge Lake provider

Stores working material and remains useful independently of TKL. The initial provider is Google Workspace.

Examples include documents, spreadsheets, PDFs, images, notes, mail-derived material and intermediate artifacts.

### TEAI

Provides enterprise integration infrastructure:

- source connectivity and endpoint adaptation;
- source events and normalization;
- Integration Binding and trigger policy;
- CNCF Job/Workflow initiation;
- delivery/retry/idempotency where required;
- OpenClaw/agent delegation through Continuation Protocol;
- integration audit/provenance.

TKL should not reproduce this infrastructure.

### TKL

Owns knowledge-lake application semantics:

- Source and Collection concepts;
- Material/Asset identity;
- acquisition state;
- classification and enrichment state;
- provenance from source material;
- curation decisions;
- relationship to KnowledgeHub objects;
- publication policy and status;
- human review where required.

### KnowledgeHub

Owns knowledge representation and processing after material has crossed the curation boundary: semantic representation, linking, retrieval, inference/knowledge processing and knowledge services.

### BoK

Represents curated knowledge products. BoK is not a raw mirror of the Knowledge Lake.

## Candidate domain concepts

Initial concepts to refine during implementation:

- `KnowledgeSource`
- `Collection`
- `Material`
- `AssetReference`
- `Acquisition`
- `Classification`
- `Enrichment`
- `Curation`
- `Publication`
- `Provenance`

These are conceptual names, not yet implementation classes.

## Lifecycle

```text
Raw -> Captured -> Classified -> Enriched -> Curated -> Published
```

The lifecycle should allow rejection, deferral, reclassification and re-curation. Publication is a decision, not an automatic consequence of acquisition.

## Workflow model

A typical workflow is:

1. receive source event through TEAI;
2. acquire or reference the material;
3. establish identity/checksum/source metadata;
4. detect duplicate or existing material;
5. classify;
6. enrich metadata/concepts/relationships;
7. review/curate;
8. publish selected knowledge into KnowledgeHub/BoK;
9. retain provenance linking the publication back to source material.

Deterministic steps should use CNCF Operations. Non-deterministic classification or knowledge work may use JudgmentAction or delegation through Continuation Protocol.

## AI and OpenClaw

TKL should describe AI work in knowledge-domain terms, not provider/tool terms.

For example:

```text
Goal: ClassifyMaterial(materialRef, classificationContext)
```

rather than encoding a sequence of LLM/tool calls.

TEAI can delegate the goal to OpenClaw. OpenClaw can choose the AI/tool interaction necessary to produce a result. TKL receives a normalized result and continues the workflow.

This keeps the TKL model stable when AI providers and external tools change.

## Google Workspace

Google Workspace is the initial provider because it can act as the project's operational data/knowledge lake while retaining familiar collaboration workflows.

The first implementation should focus on a narrow provider slice rather than attempting to cover all Workspace services. Google Drive-based material acquisition is the natural first reference path; Gmail-derived or other Workspace material can be added through TEAI as later scenarios.

## Design principles

1. Lake material is not automatically knowledge.
2. Publication into KnowledgeHub/BoK is an explicit curation boundary.
3. Source and provenance must remain traceable.
4. Provider-specific APIs belong below the TKL application model.
5. External integration infrastructure belongs to TEAI.
6. Runtime execution semantics belong to CNCF.
7. AI work should be expressed as goals/capabilities and delegated where useful.
8. Stable AI-assisted procedures should be formalized progressively.
