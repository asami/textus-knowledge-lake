# Textus Knowledge Lake (TKL)

Textus Knowledge Lake manages provider-neutral resources, evidence, knowledge-processing contexts, preparation and traceable candidate proposals. **TKL itself is the Knowledge Lake.** Google Workspace is its first production backend target.

Drive folders hold source/working artifacts and canonical TKL metadata JSON / PreparedMaterial. KnowledgeProcessingContext describes a persistent purpose and selected evidence; Drive Project and Gemini Notebook are possible external realizations. ProcessingRun records one execution and its fixed inputs, results and lineage. The exact Notebook product/API is an adapter-design question.

## Responsibility boundaries

| Owner | Responsibility |
| --- | --- |
| TKL | Resource / Evidence identity, Context / Run, preparation, PreparedMaterial, provenance, RawCandidateProposal and existing-information projections |
| Google Workspace / other providers | Physical resource representations and external processing facilities |
| TKW | Candidate identity, formation, editing, review, human approval and Admission requests |
| KnowledgeHub | Canonical Information and its versions |
| TEAI / OpenClaw | Enterprise integration and physical/tool-mediated access where used |
| BoK | Curated knowledge publication |

RawCandidateProposal is a TKL proposal, separate from a TKW Candidate. TKL does not directly review/approve or admit a Candidate into KnowledgeHub.

```text
Provider resources
  -> TKL Resource / Evidence
  -> KnowledgeProcessingContext / ProcessingRun
  -> PreparedMaterial(version)
  -> RawCandidateProposal
  -> TKW formation / edit / review / approval
  -> KnowledgeHub Information
  -> TKL existing-information projection for later preparation
```

This diagram describes the eventual application relationship. Actual external integration is distinct from the project-local Phase 1 acceptance described below.

## First backend and processing contexts

The first production target uses a Drive folder or folder set. Small independent JSON records represent TKL entities; a separate TKL database is not mandatory initially. Stable TKL IDs, entity revisions, provider revisions and retained input snapshot identities have different meanings.

Drive Project is the first external processing-context target. Manual processing and import of preparation results are supported design paths. Gemini Notebook is an optional context for preparation, analysis, synthesis and production, with concrete capabilities to be verified during integration.

Source resources are referenced by default. Retain/materialize required input content according to policy; a provider revision reference alone does not prove permanent content availability. Record unretained or unconfirmed inputs explicitly.

JSON is the lossless representation of PreparedMaterial. Markdown is a derived projection for people and LLMs. Historical Run inputs and material versions remain stable when the current Context changes.

## Phase 1: project-local development

[Phase 1](docs/phase/phase-1.md) implements and validates the TKL-owned preparation/proposal path using repository-owned fixtures and executable specifications:

```text
Small Drive-shaped source fixtures
  -> Resource / Evidence -> Context / fixed-input Run
  -> preparation-result import -> persisted PreparedMaterial
  -> JSON / Markdown -> RawCandidateProposal export
  -> local boundary fixture for TKL result/correlation handling
```

Closure requires TKL-local behavior, reproducible local validation, accurate documentation, independent review and concrete external integration proposals. It does not require another project's build, service, release or contract acceptance, live Google credentials, TKW Candidate creation, KnowledgeHub Admission or a BoK publication decision.

A simulated receipt / Candidate reference proves TKL-side handling only. Durable downstream receipt, concurrent duplicate prevention and production Admission remain requirements for their owners. TKL does not implement a substitute downstream service.

The [acceptance checklist](docs/phase/phase-1-checklist.md) records pending delivery. Current CML is a scaffold; documentation and proposed contracts do not imply implemented behavior.

## Integration management

The [integration register](docs/notes/phase-1-integration-register.md) tracks owner, contract/version, TKL evidence, external unverified guarantees, open questions and follow-up work for each boundary. Phase closure produces a proposal package in this repository with sample payloads/fixtures, acceptance examples and recommended receiving-project work.

Provider adapters map the semantic model onto external capabilities. TEAI/OpenClaw may perform transport, invocation/delegation and continuation where appropriate; these mechanisms do not redefine the TKL domain model. No provider-specific physical integration or shared infrastructure is invented to bypass a missing upstream capability.

## Design records

- [Project-local Phase 1 decision](docs/journal/2026-10-02-phase-1-project-local-boundary.md)
- [Google Workspace backend and contexts](docs/notes/google-workspace-backed-tkl.md)
- [Architecture](docs/notes/architecture.md)
- [Application model](docs/notes/application-model.md)
- [Review follow-up](docs/journal/2026-10-02-google-workspace-backed-tkl-review-follow-up.md)
