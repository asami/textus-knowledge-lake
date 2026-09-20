# Textus Knowledge Lake (TKL)

Textus Knowledge Lake is the acquisition and curation application for the Textus knowledge architecture.

Its initial deployment model uses Google Workspace as a **Knowledge Lake**: project materials such as documents, spreadsheets, mail-derived artifacts, images, PDFs, notes, and intermediate work can remain in the workspace while TKL discovers, organizes, enriches, curates, and publishes selected material into KnowledgeHub and Body of Knowledge (BoK) products.

Google Workspace is the first provider, not the TKL architecture itself.

## Position in the Textus architecture

```text
Google Workspace / Slack / GitHub / Files / ...
                    |
                    v
                   TEAI
      integration / events / continuation
                    |
                    v
          textus-knowledge-lake
        acquisition / curation
                    |
                    v
              KnowledgeHub
     representation / processing / retrieval
                    |
                    v
                   BoK
            curated publication
```

A useful responsibility summary is:

- **Workspace / source systems** — Knowledge Lake storage and working materials.
- **TEAI** — connectivity, enterprise events, integration bindings, Job/Workflow initiation, delivery/retry and AI/agent continuation.
- **TKL** — knowledge acquisition, classification, enrichment, curation and publication decisions.
- **KnowledgeHub** — knowledge representation, processing, linking, retrieval and knowledge services.
- **BoK** — curated knowledge products for human and machine use.

## Knowledge lifecycle

TKL treats lake content as material that may progressively become managed knowledge.

```text
Raw
  -> Captured
  -> Classified
  -> Enriched
  -> Curated
  -> Published
```

Not everything in the lake must become KnowledgeHub knowledge or a BoK publication.

TKL should preserve source identity, provenance, curation state and publication decisions throughout this lifecycle.

## TEAI integration

TKL does not implement a general-purpose external integration layer. It uses **textus-enterprise-application-integration (TEAI)** for integration infrastructure.

Example:

```text
Google Drive
  -> FileCreated
  -> TEAI event/binding
  -> CNCF Job
  -> TKL Workflow
       -> acquire
       -> identify
       -> extract metadata
       -> classify
       -> enrich/relate
       -> curate
       -> publish
  -> KnowledgeHub / BoK
```

TEAI may use direct deterministic endpoints or delegate agent-mediated external interaction through OpenClaw and the CNCF Continuation Protocol.

## AI-assisted curation

AI is useful for non-deterministic knowledge work such as:

- classification;
- summarization;
- concept/entity extraction;
- facet suggestions;
- relationship discovery;
- duplicate/related-material assessment;
- publication suggestions.

A TKL Workflow can delegate such work through TEAI/OpenClaw and receive the result through the Continuation Protocol. Human review can remain an explicit workflow step before publication.

Stable processing discovered through operation should progressively move toward deterministic policies, Workflows and Operations where appropriate.

## Provider independence

The initial Knowledge Lake is Google Workspace, but TKL should be able to acquire material from additional sources through TEAI, for example:

- Slack or other collaboration systems;
- GitHub;
- local/project files;
- Web sources;
- other enterprise repositories;
- OpenClaw-accessible tools and services.

Provider-specific connection details should not leak into the core TKL knowledge model.

## Initial reference scenario

Phase 1 should establish a minimal end-to-end path:

```text
Google Workspace
      |
      v
     TEAI
      |
      v
TKL acquisition/curation workflow
      |
      v
 KnowledgeHub
      |
      v
     BoK
```

This scenario is also a reference application for TEAI: it exercises enterprise events, Job/Workflow execution, external integration and AI/agent continuation in a real knowledge-management use case.
