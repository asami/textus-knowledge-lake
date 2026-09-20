# Textus Knowledge Lake (TKL)

Textus Knowledge Lake is the **Knowledge Ingestion Application** that brings knowledge candidates, primarily prepared in an external Knowledge Lake, into the Textus World.

The initial Knowledge Lake is Google Workspace. Raw and working materials remain there, and NotebookLM, Workspace AI, and humans perform primary knowledge processing. TKL begins at the boundary where those results become **Knowledge Candidates** for Textus.

## Core definition

> TKL imports knowledge candidates that have received primary processing in a Knowledge Lake into the Textus World.

TKL is therefore not intended to reproduce NotebookLM or to ingest every raw file merely because it exists in Google Workspace.

```text
              Google Workspace
               Knowledge Lake
                     |
        +------------+-------------+
        |                          |
   Raw Materials              NotebookLM /
 PDF / Sheets / Docs          Workspace AI /
 Mail / Images / ...          Human analysis
        |                          |
        +---- source/provenance ---+
                                   |
                           Knowledge Candidate
                                   |
                                  TEAI
                                   |
                                   v
                     textus-knowledge-lake
                       Knowledge Ingestion
                                   |
                            Textus World
                           /            \
                    Information       Knowledge
                           \            /
                            KnowledgeHub
                                 |
                                BoK
```

## Responsibility boundaries

- **Google Workspace / Knowledge Lake** — stores source and working materials and supports primary knowledge processing.
- **NotebookLM / Workspace AI / humans** — read, compare, summarize, investigate, discuss, and prepare Knowledge Candidates.
- **TEAI** — transports events/data, integration bindings, authentication, delivery/retry, Job/Workflow initiation and continuation.
- **TKL** — validates and interprets Knowledge Candidates, normalizes provenance, resolves identity, maps them to Textus concepts, and ingests them into the Textus World.
- **KnowledgeHub** — represents, links, processes and retrieves Textus knowledge.
- **BoK** — publishes curated knowledge products.

## Knowledge Candidate

The central input concept of TKL is `KnowledgeCandidate`, not raw `Material`.

A candidate may contain or reference:

- content/summary;
- claims;
- concepts/entities;
- source references and citations;
- context;
- provenance;
- processing history;
- human notes/review;
- generation/processing agent information.

The exact DTO/model will be refined with TEAI and KnowledgeHub.

## Source material

Original materials do not necessarily need to be copied into Textus.

TKL can retain provider-neutral external references such as:

```text
ExternalMaterialReference
  provider
  resourceId
  version
  checksum
  capturedAt
```

Knowledge imported into Textus must remain traceable to its source material and processing provenance.

## Ingestion flow

```text
KnowledgeCandidate
       |
       v
      TEAI
       |
       v
      TKL
       |
       +-- validate
       +-- normalize provenance/context
       +-- resolve identity
       +-- map ontology/facets
       +-- map Information / Knowledge
       +-- link existing knowledge
       +-- ingest
       |
       v
   Textus World
```

Raw acquisition, broad summarization, exploratory comparison and source-set Q&A should normally happen on the Knowledge Lake side before this boundary.

## Provider independence

Google Workspace + NotebookLM is the first reference environment, not a hard dependency. Future enterprise lakes and knowledge-processing environments can produce the same provider-neutral Knowledge Candidate contract.

## Initial reference scenario

Phase 1 validates:

```text
Google Workspace
   -> NotebookLM / human primary processing
   -> Knowledge Candidate
   -> TEAI
   -> TKL
   -> KnowledgeHub / Textus World
   -> BoK
```

The primary architectural question is no longer how TKL processes raw Drive files. It is how a traceable, context-rich Knowledge Candidate crosses the Knowledge Lake/Textus boundary correctly.
