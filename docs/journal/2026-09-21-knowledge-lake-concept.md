# Knowledge Lake Concept and Project Creation

Date: 2026-09-21

## Context

The project needs a practical place for project materials that should be available to knowledge work without requiring every item to be immediately modeled as KnowledgeHub knowledge or published into a BoK.

Google Workspace is a useful operational store for this purpose. Documents, spreadsheets, mail-related material, images, PDFs and other project artifacts can remain in familiar collaborative storage while selected content is progressively incorporated into the Textus knowledge architecture.

This led to the **Knowledge Lake** concept and the creation of `textus-knowledge-lake` (TKL).

## Decision

Create TKL as a dedicated application rather than embedding Knowledge Lake behavior directly in KnowledgeHub or TEAI.

The initial arrangement is:

```text
Google Workspace -> TEAI -> TKL -> KnowledgeHub -> BoK
```

Google Workspace is the first Knowledge Lake provider. It must not become part of the core TKL model so that additional sources can be incorporated later.

## Why a separate application

The responsibilities are different:

- TEAI answers how enterprise systems and events are connected and executed.
- TKL answers how source material is acquired, classified, enriched, curated and selected for publication.
- KnowledgeHub answers how knowledge is represented, linked, processed and retrieved.
- BoK is the curated knowledge product.

Keeping these boundaries separate prevents KnowledgeHub from becoming a raw-document ingestion system and prevents TEAI from acquiring knowledge-domain semantics.

## Curation boundary

A central principle is that **material is not knowledge merely because it exists in the lake**.

Material may move through:

```text
Raw -> Captured -> Classified -> Enriched -> Curated -> Published
```

Some material may never be published. TKL must preserve this distinction and the provenance of published knowledge.

## TEAI as infrastructure

The integration foundation should be TEAI.

For example, when a file is created or changed in Google Workspace, TEAI detects/receives the external event, applies the integration binding and starts a CNCF Job/Workflow associated with TKL processing.

TKL then performs the knowledge-specific workflow.

This makes TKL an early real application of TEAI rather than introducing another integration mechanism.

## AI-assisted knowledge work

Classification, summarization, concept extraction, facet suggestion and relationship discovery are natural AI-assisted tasks.

TKL can express these as goals. TEAI can delegate them through the CNCF Continuation Protocol to OpenClaw or another agent environment. The result returns to the same workflow for review and subsequent processing.

This preserves the ability to start with exploratory AI processing and progressively formalize stable portions into deterministic Workflow/Operation behavior.

## Direction

TKL should initially prove one narrow end-to-end path from Google Workspace through TEAI and TKL into KnowledgeHub/BoK.

The project should then expand by knowledge use case rather than by trying to support every Google Workspace API at once.
