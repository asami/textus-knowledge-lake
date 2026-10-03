# Phase 1 Integration Register

- Date: 2026-10-02; reconciled 2026-10-03
- Current status: Seven boundaries reconciled to implemented-local / proposal-ready / later-scope; external acceptance unverified; native Step acceptance and Phase closure pending.
- Owner: Textus Knowledge Lake
- Update rule: Preserve stable IDs and frozen v1 contracts. Record attributable evidence, compatibility decisions and owner/resume conditions; version richer proposals explicitly. Fixture success never establishes external agreement.
- Authority: [Project-local Phase 1](../phase/phase-1.md)
- Package: [TKL-P1-Integration-Proposals/1](../journal/2026/10/2026-10-02-phase-1-integration-proposals-v1.md)

Source application version: `0.1.0-SNAPSHOT`. Accepted Step 1: `2511186de891e3eb40863bd14d898eca8701ef47`; accepted Step 2: `5778cfcad83f5a8353d91b1a29f1bf4adbb431f4`. Native Step 3 commit/tree and final review/full-validation/release binding remain pending. External implementation/acceptance is not a Phase closure dependency.

Locally readable sources: [preparation contract](../spec/preparation-contract-v1.md), [local-flow contract](../spec/local-preparation-flow-v1.md), [native spec](../spec/native-application-model-v1.md), [canonical schema](../../schemas/tkl/preparation-bundle-v1.schema.json), [local-flow schema](../../schemas/tkl/local-flow-v1.schema.json), [complete bundle](../../src/test/resources/tkl/preparation-bundle-v1.json), [local-flow fixture](../../fixtures/tkl/local-flow-v1.json). The package contains exact samples and all receiving-owner acceptance examples.

## TKL-INT-01

- Owner: Textus Knowledge Workbench
- Current status: proposal-ready; implemented-local TKL export/reservation/result handling
- Purpose and input/output: tkl.proposal-request.v1 with submit/query and immutable RawCandidateProposal -> tkl.proposal-observation.v1 four closed outcomes
- Package / source version: `TKL-P1-Integration-Proposals/1` / `0.1.0-SNAPSHOT`; accepted Step 1/2 commits listed above.
- Contract versions: `schemas/tkl/local-flow-v1.schema.json`; `schemas/tkl/preparation-bundle-v1.schema.json`
- Local evidence: Accepted Step2 172-test accumulator and actual local CLI; native-test6 8/8 and CLI revision7. ProposalDeliverySpec preserves key, correlation, uncertainty and local reservation/result failures.
- External unverified guarantees: Authenticated producer, one Candidate/durable receipt under simultaneous identical submissions, transaction and recovery, lookup retention, reference access, lifecycle/review/approval/Admission.
- Next owner work: TKW owner reviews TKL-TKW-01..07 against this minimal envelope, chooses receiving scope and versions any richer common API in its own repository.
- Resume condition: Separately scoped receiver work, agreed exact schema/identity/outcome mappings and independent receiver durability/concurrency evidence.

Compatibility choices / open questions:

- Versioned minimal v1 mapping to richer conceptual TKW requirements; no silently added v1 fields.
- Producer authentication mapping, same-content equality, operation names/transport, receipt retention, candidate reference and payload/lineage resolution.

Proposed receiving-owner acceptance examples:

- Identical replay or concurrent submissions resolve to the same durable receipt/Candidate without resetting human edits.
- Different immutable payload under same producer/proposal key returns conflict, retaining stored proposal and candidate.
- Lost response after receiver commit followed by same-key query resolves the durable outcome.
- Inject candidate/receipt persistence faults: never return premature success or duplicate on recovery.
- Preserve original evidence/material/run versions and expose reference-only and unknown inputs.

## TKL-INT-02

