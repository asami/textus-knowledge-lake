# Phase 1 integration proposals v1

- Package: `TKL-P1-Integration-Proposals/1`
- Date: 2026-10-02; reconciliation: 2026-10-03
- Current status: Proposal-ready documentation of implemented local behavior; external acceptance unverified. Complete Step accumulator/review/native acceptance commit and final Phase review/full validation/release remain pending.
- Owner: Textus Knowledge Lake; each receiving owner owns its separately scoped follow-up below.
- Update rule: Preserve package identity and exact v1 contracts; version richer proposals explicitly. Change acceptance bindings only from actual accepted commits and attributable review/validation evidence. Never infer external acceptance from fixtures.
- Authority: [Phase 1](../../../phase/phase-1.md); [checklist](../../../phase/phase-1-checklist.md); [register](../../../notes/phase-1-integration-register.md).

## Source and contract inventory

Source application version: `0.1.0-SNAPSHOT`. Accepted Step 1 commit: `2511186de891e3eb40863bd14d898eca8701ef47`. Accepted Step 2 commit: `5778cfcad83f5a8353d91b1a29f1bf4adbb431f4`. Native Step 3 has not yet been independently accepted or committed.

| Artifact | Repository source |
| --- | --- |
| Preparation contract / `tkl.preparation-bundle.v1` | [Contract](../../../spec/preparation-contract-v1.md); [schema](../../../../schemas/tkl/preparation-bundle-v1.schema.json) |
| Local state / request / observation / fixture / report v1 | [Local-flow contract](../../../spec/local-preparation-flow-v1.md); [schema](../../../../schemas/tkl/local-flow-v1.schema.json) |
| Complete reproducible canonical bundle | [Bundle fixture](../../../../src/test/resources/tkl/preparation-bundle-v1.json) |
| Explicit source, manual result and delivery observations | [Local-flow fixture](../../../../fixtures/tkl/local-flow-v1.json) |
| Native CML and runtime contract | [CML](../../../../src/main/cozy/textus-knowledge-lake.cml); [native spec and portable commands](../../../spec/native-application-model-v1.md#local-development-startup) |
| Native coordinate and ABI | [Source descriptor](../../../../src/main/car/component-descriptor.json); [ABI](../../../../src/main/car/abi-manifest.json) |

Unchanged v1 schemas reject unknown fields and require explicit nullable fields; opaque IDs/text remain exact. No new runtime DTO or endpoint is declared. TKW owns Candidate formation/edit/review/approval/Admission, KnowledgeHub canonical Information, and BoK publication. TKL creates no substitute downstream store or receipt/deduplication guarantee.

Drive folder(s) are the first canonical production backend target, using small independently managed JSON records. Drive Project is the first external logical processing context, with manual preparation/import permitted. Notebook is optional later scope. Local fixtures prove no live provider capability.

## Observed local evidence

Accepted Step 2 recorded a 172-test accumulator and an actual local CLI. Corrected native CML passed the pinned parser/generator and compiler. Inspection of 13 generated Scala sources and retained model metadata preserved all 31 Use Cases, eight states, eight events, ten transitions, required command/result fields and primary main-flow/summary/pre/postconditions. The selected route is an ENTITY-associated StateMachine. Dedicated WORKFLOW/Composite execution, ApplicationCapability/UseCaseSlice projection and alternate ComponentStyle remain unverified.

Native test attempt 6 passed released runtime preparation, retained canonical source/target descriptor and framework integrity inputs, and preserved generated fidelity. All eight unchanged NativeFlowSpec cases passed: 8 succeeded, 0 failed/canceled/ignored/pending. Normal generated Component dispatch exercised persisted success, initialized-store refusal, Applied projection failure, malformed/missing fixture, missing arguments, invalid path and excluded CRUD/future targets. The representative native CLI succeeded at revision 7 with `unknown` then `confirmed`, reporting actual persisted state and exports. FixtureExecutionResult is RecordPresentable carried through standard OperationResponse(Record), without claiming OperationResult inheritance.

### Actual startup diagnostic history

Attempt 4 stopped before tests because the style-less CML route needed its canonical source component descriptor. After that prerequisite, attempt 5 completed preparation but all eight tests stopped at the sole-component guard; CLI5 was not reached. Startup exposed 13 builtins plus KnowledgeLake. Public upsert already matched `org.simplemodeling.textus.KnowledgeLake@default` and replaced the local-first-loaded implementation with the parent AppClassLoader implementation; Fixed authentication remained intact. The builtins caused the remaining guard failure, disproving the duplicate-identity hypothesis. The normal descriptor was corrected to exclude exactly the 13 fully qualified names in the [startup contract](../../../spec/native-application-model-v1.md#local-development-startup). Released filtering matches complete trimmed/lowercased Component.name values; short aliases and `--no-default-components` did not remove this descriptor-factory set. Native test/CLI attempt 6 then passed, with public upsert and real dispatch preserved.

### Actual CLI semantic excerpt

This excerpt omits machine-specific output paths and is not the full original report. Full unchanged output remains retained evidence. The portable command in the native spec illustrates reproduction and is not the executed absolute argv.

```json
{
  "schema": "tkl.local-flow-report.v1",
  "storeRevision": 7,
  "runId": "run-local-001",
  "materialRef": {
    "id": "material:準備",
    "revision": 1
  },
  "proposal": {
    "producerId": "textus-knowledge-lake",
    "proposalId": "proposal-local-001"
  },
  "outcomes": [
    "unknown",
    "confirmed"
  ],
  "assurance": "fixture-only; external guarantees unverified"
}
```

## Exact decoded local fixture samples

These actual decoded fixture request/observations prove TKL handling only; confirmation is simulated. Complete Context, baseline and retention/unknown provenance are available in the linked bundle/local-flow fixtures.

### Submit request

```json
{
  "schema": "tkl.proposal-request.v1",
  "correlationId": "fixture-submit-001",
  "operation": "submit",
  "proposal": {
    "producerId": "textus-knowledge-lake",
    "proposalId": "proposal-local-001",
    "producingRunId": "run-local-001",
    "materialRef": {
      "id": "material:準備",
      "revision": 1
    },
    "title": "準備資料 🌱",
    "summary": "Local preparation from retained text and reference metadata.",
    "evidenceRefs": [
      {
        "id": "evidence:資料/α",
        "revision": 1
      },
      {
        "id": "evidence:資料/α",
        "revision": 2
      }
    ]
  }
}
```

### Submit observation

```json
{
  "schema": "tkl.proposal-observation.v1",
  "correlationId": "fixture-submit-001",
  "producerId": "textus-knowledge-lake",
  "proposalId": "proposal-local-001",
  "outcome": "unknown",
  "receiptId": null,
  "candidateRef": null,
  "message": "Simulated fixture timeout; external application and delivery outcome are unknown."
}
```

### Query observation

```json
{
  "schema": "tkl.proposal-observation.v1",
  "correlationId": "fixture-query-001",
  "producerId": "textus-knowledge-lake",
  "proposalId": "proposal-local-001",
  "outcome": "confirmed",
  "receiptId": "fixture-receipt-001",
  "candidateRef": {
    "id": "fixture-candidate-001",
    "revision": 1
  },
  "message": "Simulated fixture confirmation; no authenticated downstream receipt or Candidate persistence is proved."
}
```

## Conceptual TKW requirements versus minimal v1

Richer requirements remain owner proposals and cannot silently become v1 fields. TKL-TKW-01..07 cover discovery proposal integration, 08..10 later feedback, and 11..12 later reverse preparation.

| Conceptual requirement | Minimal v1 mapping / limitation |
| --- | --- |
| `schemaVersion` | Root schema literal tkl.proposal-request.v1 / tkl.proposal-observation.v1; schema field, not conceptual schemaVersion field |
| `producerRef / proposalId` | producerId / proposalId; opaque producer string is not authenticated identity |
| `candidateKind / suggestedContent` | title/summary are implemented; candidate kind/richer structured content are not v1 fields |
| `preparedMaterialRefs` | One materialRef with exact id/revision; no URL/access resolver in the proposal |
| `evidenceRefs / provenanceRef` | evidenceRefs on proposal; complete retained/unknown provenance available through producing Run/bundle, no separate provenanceRef field |
| `sourceContextId / sourceRunId` | producingRunId on proposal; fixed Context ref/baseline available in RunInput, no direct sourceContextId field |
| `existingInformationRefs` | RunInput.context.existingInformation contains versioned baseline/snapshot; no direct proposal field |
| `createdAt / correlationRef` | createdAt absent; request.correlationId identifies an attempt and is separate from proposal identity |

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

## Release binding and remaining acceptance

Native Step 3 acceptance commit: **pending**. Before actual Phase closure, release bookkeeping must replace this pending binding with the actual independently accepted Step 3 Git commit/tree and final validation/review evidence. Do not fabricate a future commit or bind this package to its own future commit. Phase 1 is not closed.

The current complete Step six-suite accumulator and representative CLI, independent Step review/acceptance commit, sole Phase full review, final full validation and release are pending. Earlier focused native 8/8 and CLI6 evidence is retained, without being rebound to edited documentation or substituted for complete acceptance.

All 11 pending M2 review IDs remain: TKL-P1-03A-VAL-001 through TKL-P1-03A-VAL-006 and TKL-P1-03B-VAL-001 through TKL-P1-03B-VAL-005. Slice A retains original three/additional one validation histories; Slice B retains original three/additional two. Step and Phase repair cycles remain zero. Accepted nonblocking findings remain an empty typed ledger set; no empty Hygiene or Development Candidate journals are created. Later independent findings require parent-owned typed disposition/materialization.

## Change history

- 2026-10-03: Published package /1 with exact v1 sources, samples and admitted native evidence; reconciled seven owner boundaries. External agreement remains unverified and native acceptance/release binding pending.
