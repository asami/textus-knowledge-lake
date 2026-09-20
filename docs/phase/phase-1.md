# Phase 1 — Google Workspace Knowledge Lake Reference Flow

## Goal

Establish the minimum end-to-end architecture for using Google Workspace as a Knowledge Lake and curating selected material through TKL into KnowledgeHub/BoK, using TEAI as the integration foundation.

## Reference flow

```text
Google Workspace
  -> TEAI source event / Integration Binding
  -> CNCF Job / Workflow
  -> TKL acquisition
  -> classification/enrichment
  -> curation decision
  -> KnowledgeHub
  -> BoK publication/reference
```

## Scope

### Architecture

- define the TKL/TEAI/KnowledgeHub/BoK responsibility boundary;
- define initial Knowledge Source, Material, Curation and Publication concepts;
- define source/provenance identity;
- define the initial material lifecycle.

### Google Workspace reference source

- select a narrow Google Drive-based acquisition scenario;
- represent source file identity and metadata without leaking Google-specific concepts into the core model;
- receive source changes through TEAI rather than implementing a parallel integration subsystem.

### Workflow

- start a CNCF Job/Workflow from the TEAI integration event;
- acquire/reference the source material;
- identify and record provenance;
- classify/enrich;
- provide a curation decision point;
- publish/reference selected knowledge in KnowledgeHub/BoK;
- make execution observable through CNCF Job/Workflow facilities.

### AI/agent reference path

- identify at least one non-deterministic curation step;
- express it as a knowledge-domain goal;
- allow TEAI to delegate it through Continuation Protocol to OpenClaw;
- return a normalized result to the same workflow;
- retain provenance of the AI-assisted result.

## Non-goals

Phase 1 does not attempt to:

- support every Google Workspace service;
- build a generic connector catalog in TKL;
- duplicate TEAI integration infrastructure;
- duplicate CNCF Job/Workflow/StateMachine;
- automatically publish all lake material;
- fully automate curation without an explicit policy/review boundary.

## Completion criteria

Phase 1 is complete when one representative Google Workspace material item can travel through the reference flow with traceable source/provenance, observable CNCF execution, a curation decision, and a resulting KnowledgeHub/BoK representation or publication decision.

At least one workflow step should also demonstrate the provider-neutral AI/agent continuation path through TEAI/OpenClaw.
