# Phase 1 — Project-local Knowledge Preparation and Proposal

- Current status: Closed on 2026-10-03
- Owner: Textus Knowledge Lake
- Mutation scope: `textus-knowledge-lake` only
- Update rule: Preserve Phase goals, scope and completion criteria; update status only from actual source, validation, independent review and accepted commit/tree evidence.
- Scope authority: [Project-local Phase decision](../journal/2026-10-02-phase-1-project-local-boundary.md)
- Checklist: [Phase 1 acceptance](phase-1-checklist.md)

## Goal

Implement and validate the first TKL preparation/proposal path entirely inside this project. Google Workspace remains the first production backend target; Phase 1 establishes the provider-neutral model, JSON representation, local execution and adapter contracts needed for that target.

Phase completion does not depend on another project's development, release, running service or approval of a proposed integration contract. TKL manages those integration requirements and supplies concrete proposals when closing this Phase.

## Local reference flow

```text
Local fixtures representing a small Drive source set
  -> Resource / selected Evidence
  -> KnowledgeProcessingContext / external context binding
  -> ProcessingRun / fixed input and existing-information snapshot
  -> deterministic or manually supplied preparation result
  -> persisted PreparedMaterial version / Markdown projection
  -> RawCandidateProposal export
  -> local boundary fixture: result handling and proposal correspondence
```

All commands, fixtures and executable specifications required for closure must run from this repository without a running TKW, TEAI, OpenClaw, KnowledgeHub or BoK, live Google credentials, or building/publishing another source checkout. Ordinary installed development tools and released dependencies are permitted.

## In scope

### Domain and persistence

- Define provider-neutral Resource, Evidence, KnowledgeProcessingContext, ProcessingRun, PreparedMaterial and RawCandidateProposal models, versioned JSON and local validation.
- Keep stable TKL IDs separate from provider IDs, entity revisions, provider revisions and input snapshot IDs.
- Capture each Run's fixed input manifest, retained/unretained content status, existing-information baseline, processor/actor and known/unknown provenance.
- Preserve historical Run inputs and PreparedMaterial versions when a Context changes.
- Implement lossless PreparedMaterial JSON and a Markdown projection that does not require re-reading external sources.
- Exercise persistence and update-conflict/partial-failure behavior through a project-local adapter. State precisely what that adapter guarantees; this evidence does not prove equivalent Drive write concurrency or retention.

### Preparation and proposal

- Execute one representative preparation/proposal scenario using project-local source and processing-result fixtures; manually supplied results are permitted.
- Represent a Drive Project binding as external context metadata without requiring automated external operations.
- Export an evidence-grounded RawCandidateProposal with producer-scoped proposal identity, producing Run and versioned material/evidence references.
- Implement the TKL side of boundary result handling: record confirmed fixture results, rejection/conflict, timeout/unknown outcome, and preserve the same proposal identity across query/retry.
- Treat any simulated receipt / TKW Candidate reference as fixture data. TKL must not create or operate a substitute TKW candidate store, review/approval lifecycle or Admission service.
- Express the application Use Case / Workflow using supported native CML constructs. Check current syntax rather than inventing extensions; record unsupported tooling capabilities as integration proposals. Local executable acceptance must still exercise the implemented TKL behavior.

### Integration management and closing proposals

- Maintain the [Phase 1 integration register](../notes/phase-1-integration-register.md) in this repository.
- For each boundary, record owner, purpose, inputs/outputs, proposed schema/reference version, TKL-side evidence, external unverified guarantees, status and next action.
- Manage changes and unresolved compatibility decisions without blocking TKL-local implementation on external agreement.
- At closure, publish a versioned proposal package with sample payloads/fixtures, acceptance examples, proposed contracts, unresolved questions and recommended receiving-project work. Reference the accepted TKL commit/tree and exact exported contract versions.
- Writing proposals in this repository is the closure responsibility. Posting messages, changing other repositories, deploying or pushing is separate work requiring its own authority.

## External boundaries

