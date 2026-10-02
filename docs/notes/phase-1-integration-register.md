# Phase 1 Integration Register

- Date: 2026-10-02
- Status: Initial register; proposals and local evidence pending
- Owner: Textus Knowledge Lake
- Authority: [Project-local Phase 1](../phase/phase-1.md)

Phase 1 develops and validates TKL locally. This register manages proposed external contracts without turning their implementation or acceptance into a Phase closure dependency. Contract names below are conceptual until a schema/version is frozen.

| ID | Owner / boundary | Purpose and proposed input → output | Contract / source | TKL evidence and external limits | Status / next action |
| --- | --- | --- | --- | --- | --- |
| TKL-INT-01 | TKW proposal receipt | RawCandidateProposal + versioned lineage → receipt / Candidate reference, rejection/conflict or unknown result | [TKL handoff design](google-workspace-backed-tkl.md); TKW requirements TKL-TKW-01–07 | Local export/result fixtures pending; durable receipt, atomic deduplication and Candidate persistence are TKW guarantees, unverified here | Proposal pending; freeze TKL envelope/results and recommend TKW acceptance examples at closure |
| TKL-INT-02 | Google Workspace storage / processing adapter | Resource / record / selected snapshot / context binding → stored representation / observed preparation result | [Backend/context design](google-workspace-backed-tkl.md) | Local adapter/manual-result fixtures pending; Drive atomic writes, retention, permissions and automation unverified | Proposal pending; specify adapter obligations and live verification scenarios |
| TKL-INT-03 | TEAI / OpenClaw | Provider-neutral invocation/delegation + correlation → result/continuation or explicit failure/unknown outcome | [Architecture integration boundary](architecture.md) | Local boundary examples pending; no proof of actual transport, authenticated producer identity, OAuth or external execution | Proposal pending; identify required existing operations and any missing shared capability |
| TKL-INT-04 | KnowledgeHub via TKW | Versioned canonical Information / Admission references → TKL existing-information snapshot/projection input | [Feedback model](application-model.md); TKW requirements TKL-TKW-08–10 | Versioned input fixture pending; live Admission, current-state queries and change-event guarantees unverified | Later integration proposal; freeze minimal reference requirements, document synchronization follow-up |
| TKL-INT-05 | TKW existing-candidate preparation | CandidatePreparationRequest with request/candidate revision → Run / PreparedMaterial result references | [Correlation design](google-workspace-backed-tkl.md); TKW requirements TKL-TKW-11–12 | Conceptual request/result only; no Phase 1 requirement to implement the reverse path | Later integration proposal; record stale-candidate and duplicate-result acceptance cases |
| TKL-INT-06 | CML / Cozy / CNCF tooling | Native application model / workflow / specs → validated model or executable support | [Application model](application-model.md) | Syntax/capability inspection pending; no invented CML or component-local infrastructure workaround | Capability assessment pending; use supported constructs and propose gaps to their actual owner |
| TKL-INT-07 | BoK | Admitted Information and curation decision → publication outcome | Downstream of TKW / KnowledgeHub | No TKL publication operation or publication-decision proof | Later owner proposal if needed; exclude from TKL Phase 1 closure |

## Per-boundary record

As contracts are specified, append each exact schema version and sample path, producing/consuming ownership, compatibility decisions, local validation command/result, external unverified guarantees, open questions, change history and next action. Use stable IDs above. Do not label a proposal agreed, implemented or externally verified without attributable evidence.

The existing [TKW requirements journal](../../../textus-knowledge-workbench/docs/journal/2026/10/2026-10-02-tkl-workbench-requirements.md) is a requesting-side proposal, not an accepted downstream API. Its initial TKW acceptance scope does not become TKL Phase 1 acceptance scope.

## Closure proposal package

At Phase closure, write the proposal package into a TKL journal and link repository-owned exported schemas, sample requests/results and executable acceptance examples. Bind the package to exact TKL source/contract versions and include:

- implemented TKL behavior and local validation evidence;
- receiving owner and required capability for each still-open boundary;
- input/output schema, stable IDs/revisions, retry/correlation and failure semantics;
- external guarantees still requiring verification, compatibility choices and open questions;
- recommended follow-up work and conditions for starting actual integration.

Reconcile every register entry to implemented-local / proposal-ready / later-scope with evidence and an owner. Proposal-ready means ready for owner review; it does not mean the owner has accepted or implemented it. No external posting or repository update is required for local closure.