- Owner: TKL Google Workspace adapter / provider integration owner
- Current status: proposal-ready; implemented-local provider-neutral snapshots/manual result import
- Purpose and input/output: Resource provider mapping, exact Evidence/Context/Run inputs and manual PreparationResult -> versioned PreparedMaterial/bundle/Markdown
- Package / source version: `TKL-P1-Integration-Proposals/1` / `0.1.0-SNAPSHOT`; accepted Step 1/2 commits listed above.
- Contract versions: `tkl.preparation-bundle.v1`; `tkl.local-flow-fixture.v1`
- Local evidence: Fixtures represent Drive resources, drive-project binding, retained Japanese text, reference-only unknown content and baseline revision3; local CAS/partial failure specs and native persisted outputs pass.
- External unverified guarantees: Google API operations/permissions, conditional update atomicity, retention/export, Drive Project automation or processor-visible inputs. Local aggregate store is fixture adapter evidence, not a mandatory production Drive aggregate.
- Next owner work: Prioritize Drive folder(s) as canonical TKL backend with Drive Project processing context; start a small manually assisted live verification after separate authorization.
- Resume condition: Provider adapter scope authorized and live permissions/update/retention/import behavior independently verified.

Compatibility choices / open questions:

- Small independently managed Drive JSON records, CAS/arbitration and permission/retention policy.
- Verified context API and manual result provenance; actual Gemini Notebook product/API remains an optional later adapter question.

Proposed receiving-owner acceptance examples:

- Two writes using the same expected provider revision cannot silently lose one update; verify actual capability or an explicit owner-selected coordinator.
- Historical retained input remains accessible after source change/deletion; a reference-only input remains visibly unretained.
- External artifact created but import/update fails: retain observable partial result and resume without treating unknown as absence.
- Manual Drive Project preparation imports exact returned content with only observed actor/input facts; Context change leaves prior Run snapshots untouched.

## TKL-INT-03

- Owner: TEAI / OpenClaw integration owners
- Current status: proposal-ready; no physical transport implemented
- Purpose and input/output: Exact proposed request/result plus correlation/producer context -> observable success, structured failure or explicit unknown/continuation
- Package / source version: `TKL-P1-Integration-Proposals/1` / `0.1.0-SNAPSHOT`; accepted Step 1/2 commits listed above.
- Contract versions: `tkl.proposal-request.v1`; `tkl.proposal-observation.v1`; `PreparationResult in local-flow v1`
- Local evidence: Stateless fixture port validates result correspondence, exceptions/timeouts, durable reservation and unrecorded completion; no transport/authentication proof.
- External unverified guarantees: Existing physical operations, policy, OAuth, authenticated identity, cancellation/deadline semantics, continuation and redelivery.
- Next owner work: Owners map these semantic obligations onto their verified existing facilities; record gaps and implementation proposals in their repositories.
- Resume condition: Physical facility selected and independently validated under a separately authorized integration scope.

Compatibility choices / open questions:

- Existing operations and supported physical request/result mapping must be inspected by owner before selecting any new shared capability.
- Authentication identity, deadlines and effect visibility across retries/continuations.

Proposed receiving-owner acceptance examples:

- Timeout after possible remote effect reports unknown and retains original key/correlation.
- Mismatched producer/proposal/correlation cannot become success.
- Transport retries/continuations preserve immutable payload and resolve pending state explicitly.

## TKL-INT-04

- Owner: KnowledgeHub canonical Information owner and TKW Admission/feedback owner
- Current status: later-scope; implemented-local versioned ExistingInformation snapshot only
- Purpose and input/output: Versioned canonical Information and admission/candidate trace -> immutable Run baseline and future current-context projection
- Package / source version: `TKL-P1-Integration-Proposals/1` / `0.1.0-SNAPSHOT`; accepted Step 1/2 commits listed above.
- Contract versions: `ExistingInformation and RunInput in tkl.preparation-bundle.v1`
- Local evidence: Fixture baseline information:baseline@3 and snapshot:baseline-3 retained in each fixed Run input; no current-query or synchronization execution.
- External unverified guarantees: Admission, current-state access, trace, event delivery/order/gap recovery and projection reconciliation.
- Next owner work: Follow TKL-TKW-08..10 with owners; design feedback/current-state/rebuild in later scoped work, preserving KnowledgeHub authority.
- Resume condition: Canonical version/query/change contracts and later TKL feedback scope accepted and validated.