| Boundary | TKL Phase 1 responsibility | External responsibility / proposal |
| --- | --- | --- |
| Google Workspace / Drive Project | JSON storage/adapter requirements, resource mapping, context bindings, manual-result import and local persistence evidence | Live access, atomic update/retention capability verification and processing automation |
| TKW | RawCandidateProposal export, TKL result handling and receipt correspondence against local fixtures | Durable receipt, concurrent duplicate prevention, Candidate formation/edit/review/approval and Admission |
| TEAI / OpenClaw | Provider-neutral request/result, correlation and failure requirements | Transport, physical access, authentication/policy and invocation/continuation facilities |
| KnowledgeHub | Versioned existing-information fixture and projection references | Canonical Information, query/change contracts and Admission through TKW |
| BoK | Record publication as downstream scope in closing proposals where relevant | Publication workflow and publication decisions |
| CML / Cozy / CNCF | Use existing supported constructs and document missing capability precisely | Any new shared syntax, runtime facility or framework implementation |

External guarantees remain proposed/unverified until their owner validates them. Local fixture success must not be reported as external acceptance or production integration.

## Non-goals

- Changes, builds, `publishLocal`, releases or Phase completion in another project.
- Live Google Workspace operations as a closure prerequisite, or claiming Drive concurrency/retention from local-adapter results.
- TKW candidate lifecycle, human review/approval, KnowledgeHub Admission or BoK publication decisions.
- Production receipt idempotency guarantees on behalf of TKW.
- Full knowledge feedback synchronization, existing-candidate preparation, Gmail/Slack, KAR packaging, Notebook automation or a complete UI.
- Local replacements for absent shared infrastructure or downstream services.

## Completion criteria

1. The representative local flow produces persisted, versioned PreparedMaterial and an exportable RawCandidateProposal with traceable Evidence / Run / input lineage.
2. Executable specifications cover JSON round-trip/projection, stable identities, frozen historical inputs, retention/unknown-provenance semantics, local update conflicts/partial failures and TKL-side retry/result correlation.
3. The source/model, schemas, fixture inputs/results and repository-owned validation command are reproducible without external projects or live services.
4. README, application model, CML and journal accurately describe implemented behavior and its limits; validation and independent review evidence are recorded.
5. The integration register is reconciled, and a closure proposal package identifies all necessary external work with owners, versioned artifacts, acceptance examples and resume conditions. External owners need not have implemented or accepted those proposals for this Phase to close.

The first production path after this local baseline prioritizes Drive + Drive Project, with manually assisted processing permitted. Gemini Notebook remains an optional processing-context realization and is not a Phase 1 dependency.

## Current local evidence and closure binding

Accepted Step 1: `2511186de891e3eb40863bd14d898eca8701ef47`; accepted Step 2: `5778cfcad83f5a8353d91b1a29f1bf4adbb431f4`. Accepted native Step 3: `06c144b91111c408a0d0699178bffad66a7252fd`; source tree: `a3d54dd9cd55b96c640cfc00ab2d64d68c6d6bfc`. Independent full Phase review `TKL-P1-full-review-result/1` passed with zero blockers. Final `sbt --batch test` (`tkl-p1-final-full-suite-1`) passed all 251 tests in six suites, with zero failed/canceled/ignored/pending tests, zero aborted suites, exit 0 and released serial lock. The eight native scenarios and retained CLI revision 7 establish local fixture behavior. Closure reference: `tkl-phase-1-closure@1`; the distinct local release commit records this binding in its Git message. External guarantees remain unverified.

The [public proposal/evidence journal](../journal/2026/10/2026-10-02-phase-1-integration-proposals-v1.md) supplies all seven owner proposals with exact v1 sources, samples, acceptance examples, unresolved guarantees and resume conditions. All five completion criteria and all twelve checklist items are satisfied within TKL. Nonblocking [HYG-TKL-P1-001](../journal/2026/10/2026-10-02-phase-1-hygiene-follow-up.md) is recorded exactly for follow-up. No Development Candidate was added; no empty journal was created.
