# Phase 2.1 — Manual Preparation and Proposal Export

status=planned
split_full_test_policy=final-only
split_full_validation_method=sbt-full-suite
split_validation_bootstrap=none
validation_ownership=aggregate-final-owner
aggregate_validation_owner=PHASE-2.1
aggregate_validation_sequence=["PHASE-2","PHASE-2.1"]
depends_on=[Phase 2](phase-2.md)

- Current status: Planned; implementation not started
- Defined: 2026-10-03
- Split from: [Phase 2](phase-2.md)
- Owner and mutation scope: Textus Knowledge Lake / `textus-knowledge-lake`
- Predecessor: Phase 2; successor: none currently defined
- Checklist: [Manual flow and aggregate acceptance](phase-2.1-checklist.md)
- Provenance: [Original definition and applied split](../journal/2026-10-03-phase-2-definition.md)
- Update rule: Do not start before predecessor closure and a fresh exact Phase invocation; preserve actual evidence and versioned contracts.

## Goal and consumed handoff

Use the accepted Drive backend for one actual manual processing session, validated result import, persisted PreparedMaterial and canonical v1 proposal export through native TKL. Close the original end-to-end Google Workspace target, including all required live observations and the aggregate complete suite.

The accepted Phase 2 release, versioned Drive record/publication/writer/recovery specification, credential/configuration interface without secrets, storage behavioral and live receipts, closed v1 compatibility constraints, and a concrete manual processing product/reference/procedure with frozen-package and result-import formats. Phase 2.1 consumes these artifacts; it must not rediscover or silently weaken storage guarantees. If the contracts remain unsettled, return to planning and reassess the runtime profile before dependent implementation.

Drive folders are canonical storage; Context identity remains TKL-owned and the external binding is separate. TKW owns Candidate formation/review/approval, Hub owns canonical Information and BoK owns publication. Live downstream delivery/feedback is outside this flow. Keep [canonical v1](../spec/preparation-contract-v1.md) and [local behavior](../spec/local-preparation-flow-v1.md) passing. Only TKL source changes are admitted; existing released dependencies and the selected live Google environment are allowed.

## Requirements

### P2-R06 — Manual Drive Project preparation

Persist the external context binding independently of the TKL Context ID. Provide an input package and operator instructions that identify the exact frozen resources, Evidence, purpose and baseline supplied for processing. Record what TKL supplied separately from what the external processor is known to have used.

Support an actual manual processing session followed by explicit result import. Bind the import to the selected input and Run identity; validate material citations, exact text and known/unknown processor provenance. Reject a mismatched input, conflicting reuse of a completed Run, or incompatible repeated import. Repeating an already accepted identical import must have a documented outcome and must not duplicate accepted material.

The current v1 Run describes a finalized result with `completed`, `partial` or `failed` status. Pending provider work and manual-session tracking must use a separately specified operation record if needed; they must not invent new v1 Run states or alter a previously finalized Run. Unobserved model identity, hidden inputs and reproducibility remain unknown.

### P2-R07 — Proposal export and ownership

Export a canonical preparation bundle, Markdown projection and RawCandidateProposal using the accepted v1 lineage and producer-scoped proposal identity. Export must work from stored inputs and material, without rereading source documents. The initial flow ends at export; simulated downstream observations retain their fixture label.

TKW owns Candidate formation, editing, review, approval and Admission requests. KnowledgeHub owns canonical Information; BoK owns publication. The existing-information baseline may remain an explicitly supplied versioned snapshot. Live downstream delivery and feedback are later integration work.

### P2-R08-B — Native manual flow and operator guide

Implement the predecessor's package/import interface through supported native CML and the existing released runtime. Document exact commands, context binding, frozen handoff, operator session, import/export, credentials/permissions and recovery. Preserve known/unknown provenance; secrets never enter packages or logs. Do not invent shared APIs or automate an unavailable context.

### P2-R09-B — Manual and aggregate acceptance

Credential-free behavioral tests cover exact Run/input/citation/provenance binding, identical and conflicting repeat imports, finalized Run immutability, no duplicate accepted result, unknown/interrupted imports and canonical-success/projection-failure recovery. Run affected native/local regressions with controlled responses; labels remain explicit.

Perform one real identified external-context session using the frozen package, record supplied versus known processor-visible inputs, import the result and export stored bundle JSON/Markdown/proposal without rereading source documents. Combine this with accepted predecessor evidence for real Drive read/write, no-cache restart, source-change history and same-version concurrency. Any missing required observation leaves original end-to-end acceptance pending.

As aggregate final owner, verify Phase 2's accepted ancestry and deferred-validation handoff, then run the repository-complete `sbt --batch test` once for the entire serial sequence at final release. Preserve predecessor focused assurance instead of retrospectively claiming it ran the full suite.

