# Phase 1 — Knowledge Candidate Ingestion Reference Flow

## Goal

Establish the minimum end-to-end path for importing a knowledge candidate produced by primary processing in a Google Workspace Knowledge Lake into the Textus World.

## Reference flow

```text
Google Workspace sources
  -> NotebookLM / human primary processing
  -> KnowledgeCandidate
  -> TEAI
  -> CNCF Job / TKL Workflow
  -> validate / normalize / map / ingest
  -> KnowledgeHub / Textus World
  -> BoK publication decision
```

## Scope

### Architecture

- define the Knowledge Lake / TEAI / TKL / KnowledgeHub / BoK boundary;
- define an initial provider-neutral KnowledgeCandidate model;
- define ExternalMaterialReference and provenance requirements;
- define mapping from candidate concepts into Textus Information/Knowledge;
- supersede the previous assumption that TKL owns raw-material acquisition/classification.

### Google Workspace reference

- use Google Workspace as the source/working Knowledge Lake;
- use NotebookLM and/or human processing to create one representative knowledge candidate;
- retain links/citations/provenance to original Workspace materials;
- do not require TKL to copy all original source artifacts.

### TEAI integration

- transport or signal availability of the KnowledgeCandidate;
- start a CNCF Job/Workflow for ingestion;
- preserve correlation and integration provenance;
- provide source retrieval through an external reference where needed.

### TKL ingestion workflow

- validate the candidate contract;
- normalize provenance and context;
- resolve candidate/source identity;
- map concepts/facets and Information/Knowledge;
- relate to existing knowledge where applicable;
- make an explicit ingestion/curation decision;
- ingest accepted content into KnowledgeHub/Textus;
- retain traceability to source and primary-processing history.

## Non-goals

Phase 1 does not attempt to:

- reproduce NotebookLM in TKL;
- ingest every raw Google Drive file;
- implement broad source summarization/Q&A in TKL;
- support every Google Workspace API;
- duplicate TEAI integration infrastructure;
- duplicate CNCF runtime facilities;
- fully automate all semantic/curation decisions.

## Completion criteria

Phase 1 is complete when a KnowledgeCandidate derived from a small Google Workspace source set can cross the TEAI/TKL boundary and become a traceable Textus Information/Knowledge representation, with provenance back to its original sources and primary processing.

The resulting knowledge must also reach an explicit BoK publication decision, whether published, deferred, or rejected.


## Google reference implementation priority

Phase 1 should prioritize the Google preparation route based on **Drive + Drive Project + Gemini in Drive** where live Google integration is introduced.

Gemini Notebook integration is not required for Phase 1 closure. It is an optional later presentation/production integration and must not expand the Phase 1 scope.


## Editing Studio Managed Candidate Material Set (2026-10-06)

Phase 1 must define a minimum TKL-compatible Material Set representation for managed candidate sources produced by applications such as NICT Editing Studio. The first producer is allowed to materialize the set directly into Google Drive before a TKL API exists, but the stored structure must be ingestible and operable by TKL without bulk migration.

Minimum logical content:

- stable Material Set identity and manifest/metadata;
- original source files such as book/page photographs and audio;
- derived artifacts such as transcript and capture-derived ISBN/title/annotation metadata;
- provenance connecting derived artifacts to original sources;
- producer/application and candidate identity references;
- media/type metadata sufficient for TKL Resource/Evidence mapping.

The physical Google Drive folder/file layout is an implementation mapping, not canonical TKL identity. Avoid mandatory duplication of large originals. Existing Drive objects may become TKL-managed resources through stable references.

Drive + Drive Project + Gemini in Drive remains the standard initial Google preparation route. Gemini Notebook remains optional for curated investigation, synthesis or presentation where useful; the Material Set must not require Notebook to be valid. Gemini-facing documents/views may be projections while canonical management metadata remains in the TKL-defined representation.

Acceptance/planning must include a concrete Editing Studio-style fixture containing photographs, audio, transcript and capture-derived metadata and demonstrate that TKL can identify the set, its evidence/provenance and its preparation inputs.