Compatibility choices / open questions:

- Canonical query/change versions, trace from proposal through approved Candidate to Information, incremental delta and rebuild contracts.

Proposed receiving-owner acceptance examples:

- Canonical version update never rewrites a previous Run baseline.
- Duplicate/out-of-order/missing events reconcile through version-aware handling and authoritative current-state query.
- Retire superseded artifacts from active processing context while retaining history.

## TKL-INT-05

- Owner: TKW candidate-preparation request owner and later TKL preparation adapter owner
- Current status: later-scope; conceptual requirements only, no implemented API/schema
- Purpose and input/output: CandidatePreparationRequest requestId/candidate revision/resources/purpose -> correlated Run/PreparedMaterial/provenance result
- Package / source version: `TKL-P1-Integration-Proposals/1` / `0.1.0-SNAPSHOT`; accepted Step 1/2 commits listed above.
- Contract versions: `Conceptual CandidatePreparationRequest in package TKL-P1-Integration-Proposals/1; no machine-readable wire schema or endpoint is declared`
- Local evidence: Existing Run/material/versioned-resource contracts may be reused; reverse request/result path has no implementation or executable acceptance here.
- External unverified guarantees: Request idempotency, stale-candidate policy, duplicate results, receipt and application decisions.
- Next owner work: Design TKL-TKW-11..12 after initial discovery proposal integration; keep candidate lifecycle in TKW.
- Resume condition: Separately authorized reverse-path design and mutually reviewed versioned schema/correlation/application rules.

Compatibility choices / open questions:

- Request/result schema version, receiving idempotency, stale revision and explicit apply policy.

Proposed receiving-owner acceptance examples:

- Repeated result associates once to the exact requested candidate/revision.
- Candidate edited/held/rejected during preparation retains returned material without automatic overwrite or approval.
- Failed/partial/unknown preparation stays observable and cannot be reported as success.

## TKL-INT-06

- Owner: CML / Cozy / sbt-cozy / CNCF tooling owners
- Current status: implemented-local selected supported native route; proposal-ready capability follow-up
- Purpose and input/output: Pinned native CML and canonical startup metadata -> generated Component/DTO/ActionCall/model projections and real local dispatch
- Package / source version: `TKL-P1-Integration-Proposals/1` / `0.1.0-SNAPSHOT`; accepted Step 1/2 commits listed above.
- Contract versions: `native application model v1`; `source ABI and component descriptor`; `Scala3.3.8 / SBT1.12.6 / sbt-cozy0.1.16 / Cozy0.3.2.2 / CNCF0.5.2`
- Local evidence: Actual13 generated Scala sources preserve31UCs,8states,8events,10transitions, required DTO fields and primary main-flow/pre/postconditions; native-test6 prepared runtime,8/8 cases and CLI success. Named result is RecordPresentable via standard OperationResponse(Record), not OperationResult inheritance.
- External unverified guarantees: Dedicated WORKFLOW/Composite execution, ApplicationCapability/UseCaseSlice projection and alternate ComponentStyle behavior have not been accepted. No class-absence inference or substitute framework.
- Next owner work: Use the proved released route now. Owners investigate only future required shared projections and startup usability; do not make their development a Phase1 dependency.
- CAR lint observation: The prior strict checker failed; the bounded project kind, conventional source layout and help/manual conformance repair is pending fresh validation and independent acceptance. The source v2 ABI was loaded by the runtime, while the linter displayed `unknown:0.0.0`. Its public-dependency probe used `org.goldenport:goldenport-cncf_3.3:0.5.2`, versus the actual Scala 3 dependency `org.goldenport:goldenport-cncf_3:0.5.2`. Latest sbt-cozy metadata lookup was unverified; the used release remains `0.1.16`. These observations do not establish missing upstream capability or require an upgrade.
- Local boundary and remaining limits: Direct host filesystem use belongs to the accepted local fixture/export and atomic/CAS adapter and the owned ephemeral native bootstrap. Required path/report strings are single internal representations. No published CAR, previous-release ABI baseline, warehouse, live Help/Web or MCP acceptance is established; local source/runtime evidence does not substitute for them.
- Proposed tooling verification: Owners verify source v2 ABI identity reporting, Scala 3 public-dependency coordinate selection and metadata lookup with Scala `3.3.8`, SBT `1.12.6`, sbt-cozy `0.1.16`, Cozy `0.3.2.2` and CNCF `0.5.2`, retaining actual diagnostics and released-route behavior. Any publication/compatibility or future provider work requires separate scope; upstream implementation is not a prerequisite for local Phase 1 closure.
- Resume condition: Separate tooling investigation/implementation scope with exact parser/projection/runtime evidence and consumer compatibility.

