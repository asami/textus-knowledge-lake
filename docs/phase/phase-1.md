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
