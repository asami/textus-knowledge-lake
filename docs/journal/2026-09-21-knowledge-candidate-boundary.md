# TKL Boundary Refined: Knowledge Candidate Ingestion

Date: 2026-09-21

## Supersedes

This decision refines and partially supersedes the initial project concept in `2026-09-21-knowledge-lake-concept.md`, specifically the assumption that TKL begins from raw Google Workspace material and performs the primary classification/enrichment lifecycle itself.

## New definition

**TKL imports knowledge candidates that have received primary processing in a Knowledge Lake into the Textus World.**

Google Workspace is the initial Knowledge Lake. NotebookLM, Workspace AI, and humans can perform primary knowledge work inside that environment before TKL is involved.

## Reasoning

Google Workspace is useful not only as storage but as an operational environment for knowledge work. NotebookLM can work across selected source sets and support reading, comparison, Q&A, summarization and analysis. Reimplementing this capability inside TKL would duplicate a strong existing layer and blur the Textus boundary.

The better separation is:

```text
Google Workspace / NotebookLM
    primary knowledge processing
              |
       KnowledgeCandidate
              |
             TEAI
              |
             TKL
    semantic Textus ingestion
              |
        KnowledgeHub / BoK
```

## Consequences

The central TKL input concept changes from raw `Material` to `KnowledgeCandidate`.

The raw-material lifecycle belongs to the Knowledge Lake. TKL starts when a candidate is intentionally presented for Textus ingestion.

Original source material can remain in Google Workspace. Textus should retain a stable external reference plus provenance rather than automatically copying all source bytes.

TKL is responsible for validating and interpreting the candidate, normalizing provenance/context, resolving identities, mapping to Textus Information/Knowledge/facets, linking existing knowledge, and applying ingestion/curation policy.

## Processing provenance

Knowledge produced by NotebookLM or another processor is derived knowledge material, not an unqualified fact. The ingestion model must preserve source grounding and processing context, including source references/citations and, where available, processor, time, instruction/context, and human review.

This aligns with the broader KnowledgeHub requirement that meaning and annotation are contextual.

## AI layer separation

Three AI roles are intentionally separated:

- NotebookLM/Workspace AI: source-set-oriented primary knowledge processing;
- OpenClaw via TEAI: external goal/tool/human integration;
- KnowledgeHub AI: processing over formal Textus knowledge.

This avoids building one oversized agent layer and lets each environment do the work it is best suited for.

## Phase 1 impact

Phase 1 is changed from raw Google Drive ingestion to a Knowledge Candidate ingestion scenario. A small source set is processed in Google Workspace/NotebookLM, represented as a candidate, transported through TEAI, semantically ingested by TKL, and traced into KnowledgeHub/BoK.