Compatibility choices / open questions:

- Whether existing supported facilities cover future workflow/capability projection; no unsupported conclusion without actual capability evidence.

Proposed receiving-owner acceptance examples:

- Owners confirm any future dedicated WORKFLOW/Composite or Capability/UseCaseSlice construct through parser, projected semantics and runtime behavior.
- If physical OperationResult inheritance is required later, test generated base/interface and ABI explicitly; current accepted named Record payload is preserved.
- Retain actionable diagnostics for style-less canonical source metadata and exact fully qualified builtin exclusions.

## TKL-INT-07

- Owner: BoK publication / curation owner downstream of TKW and KnowledgeHub
- Current status: later-scope; no TKL publication operation
- Purpose and input/output: Admitted Information version/trace and separate curation decision -> publication outcome
- Package / source version: `TKL-P1-Integration-Proposals/1` / `0.1.0-SNAPSHOT`; accepted Step 1/2 commits listed above.
- Contract versions: `Conceptual downstream publication requirements in package TKL-P1-Integration-Proposals/1; no new schema or operation`
- Local evidence: Ownership and exclusion documented; no local publication, curation or external validation claimed.
- External unverified guarantees: Publication policy, human curation, trace and receiving operation.
- Next owner work: BoK owner evaluates publication only after actual TKW/KnowledgeHub lifecycle exists; no TKL Phase1 prerequisite.
- Resume condition: Separate publication scope and authority plus accepted upstream canonical Information trace.

Compatibility choices / open questions:

- Owner-selected publication contract and curation conditions.

Proposed receiving-owner acceptance examples:

- Publication resolves an actual admitted Information version and explicit curation authority.
- Proposal receipt/fixture confirmation alone never implies publication authorization or completion.

## Compatibility and closure bookkeeping

The package's [minimal-v1 matrix](../journal/2026/10/2026-10-02-phase-1-integration-proposals-v1.md#conceptual-tkw-requirements-versus-minimal-v1) freezes richer-field limitations. TKL-TKW requirements are requesting-side proposals, not an accepted downstream API. Proposal-ready means ready for owner review; no external agreement or implementation is implied. No external posting or other-repository update is required for local closure.

Before closure, bind the package to the actual accepted native Step 3 commit/tree and final review/full-validation evidence. Partial checklist completion does not close the Phase.

## Change history

- 2026-10-02: Initial seven stable boundary IDs and owner obligations recorded.
- 2026-10-03: Reconciled every boundary against accepted model/local flow, native 8/8 and CLI revision 7; froze package /1, versions, compatibility questions, concrete owner work and resume conditions. External limitations and pending acceptance/release remain explicit.
