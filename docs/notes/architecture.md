# Knowledge Lake Architecture

## Purpose

TKL is the gateway between an external **Knowledge Lake** and the **Textus World**.

The Knowledge Lake performs source management and primary knowledge processing. TKL receives the resulting Knowledge Candidates and performs semantic ingestion into Textus.

## Reference environment

The initial reference environment is:

```text
Google Workspace
  +-- source/working materials
  +-- NotebookLM
  +-- Workspace AI
  +-- human knowledge work
          |
          v
   KnowledgeCandidate
          |
         TEAI
          |
         TKL
          |
     Textus World
```

Google Workspace is not merely raw storage. It is an operational Knowledge Lake with its own interactive knowledge-processing capabilities.

## Boundary

### Knowledge Lake side

Responsible for:

- storing original and working material;
- organizing source sets for human work;
- reading and comparing sources;
- exploratory Q&A;
- summarization and analysis;
- extracting initial issues/concepts/claims;
- human notes and interpretation;
- producing a candidate suitable for Textus ingestion.

NotebookLM is a reference **interactive primary knowledge processor** in this layer.

### TEAI

Responsible for the integration boundary:

- detecting/transporting candidate publication events;
- provider authentication and external access;
- mapping transport DTOs;
- delivery/retry/idempotency;
- starting CNCF Job/Workflow;
- external AI/agent continuation when needed;
- integration audit/provenance.

### TKL

Responsible for semantic ingestion:

- validate KnowledgeCandidate;
- normalize source/provenance/context;
- resolve identity;
- map concepts/facets/ontology;
- map candidate content to Textus Information/Knowledge;
- link to existing Textus knowledge;
- apply ingestion/curation policy;
- ingest accepted results;
- retain traceability to external source and derived artifacts.

TKL should not duplicate NotebookLM-style primary processing.

### KnowledgeHub

Responsible for knowledge representation, linking, processing, retrieval and services after ingestion.

### BoK

Responsible for curated knowledge publication. A Knowledge Candidate does not automatically become a BoK item.

## Core model direction

### KnowledgeCandidate

```text
KnowledgeCandidate
  +-- Content
  +-- Claim*
  +-- Concept*
  +-- SourceReference*
  +-- Citation*
  +-- Context
  +-- Provenance
  +-- ProcessingHistory
  +-- HumanNote*
```

This is conceptual and not yet a fixed implementation schema.

### ExternalMaterialReference

Original source artifacts can remain in the external lake.

```text
ExternalMaterialReference
  +-- provider
  +-- resourceId
  +-- version
  +-- checksum
  +-- capturedAt
```

The reference must be sufficient to preserve identity and provenance and, where policy permits, retrieve the material through TEAI.

### Derived material provenance

A NotebookLM or human-produced candidate is derived material. Its provenance should preserve:

- source materials;
- processor/agent;
- processing time;
- relevant instruction/context where available;
- human review/annotation;
- citations/source grounding.

This supports the wider Textus principle that meaning is contextual: who attached meaning, when, and for what purpose matters.

## Lifecycle boundary

The previous TKL lifecycle beginning with `Raw -> Captured` is superseded.

Raw/captured/working-material lifecycle belongs to the external Knowledge Lake.

TKL begins approximately here:

```text
Knowledge Lake
  Raw -> Working -> Primary Processing
                         |
                         v
                 KnowledgeCandidate
                         |
                    TKL boundary
                         |
          Validate -> Interpret -> Map
                         |
                    Ingest/Reject
                         |
                    Textus World
```

## AI processing layers

The architecture can use different AI layers without collapsing them into one agent:

1. **Knowledge Lake AI** — NotebookLM/Workspace AI: understand source sets and produce candidate knowledge.
2. **Integration AI** — OpenClaw through TEAI: achieve external goals and interact with tools/services/humans.
3. **Knowledge AI** — KnowledgeHub: operate on formal Textus knowledge and relationships.

TKL coordinates the transition into layer 3; it does not replace layers 1 or 2.

## Design principles

1. TKL starts from Knowledge Candidates, not arbitrary raw lake content.
2. Primary exploratory knowledge processing belongs in the Knowledge Lake.
3. Original materials may remain external and be referenced rather than copied.
4. Provenance must connect Textus knowledge through the candidate to original sources.
5. Provider-specific integration belongs to TEAI.
6. NotebookLM is a reference processor, not a TKL dependency.
7. The KnowledgeCandidate contract should be provider-neutral.
8. Ingestion into Textus is an explicit semantic boundary.
