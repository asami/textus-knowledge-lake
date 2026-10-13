# Phase 2 — Google Drive Backend

status=planned
split_full_test_policy=final-only
split_full_validation_method=sbt-full-suite
split_validation_bootstrap=none
validation_ownership=aggregate-deferred
aggregate_validation_owner=PHASE-2.1
aggregate_validation_sequence=["PHASE-2","PHASE-2.1"]
depends_on=[Phase 1](phase-1.md)

- Current status: Planned; implementation not started
- Defined: 2026-10-03; split applied: 2026-10-03
- Owner and mutation scope: Textus Knowledge Lake / `textus-knowledge-lake`
- Reference baseline: Phase 1 release `f131d16a6dc98ff3ea754db6378aea64f090ff56`
- Predecessor: [Phase 1](phase-1.md); successor: [Phase 2.1](phase-2.1.md)
- Checklist: [Drive backend acceptance](phase-2-checklist.md)
- Historical definition and split decision: [Definition journal](../journal/2026-10-03-phase-2-definition.md)
- Update rule: Record actual source and acceptance evidence; planned declarations establish neither execution nor provider capability.

## Goal

Acquire a small real Google Drive source set and persist canonical, versioned TKL records in Drive with verified publication, conflict and restart-recovery behavior. Deliver a settled adapter and manual-processing interface handoff for Phase 2.1. This first unit retains the original Phase 2 identity. The original complete manual preparation/proposal flow is owned by Phase 2.1.

Drive folders hold TKL metadata and artifacts. KnowledgeProcessingContext remains TKL-owned; Drive Project is an external context binding. Identify its actual Google product/reference and package/import procedure during contract work; running a real manual preparation session belongs to Phase 2.1. No automatic processing API is assumed.

Use one TKL instance, one selected storage root and explicit source folders. Source folders are read-only from TKL; writes are confined to the storage root. Google Docs and UTF-8 text, including Japanese/newlines, are the minimum inputs. Preserve [canonical v1](../spec/preparation-contract-v1.md) and [accepted local flow](../spec/local-preparation-flow-v1.md) behavior. Existing released dependencies are allowed; development/builds/releases in another source repository are outside scope.

## Requirements

### P2-R01 — Explicit source selection and stable identity

Configure the source folder IDs, TKL storage root, supported media types, enumeration depth and size/count limits. Account-wide discovery is outside the initial profile. Apply pagination and an explicit shortcut policy without silently extending the selected source boundary. Report inaccessible or unsupported items.

Persist the mapping from provider identity to stable TKL Resource identity. Repeated discovery, a source rename or an in-scope move must not create a new TKL identity merely because its title or path changed. Keep Resource revision, Evidence revision, provider revision, snapshot ID and Run ID distinct. If no reliable provider revision is available, preserve that fact explicitly.

### P2-R02 — Content acquisition, retention and provenance

The minimum supported source set includes a Google Docs document and a UTF-8 text file, including Japanese text and line breaks. Select and document the Google Docs export representation after capability verification; exported text does not promise preservation of document layout. Rich document conversion, PDF/OCR and spreadsheet interpretation are later scope.

Retained Evidence contains the acquired text and an explicit representation/provenance description. Reference-only Evidence contains no retained text and remains visibly unretained. A read or export failure must never become an apparently successful empty snapshot. Freeze exact selected Evidence versions and the existing-information baseline before handing inputs to the operator. A later source or Context update must not rewrite those inputs.

### P2-R03 — Drive as canonical TKL storage

Store independently manageable, versioned JSON records and retained preparation artifacts in Drive. Define record layout, identity mapping, discovery and consistency rules in a versioned adapter specification before implementation. A manifest or index may coordinate records, but a single ever-growing lake-wide JSON aggregate is not the production storage design.

Drive is authoritative for this operational profile. Local caches are disposable; a fresh TKL process must recover committed records and reproduce bundle JSON and Markdown from Drive without relying on an earlier local store. Validate records and references when reading them. Malformed, unsupported-version or incomplete data produces an explicit error rather than an empty or partially valid lake.

Preserve the existing v1 canonical export fields, identity semantics and exact retained text. New provider metadata, storage envelopes or operation records need explicit versions and conversion rules. Do not add fields or states silently to the closed v1 wire formats.