### P2-R10-B — Full-flow proposals and closure

Reconcile the TKL-local integration register and versioned full-flow closing package using TKL-INT-01..07. Bind actual source/adapter/export versions, sanitized examples, local/live evidence, independent review, guarantees/limitations, and concrete external-owner work with resume conditions. Preserve Phase 1 history and Phase 2 storage receipts; no receiving-project acceptance is claimed.

## Steps and completion criteria

| Step | Owned result | Exit condition |
| --- | --- | --- |
| TKL-P2-03 | Frozen manual package, context session, validated import and native exports | Real manual flow plus repeated/conflicting import and recovery evidence pass |
| TKL-P2-04B | Aggregate acceptance, operator guide, integration proposals and release | All original complete-flow criteria and local/live evidence pass; independent review, aggregate suite and local closure commits complete |

1. A real manual session produces validated persisted material and a v1 proposal with exact frozen-input lineage and no repeated-import duplication.
2. Manual/native adapter tests and the aggregate full suite pass; predecessor storage evidence plus this Phase's live cycle cover the entire original required flow.
3. English operator/specification/native documents and sanitized examples agree on supported formats, provenance, behavior, errors and recovery.
4. Independent reviews, local Step/release commits, reconciled integration register and full-flow owner proposals close all original end-to-end obligations with no unpersisted accepted follow-up records.

## Validation ownership and dependencies

Phase 2.1 is the final owner for `["PHASE-2","PHASE-2.1"]`, timing `final-only`, method `sbt-full-suite`. The payload already has `build.sbt`; bootstrap is `none`. In addition to the one complete suite, retain this Phase's focused validation, live manual acceptance, independent review, Step acceptance, closure ledger and local release. Verify predecessor closure before execution; a missing or contradictory handoff cannot be waived by fixtures. No other Phase or repository is added implicitly.

## Non-goals

Storage-protocol redesign, automatic external processing, general ingestion formats/OCR, full UI/LLM orchestration, live downstream TKW/Hub/BoK operations, a substitute shared service, external repository development, unrestricted concurrent writers, destructive cleanup, public publication/deployment and Git push.

## Packing and current structural gate

Phase 1 originally estimated 420 minutes; its near-release Goal observation recorded 48,244 seconds (about 804 minutes), a ratio of about 1.91. Native integration and assurance are partially comparable; startup/repair overhead is not a global multiplier. These bottom-up estimates reuse accepted models/native behavior and include new provider uncertainty, review, likely fixes, live evidence and release bookkeeping. Confidence is low to medium; timely test-account/operator access is assumed. Waiting indefinitely for unavailable inputs is not estimated as completed work.

Phase Plan Gate: PROCEED
- planning_demand: bounded-settled, conditional on the predecessor handoff
- recommended_parent_profile: gpt-6.1-sol / medium; reassess to high if semantic decisions remain
- profile_cost_role: lower-cost execution
- expensive_reasoning_kernel: none; predecessor owns provider and interface decisions
- frozen_profile_transition_handoff: accepted Phase 2 release and complete versioned storage/manual interface plus local/live receipts
- estimated_at_recommended_profile: 6h expected, 5-9h uncertainty range; at target, ceiling -2h
- target: calibrated expected duration centered on 6h; allowed ceiling 8h
- estimate_calibration: Phase 1 420m expected / approximately 804m observed near release / ratio 1.91; partial comparability, bottom-up estimate without a global multiplier
- parent_reasoning_mode_policy: standard
- incoming_semantic_handoffs: []
- merge_attempts_for_every_sub_4h_child: none
- rebalance_attempts_for_every_sub_5h_child: none
- adjacent_merge_structural_rejection_evidence: merged expected duration approximately 12h exceeds 8h ceiling
- profile_cost_only_rejection_forbidden: true
- short_child_basis: none
- overhead_tradeoff: about 1h additional closure/handoff; no net time or numeric cost saving claimed; complete suite runs only at final owner
- agent_reasoning_mode_policy: default standard; consider pro only at an eligible supported launch with frozen quality-first evidence
- runtime_suitability: re-evaluate in the Phase execution task
- source: applied split from Phase 2 on 2026-10-03


## Split provenance and next entry

Split from Phase 2 by `$cncf-split-phase 2` on 2026-10-03, reason `time-bound`. Consumes the durable high-to-medium handoff; no semantic boundary is asserted. No completed history was moved. The original manual/full-flow acceptance items occur only in [this checklist](phase-2.1-checklist.md). Storage integration history remains with the predecessor and the [original journal](../journal/2026-10-03-phase-2-definition.md).

Start only after Phase 2 closure, through an explicit `$cncf-goal-phase Phase 2.1` or interactive exact-identity workflow. This planned structural gate does not attest a future execution runtime.
