# Phase 2 Definition — Google Drive Backend

- Date: 2026-10-03
- Status: Planned; definition recorded, execution not started
- Owner: Textus Knowledge Lake
- Phase: [Requirements and completion criteria](../phase/phase-2.md)
- Checklist: [Phase 2 acceptance](../phase/phase-2-checklist.md)

## Basis

The user requested a Phase 2 definition after the completed project-local Phase 1. The first backend remains Google Workspace: Drive folders contain canonical TKL metadata and preparation artifacts, while Drive Project supplies an external processing context. Phase 1 closed at `f131d16a6dc98ff3ea754db6378aea64f090ff56`, with local/native behavior accepted and provider guarantees still unverified.

The [Phase 1 TKL-INT-02 proposal](2026/10/2026-10-02-phase-1-integration-proposals-v1.md#tkl-int-02) supplies the starting boundary. This definition turns its Drive acquisition, storage, retention, concurrency and manual-import requirements into a bounded next Phase.

## Defined scope

Phase 2 delivers one small real Drive-backed preparation flow: explicitly selected source folders, stable Resource/Evidence mapping, independently manageable canonical JSON records, a frozen manual input handoff, result import, versioned PreparedMaterial and RawCandidateProposal export. The minimum acquisition profile includes a Google Docs document and a UTF-8 text file. One designated TKL storage root is sufficient for initial acceptance.

Actual Drive read/write and fresh-process recovery are required acceptance evidence. The operation model must preserve visible version conflicts and incomplete/unknown outcomes; local atomic filesystem behavior cannot establish remote guarantees. The writer/publication protocol is selected only after provider capability verification.

Drive Project operation is manual in this Phase. Its concrete product/reference and operator procedure must be identified before claiming that integration. TKL records supplied inputs and observed provenance without inferring hidden processor inputs or model identity. Notebook automation, live downstream Candidate delivery and feedback remain later scope.

## Execution boundaries

Implementation changes belong to TKL. Existing released dependencies and the selected live Google environment are permitted; new development or a release in another source repository is not assumed. Necessary external-owner work remains managed through a Phase 2 register and versioned closing proposals using the existing boundary IDs.

The current v1 canonical exports and accepted local behavior remain regression contracts. Provider-specific metadata and pending-operation tracking require explicit adapter contracts rather than silent changes to finalized Run semantics or the closed v1 schemas.

The planned sequence is capability/contract definition, Drive acquisition and persistence, manual preparation/export, then acceptance and closure. The Phase document defines ten requirements, six completion criteria and sixteen initially unchecked acceptance items. No execution Goal, implementation, live operation, commit or push is initiated by this definition.

## Next execution inputs

Resolve the live account and folder selection, credential mechanism, supported export representation, verified writer/publication protocol and concrete manual processing context during execution planning. Record unavailable capabilities explicitly and revise scope through an explicit decision if necessary; fixture success cannot substitute for the defined live acceptance.


## Applied split — 2026-10-03

The original definition above is historical pre-split scope. The same-task explicit `$cncf-split-phase 2` invocation applies the settled partition without a second approval. Source Goal/status: `none / no-goal-pre-entry`. The source entry returned `SPLIT_REQUIRED` for `split_reasons=["time-bound"]`: 12h expected/18h upper versus the 8h expected ceiling. This result is dated pre-split evidence; both resulting documents now record a structural `PROCEED` gate with runtime suitability deferred to execution.

The numbering contract retains [Phase 2](../phase/phase-2.md) for Drive storage and creates [Phase 2.1](../phase/phase-2.1.md) for manual processing/export. Earlier 2.1/2.2 proposal labels were not adopted identities. README is the authoritative sequence index; no other strategy/index or former successor exists. Phase 1 closed history, dates, accepted records and integration package remain unchanged. No Phase 2 completed Step, Slice, validation or commit existed to move.

| Original unfinished authority | Owning Phase after split |
| --- | --- |
| P2-R01..05; P2-C01..09; original completion criteria 1 and 2 | Phase 2 |
| P2-R06/R07; P2-C10..16; original completion criteria 3..6 | Phase 2.1 |
| P2-R08 storage configuration/native recovery and settled manual interface | Phase 2, explicitly scoped P2-R08-A |
| P2-R08 manual native commands and operator workflow | Phase 2.1, explicitly scoped P2-R08-B |
| P2-R09 focused/local/live storage evidence | Phase 2, explicitly scoped P2-R09-A |
| P2-R09 manual evidence, combined end-to-end acceptance and aggregate suite | Phase 2.1, explicitly scoped P2-R09-B |
| P2-R10 storage register/proposals and versioned handoff | Phase 2, explicitly scoped P2-R10-A |
| P2-R10 reconciled full-flow closing owner proposals | Phase 2.1, explicitly scoped P2-R10-B |
| TKL-P2-01, TKL-P2-02, storage closure fragment TKL-P2-04A | Phase 2 |
| TKL-P2-03, complete-flow closure fragment TKL-P2-04B | Phase 2.1 |

Original unchecked IDs occur exactly once across the two checklists. New first-unit P2-C17..19 provide focused validation, handoff and intermediate closure obligations without duplicating original full-flow items. Their own completion mappings reference predecessor evidence where appropriate.

Validation timing is `final-only`, method `sbt-full-suite`, bootstrap `none`, serial sequence `["PHASE-2","PHASE-2.1"]`; final owner is PHASE-2.1. The helper-generated declarations are reciprocal. Phase 2 closes with focused assurance and a pending aggregate complete-validation gate. Phase 2.1 verifies the predecessor handoff and runs the complete suite once. Both still require focused validation, independent reviews, Step acceptance, closure ledger, scoped live acceptance and local release; split itself runs no product tests.

The parent planning capability is `PROCEED_UNVERIFIED`: partitioning is bounded/settled, current runtime is not directly observable and the required current-task profile resolver returned `NOT_FOUND`. This does not attest future execution capability. Provider capabilities, credential selection and manual product operation remain execution inputs, not solved facts.

Phase 2 owns the expensive discovery/kernel: capability, publication, writer, recovery and manual-interface decisions, together with coherent storage implementation and acceptance. It is expected to take 7h (6-10h range), 1h above the 6h target and 1h below the 8h ceiling, at gpt-6.1-sol/high. Phase 2.1 expects 6h (5-9h range), at target and 2h below ceiling, at gpt-6.1-sol/medium only when the handoff is settled; otherwise recheck to high. Agent reasoning-mode policy is default standard, with pro considered only at an eligible supported launch when frozen quality-first evidence justifies it.

Calibration: Phase 1 planned 420m and near-release observation 48,244s (~804m), ratio ~1.91; same-repository native/assurance work is partially comparable, but this is not a global multiplier. Bottom-up partition estimates incorporate provider uncertainty and closure overhead. Confidence is low to medium, assuming timely account/folder/operator access. Upper bounds above eight hours disclose risk and do not trigger additional splitting.

The exact high-to-medium handoff is the accepted Phase 2 local release, versioned record/publication/writer/recovery and manual package/import contracts, credential/configuration interface without secrets, scoped behavioral/live storage receipts, v1 compatibility constraints and the identified external manual context procedure. Phase 2.1 consumes it without redesigning storage. There is no independent cost-isolation split claim: about 1h extra closure/handoff yields 13h split versus 12h unsplit, with no claimed numeric saving; final-only avoids duplicate complete validation. Keeping coherent storage acceptance with discovery avoids a decision-only micro-Phase.

Incoming semantic handoffs are `PHASE-2: []`, `PHASE-2.1: []`. The ordinary release/contract dependency is not a genuine semantic boundary. Both children exceed five hours, so no short-child rebalance/merge exception is used. Their adjacent merge recreates the approximately twelve-hour scope above the eight-hour ceiling; `profile_cost_only_rejection_forbidden=true`.

The split is planning-only and uncommitted. It creates no Goal, starts no child execution and performs no live operation. The first Phase may start immediately from the validated uncommitted planning set; its normal acceptance/management commit saves that set. Entry points: `$cncf-goal-phase Phase 2` or `$cncf-manage-phase Phase 2`; Phase 2.1 requires predecessor closure and its own exact invocation.