### P2-R04 — Concurrency and consistent publication

Verify the chosen Drive operations and their actual conditional-update behavior before selecting a write protocol. A revision field in JSON, a provider version value or a read followed by an unconditional write is not sufficient evidence of compare-and-set behavior.

Define the supported writer model, expected-version checks, conflict result and publication boundary. Under that model, competing writes based on the same accepted version must not silently lose an update or both report acceptance of incompatible state. If a coordinator or restricted writer profile is necessary, specify and verify its enforcement and bypass limitations. An in-process lock alone cannot claim coordination across processes or external editors.

Multi-record publication must expose a complete validated logical state or a visible incomplete/unknown operation. Specify how a Run, its material and their references become visible together, and how readers detect missing records. Do not claim that independent Drive writes form a provider transaction. The existing aggregate `PreparationStore` CAS port may be implemented only if its observable contract is actually preserved; otherwise introduce an explicit provider-neutral persistence boundary and retain the accepted local adapter behavior.

### P2-R05 — Observable failures and resumable operations

Record enough durable operation identity, input/version selection and provider artifact references to reconcile interrupted writes and imports after restart. Separate known nonapplication, known application and an unknown outcome. An authorization failure, conflict, interrupted upload or lost response must not fabricate success or automatically assign a new logical material/proposal identity.

Recovery inspects existing artifacts and the accepted publication state before deciding whether to resume. Detect and report duplicate or orphaned provider artifacts without treating them as additional accepted material versions. A failed Markdown/export projection after canonical persistence remains distinguishable from failure to persist the canonical result. Document recovery steps and bounded retry behavior; automatic destructive cleanup is outside this Phase.

### P2-R08-A — Storage runtime and processing-interface handoff

Expose source acquisition, canonical persistence and restoration through TKL's supported native CML/released CNCF route. Inspect existing released provider facilities first; keep physical Drive access behind an explicit adapter. Document credential setup, permissions, folders, formats, limits and recovery. Tokens/secrets must not enter source, fixtures, logs or handoffs; authorization errors remain actionable.

Settle a versioned manual package/import contract before handing off: concrete external context binding, exact frozen Resource/Evidence/baseline selection, Run and operation identities, citations, finalized v1 Run states, known/unknown provenance, identical versus conflicting import, and projection failure after persistence. Provider operation tracking must not silently extend closed v1 wire formats. The dependent Phase implements this interface and performs the real session.

### P2-R09-A — Focused storage and live acceptance

Keep ordinary tests credential-free. Verify bounded discovery/pagination/shortcuts, stable IDs, exact acquisition, retained/reference-only distinctions, closed-envelope/reference validation, concurrency, partial/unknown writes, restart reconciliation and duplicate/orphan reporting using controlled provider responses. Retain affected Phase 1/native regressions.

Use an explicitly selected account/root and disposable test documents for real Docs/text reads, canonical writes, fresh-process recovery without prior cache, history after source edit, and same-version competing updates under the enforced writer model. Record actual application/adapter versions, observations and limitations. Label controlled faults separately; they cannot prove unobserved provider guarantees. Missing credentials/capabilities leave acceptance pending.

### P2-R10-A — Storage closure and integration handoff

Maintain a TKL-local integration register and storage owner proposal/handoff using stable TKL-INT-01..07 IDs. Preserve the historical Phase 1 package. Bind actual storage contract/source versions, sanitized examples, focused/live receipts, independent review, release and unresolved external-owner follow-ups. Full end-to-end closing proposals belong to Phase 2.1. No other project is an implicit implementation owner or new dependency.

## Steps and completion criteria

| Step | Owned result | Exit condition |
| --- | --- | --- |
| TKL-P2-01 | Capability evidence; storage/writer/recovery and manual-interface contracts | Implementable supported profile and downstream formats are settled before dependent coding |
| TKL-P2-02 | Drive discovery/acquisition, canonical persistence and recovery | Behavioral specs and real storage/history/restart/competing-write evidence pass |
| TKL-P2-04A | Storage guide, integration handoff, independent review and local release | Every storage criterion below is evidenced; aggregate complete validation remains assigned to Phase 2.1 |

1. Real supported Docs/text sources map to stable Resources and versioned Evidence with correct retention/provenance.
2. Canonical Drive records and their bundle/Markdown projections survive fresh-process recovery; publication, conflicts and interruption recovery meet the observed writer contract.
3. Credential-free focused storage/native regressions and scoped live storage observations pass and document limitations.
4. English adapter/operator documents and the concrete manual package/import handoff agree with implemented behavior and preserve v1 compatibility.
5. Independent review, Step acceptance, closure ledger, local release and integration handoff are complete, with repository-complete validation explicitly deferred to Phase 2.1.

## Validation ownership and handoff

The serial split sequence is Phase 2 -> Phase 2.1. Phase 2 omits only the repository-complete `sbt --batch test`; Phase 2.1 runs it once for both members. This Phase still owns focused behavior/consumer validation, live storage acceptance, independent reviews, Step commits, closure ledger and its release commit. Closure uses focused assurance with the aggregate gate pending; it must not claim a full-suite pass. No bootstrap entry is needed because `build.sbt` already exists.

The accepted Phase 2 release, versioned Drive record/publication/writer/recovery specification, credential/configuration interface without secrets, storage behavioral and live receipts, closed v1 compatibility constraints, and a concrete manual processing product/reference/procedure with frozen-package and result-import formats. Phase 2.1 consumes these artifacts; it must not rediscover or silently weaken storage guarantees. If the contracts remain unsettled, return to planning and reassess the runtime profile before dependent implementation.

## Non-goals and execution inputs

Actual manual processing/import, proposal export integration and original full-flow acceptance belong to Phase 2.1. Exclude automated Notebook/Drive Project orchestration, general formats/OCR, organization-wide indexing, unrestricted external writers, production HA, downstream TKW/Hub/BoK acceptance, automatic destructive cleanup, source edits in another project, publication/deployment/push and a mandatory separate database.

Select the live account/source and storage folder IDs, credential facility, Docs export representation, enforced writer protocol and concrete processing context. Use actual provider evidence; unavailable capabilities require an explicit design decision rather than a simulated success.

## Packing and current structural gate

Phase 1 originally estimated 420 minutes; its near-release Goal observation recorded 48,244 seconds (about 804 minutes), a ratio of about 1.91. Native integration and assurance are partially comparable; startup/repair overhead is not a global multiplier. These bottom-up estimates reuse accepted models/native behavior and include new provider uncertainty, review, likely fixes, live evidence and release bookkeeping. Confidence is low to medium; timely test-account/operator access is assumed. Waiting indefinitely for unavailable inputs is not estimated as completed work.

Phase Plan Gate: PROCEED
- planning_demand: open-ended-discovery
- recommended_parent_profile: gpt-6.1-sol / high
- profile_cost_role: expensive reasoning kernel
- expensive_reasoning_kernel: actual provider capabilities and interacting storage/publication/concurrency/recovery/manual-interface decisions
- frozen_profile_transition_handoff: produces the versioned handoff specified above
- estimated_at_recommended_profile: 7h expected, 6-10h uncertainty range; target +1h, ceiling -1h
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


## Dated split provenance — 2026-10-03

The explicit `$cncf-split-phase 2` invocation applied `split_reasons=["time-bound"]` to the still-unstarted Phase. The original 12h expected/18h upper estimate and `SPLIT_REQUIRED` are dated **pre-split gate evidence**, not this Phase's current gate. Numbering retains Phase 2 and adds Phase 2.1; the earlier tentative 2.1/2.2 labels were proposal labels only. There was no Phase 2 Goal or completed work (`none / no-goal-pre-entry`); Phase 1 history and original definition prose remain unchanged.

The storage decision kernel and its coherent acceptance occupy 7h at high; downstream settled execution occupies 6h at medium, conditional on the complete handoff. Approximately 1h of extra closure/handoff produces a 13h split estimate versus 12h unsplit; no net numeric saving is claimed. `final-only` avoids duplicating complete validation. Neither child is below five hours. Merging recreates the oversized original. Incoming semantic handoffs are `PHASE-2: []`, `PHASE-2.1: []`; the contract handoff is an ordinary serial dependency, not an invented semantic gate.

The [split note](../journal/2026-10-03-phase-2-definition.md#applied-split--2026-10-03) records exact moved requirements, original acceptance ownership and unchanged history. Planning documents may be validated and uncommitted at first entry; execution freezes and saves the complete split set through its ordinary acceptance/management commit.
