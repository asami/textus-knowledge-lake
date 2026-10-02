# Local preparation flow v1

The package `org.simplemodeling.textus.knowledgelake.local` implements TKL's project-local preparation, durable proposal reservation and fixture result handling. The immutable [preparation-bundle v1](preparation-contract-v1.md) model, public codec API and format, and Markdown projection remain unchanged. The [local-flow schema](../../schemas/tkl/local-flow-v1.schema.json) describes proposed TKL envelopes. These envelopes are not an accepted TKW or TEAI API.

The [repository fixture](../../fixtures/tkl/local-flow-v1.json) uses manually supplied retained text and reference metadata. Its simulated timeout and confirmation are observations of fixture assertions. Receipt and Candidate refs are opaque fixture data. There is no Candidate store, remote receipt persistence, deduplication service, authentication, transport implementation or downstream review/Admission lifecycle.

## Local API

All operations use explicitly supplied IDs and exact immutable values. Metadata follows ECMAScript nonblank semantics, including Unicode whitespace and BOM; values are never trimmed or normalized. Literal content and section text remain exact, including empty strings and newlines. TKL refs, provider IDs/revisions, entity revisions, Run/Input IDs, producer-scoped proposal keys, delivery correlation IDs, local store revisions and Candidate fixture refs have separate meanings.

| Type | Fields or values |
| --- | --- |
| `LocalWriteOutcome` | `NotApplied` (`not-applied`), `Unknown` (`unknown`), `Applied` (`applied`) |
| `LocalError` | `code: String`, `message: String`, `writeOutcome: LocalWriteOutcome`, `observedRevision: Option[Long]` |
| `LocalState` | `revision: Long`, `bundle: PreparationBundle`, `deliveries: Vector[DeliveryAttempt]` |
| `LocalDraft` | `bundle: PreparationBundle`, `deliveries: Vector[DeliveryAttempt]` |
| `StoreWriteStage` | `TemporarySynced`, `BeforePublish`, `AfterPublish` |
| `MaterialDraft` | `ref: VersionedRef`, `title: String`, `summary: String`, `sections: Vector[MaterialSection]` |
| `PreparationResult` | `processor: Provenance`, `status: RunStatus`, `failures: Vector[ProcessingFailure]`, `material: Option[MaterialDraft]` |
| `ProposalKey` | `producerId: String`, `proposalId: String` |
| `DeliveryOperation` | `Submit` (`submit`), `Query` (`query`) |
| `ProposalRequest` | `correlationId: String`, `operation: DeliveryOperation`, `proposal: RawCandidateProposal` |
| `ProposalOutcome` | `Confirmed` (`confirmed`), `Rejected` (`rejected`), `Conflict` (`conflict`), `Unknown` (`unknown`) |
| `ProposalObservation` | `correlationId: String`, `producerId: String`, `proposalId: String`, `outcome: ProposalOutcome`, `receiptId: Option[String]`, `candidateRef: Option[VersionedRef]`, `message: String` |
| `DeliveryAttempt` | `request: ProposalRequest`, `observation: Option[ProposalObservation]` |

`PreparationStore` exposes:

```scala
def read(): Either[LocalError, LocalState]
def compareAndSet(expectedRevision: Long, draft: LocalDraft): Either[LocalError, LocalState]
```

`FilePreparationStore(directory: java.nio.file.Path, observer: StoreWriteObserver = StoreWriteObserver.NOOP)` implements the port. `StoreWriteObserver.observe(stage: StoreWriteStage): Unit` is a deterministic local failure-injection seam. `NOOP` does nothing. It does not authorize automatic retry.

`LocalPreparationStore.EMPTY` is the exact virtual state at revision zero: all six bundle vectors and deliveries empty. Pure `validateState(state): Either[Vector[ModelError], LocalState]` admits zero only for this exact value. Pure `validateTransition(previous, draft): Either[Vector[ModelError], LocalDraft]` checks a complete proposed transition and returns the validated draft. Neither helper calls a codec or performs I/O.

`PreparationFlow` exposes:

```scala
def freezeInput(bundle: PreparationBundle, contextRef: VersionedRef,
  inputId: String): Either[Vector[ModelError], RunInput]
def prepare(store: PreparationStore, expectedRevision: Long,
  contextRef: VersionedRef, inputId: String, runId: String,
  result: PreparationResult): Either[LocalError, LocalState]
def propose(store: PreparationStore, expectedRevision: Long,
  producerId: String, proposalId: String,
  materialRef: VersionedRef): Either[LocalError, LocalState]
```

`freezeInput` validates the supplied bundle, resolves the exact Context, resolves selected Evidence in Context order and resolves only its Resource refs in first-occurrence order. It embeds the exact Context, baseline, bindings, retention and provenance. The RunInput ID must be explicitly nonblank. There is no provider lookup or new content inference.

`prepare` reads the current state and verifies the expected local revision. It freezes the current exact selection, constructs the supplied Run ID/Input ID and appends the Run and optional material in one CAS. Supplied processor and failure values are retained exactly. Existing model validation enforces all reference, selection, status and lineage rules. Failed Runs cannot carry material, failed Runs without material are observable, and partial Runs can retain material with explicit failures. A fault cannot split the Run/material append into two primary states.

`propose` resolves the exact material and producing Run, derives title and summary from material and derives citations by first-occurrence union of its section refs. The caller supplies producer and proposal IDs. An existing `(producerId, proposalId)` returns current state without a write only if the derived proposal is identical and the expected store revision still matches. Differing content under the same key returns `immutable-conflict`. Another producer may use the same local proposal ID. A new proposal appends in one CAS.

`ProposalTransport.exchange(request: ProposalRequest): ProposalObservation` is the local invocation port. `FixtureProposalTransport(observations: Vector[ProposalObservation])` performs a stateless exact lookup by correlation and producer/proposal key. Missing lookup returns matching Unknown with null receipt/Candidate and an explicit missing-fixture explanation. It creates no receiving service or remote state.

`ProposalDelivery.execute(store: PreparationStore, expectedRevision: Long, request: ProposalRequest, transport: ProposalTransport): Either[LocalError, LocalState]` owns reservation, invocation and result recording.

## Local primary persistence

Only the explicitly selected store directory is used. Primary filenames are fixed `state.json` and stable `state.lock`; each publication uses a unique `.state-*.tmp` in that directory. Opaque entity/provider IDs never select paths. The adapter rejects symlink store directories, state/lock symlinks and nonregular state/lock files with `NOFOLLOW_LINKS` checks. This is a narrowly scoped adapter rule, not a hostile-filesystem security proof or shared-filesystem credential design.

Missing primary state reads as virtual revision zero without eager state creation. Published state revisions are positive Scala `Long` values. Corrupt, wrong-schema or invalid-lineage state returns a structured error; CAS rereads it and refuses to overwrite it. Reads use only published `state.json`. Interrupted or unknown temporary files are ignored; there is no adoption, recovery migration or unsolicited cleanup.

CAS acquires a nonblocking exclusive FileChannel lock on the stable lock file. An overlapping JVM lock or an unavailable other-process lock returns `busy` / NotApplied without changing primary state. Under lock, CAS rereads the published state, compares the exact expected revision and reports `revision-conflict` / NotApplied with the actual observed revision. It rejects `Long.MaxValue` exhaustion before incrementing. It validates state and transition before writing. Only the local store revision increments; entity/Run/Input/proposal identities never change automatically.

The complete canonical UTF-8 local-state JSON is written to a newly created temporary file, forced with `force(true)` and closed. The observer sees `TemporarySynced`, then `BeforePublish`. Publication uses `Files.move(temp, state, ATOMIC_MOVE, REPLACE_EXISTING)` with no fallback. After the move returns, the observer sees `AfterPublish`. Lock and channel resources are released on every path. Cleanup is best effort and limited to that operation's own temporary file.

Every top-level bundle vector is an append-only prefix of exact immutable objects. Deletion, reordering or changes under a stored ref, Run ID or proposal key are rejected. A reused RunInput ID denotes an equal entire RunInput. A reused InputSnapshot ID across top-level Evidence, Context baselines and historical Run inputs denotes an equal entire snapshot. Distinct model namespaces remain distinct. New Context/material revisions can append while historical inputs remain unchanged.

Attempt request values and order are immutable. Existing observations are immutable. A prior null observation may become one valid observation, exactly once. New attempts append with null observation only. Every attempt payload equals the stored proposal under its producer-scoped key, every observation matches its own request and all correlations are globally unique. Request replacement, result rewriting, deletion/reordering, duplicate correlations and new already-observed attempts are rejected.

| Failing operation | Outcome and meaning |
| --- | --- |
| Invalid draft, corrupt read, busy lock, revision conflict/exhaustion | NotApplied; this operation did not publish |
| Probe/I/O/observer failure before invoking the move | `io`, NotApplied; old published bytes remain untouched |
| `AtomicMoveNotSupportedException` from an unapplied move | `io`, NotApplied; no non-atomic fallback |
| Other failure after move invocation, or any fault after successful move | `io`, Unknown; application may have occurred, an explicit read resolves actual state |
| Export I/O after known committed primary state | `projection-write`, Applied; report includes known primary revision |

Once the move returned successfully, even an `AtomicMoveNotSupportedException` raised by a later observer is Unknown. Unknown never asserts commit absence; an available observed revision is not a rollback guarantee. LocalWriteOutcome describes the failing operation independently of earlier successful operations in the same command.

When locking and atomic move are supported, this adapter supplies cross-instance/process optimistic CAS and complete old/new publication. Before-publication faults preserve prior published bytes; postpublication uncertainty can be resolved by read. It promises no power-loss or directory-fsync guarantee, Drive/remote concurrency or retention equivalence, authentication proof or TKW receipt idempotency.

## Delivery lifecycle and recovery

Before invoking the port, delivery reads state, verifies expected revision, checks nonblank correlation and checks that the entire request proposal equals the stored producer-key proposal. Correlations are globally unique. First Submit is allowed. Query requires a prior unresolved attempt. Later Submit or Query is permitted only when the latest attempt for that same key is pending or Unknown. Confirmed, Rejected and Conflict are terminal for the frozen key; no later invocation is allowed.

The request is saved as `DeliveryAttempt(request, None)` by CAS before exchange. Failed or uncertain reservation never invokes the port. After successful reservation, exchange runs at most once. Returned correlation and producer/proposal key must all match. Malformed or mismatched assertions become matching Unknown with explanatory message and null receipt/Candidate, preserving the original request. A NonFatal port exception also becomes matching Unknown; a timeout never implies rejection or nonapplication.

Confirmed requires both a nonblank receipt ID and a positive versioned Candidate ref. Rejected, Conflict and Unknown require both fields null. All required observation metadata is ECMAScript nonblank and remains unnormalized. These are assertions from the selected fixture port, not authenticated downstream proof.

The exact validated observation is recorded by a second CAS at the reservation's returned revision. There is no automatic exchange or CAS retry. Final CAS conflict/error returns `result-not-recorded`, Unknown, with the original cause in its message and any available observed revision. It never returns persisted success. On confirmed nonapplication/conflict the pending reservation remains; a later explicit Query uses a new correlation with the same key/payload. If the final CAS may have applied before a fault, an explicit read resolves pending versus observed. Completed primary state is never reverted to fit an assumed pending result. If the read exposes a terminal assertion, further Submit/Query is rejected locally.

Recovery preserves the proposal key and payload. It never infers a receipt, generates a new proposal ID or claims downstream deduplication/idempotency.

## Strict wire formats

Every object rejects unknown fields and requires all declared fields. Optional values use explicit null. Arrays and retained strings preserve exact order/content. Revisions are integers in `1..9223372036854775807`; revision zero is never serializable persisted state. Enum strings are closed. Nested requests/observations carry their own schema markers, and nested canonical bundles carry `tkl.preparation-bundle.v1`.

| Root schema | Exact fields in canonical declaration order |
| --- | --- |
| `tkl.local-state.v1` | `schema`, `revision`, `bundle`, `deliveries` |
| `tkl.proposal-request.v1` | `schema`, `correlationId`, `operation`, `proposal` |
| `tkl.proposal-observation.v1` | `schema`, `correlationId`, `producerId`, `proposalId`, `outcome`, `receiptId`, `candidateRef`, `message` |
| `tkl.local-flow-fixture.v1` | `schema`, `initial`, `preparation`, `proposal`, `deliveries` |
| `tkl.local-flow-report.v1` | `schema`, `storeRevision`, `runId`, `materialRef`, `proposal`, `outcomes`, `bundlePath`, `markdownPath`, `requestPath`, `assurance` |

New nested records use their exact DTO field names. `DeliveryAttempt` has `request`, `observation`. `PreparationCommand` has `contextRef`, `inputId`, `runId`, `result`. `PreparationResult` has `processor`, `status`, `failures`, `material`; status strings match preparation-bundle v1. `MaterialDraft` has `ref`, `title`, `summary`, `sections`; sections retain existing MaterialSection fields. `ProposalCommand` has `producerId`, `proposalId`, `materialRef`. `FixtureDelivery` has `correlationId`, `operation`, `observation`. `ProposalKey` has `producerId`, `proposalId`.

`LocalFlowFixture(initial: PreparationBundle, preparation: PreparationCommand, proposal: ProposalCommand, deliveries: Vector[FixtureDelivery])` contains every explicit input. Its seed has no Runs/materials/proposals. It supplies a prepared material for the proposal and at least one delivery for the first-request export. Fixture decoding validates source selections, supplied preparation lineage and matching delivery assertions.

`LocalFlowReport(storeRevision: Long, runId: String, materialRef: VersionedRef, proposal: ProposalKey, outcomes: Vector[ProposalOutcome], bundlePath: String, markdownPath: String, requestPath: String, assurance: String)` reports actual persisted revision, recorded outcomes and fixed export paths. Assurance is exactly `fixture-only; external guarantees unverified`.

`LocalFlowJson` exposes these strict current-v1 boundaries:

```scala
def encodeState(state: LocalState): Either[Vector[ModelError], String]
def decodeState(text: String): Either[Vector[ModelError], LocalState]
def encodeRequest(request: ProposalRequest): Either[Vector[ModelError], String]
def decodeRequest(text: String): Either[Vector[ModelError], ProposalRequest]
def encodeObservation(observation: ProposalObservation): Either[Vector[ModelError], String]
def decodeObservation(text: String): Either[Vector[ModelError], ProposalObservation]
def decodeFixture(text: String): Either[Vector[ModelError], LocalFlowFixture]
def encodeReport(report: LocalFlowReport): Either[Vector[ModelError], String]
```

Private codecs for closed DTOs enforce declared keys and types. The nested bundle reuses `PreparationJson.encode/decode`; the canonical public v1 codec API and format remain unchanged. Both canonical and local private Long decoders accept only JSON numbers with exact values within the Long range. Strings, fractions and overflow are rejected; mathematically integral numeric spellings such as `1.0` and `1e0` are accepted. Existing semantic validation retains positivity checks. Expected malformed JSON, wrong types, missing/extra keys, unsupported versions/enums, out-of-range Longs and invalid new semantics return structured ModelError vectors. State validation does not recurse into encode/decode. There is no automatic migration or compatibility fallback.

The draft-2020-12 schema is oneOf the five formats. It uses closed DTO objects, explicit nullable fields, existing external preparation schema refs and structural confirmed/nonconfirmed and failed/material-null conditions. Scala additionally enforces cross-record lineage, exact reference resolution, immutable prefixes, correlation and lifecycle transitions. The unchanged external preparation schema's entity revision definitions are further bounded by Scala Long decoding.

## Runnable local fixture

`LocalFlowMain.execute(fixturePath: Path, storeDirectory: Path): Either[LocalError, LocalFlowReport]` reads a strict UTF-8 fixture. It requires virtual empty store revision zero, seeds the source-only bundle, prepares once, proposes once and executes fixture deliveries in order using each returned revision. It rereads persisted state and renders Markdown through unchanged `PreparedMaterialMarkdown`.

It writes only fixed projection filenames in the selected directory: `preparation-bundle.json`, `prepared-material.md`, `proposal-request.json`. The request export is the first fixture request with the exact frozen proposal. These files are derived projections with no atomic primary-store guarantee. Export failure returns projection-write / Applied with known primary revision and leaves committed state intact. Re-execution against initialized primary state returns already-initialized and never reinitializes or overwrites the flow.

`main(args: Array[String]): Unit` accepts exactly two arguments: readable UTF-8 fixture path and explicit output/store directory. Success prints one compact JSON report prefixed `TKL_LOCAL_FLOW_RESULT=`. Failure throws a descriptive IllegalStateException so the CLI/SBT command exits nonzero.

Run from this repository with a fresh, absent output directory:

```text
sbt --batch "runMain org.simplemodeling.textus.knowledgelake.local.LocalFlowMain fixtures/tkl/local-flow-v1.json target/tkl-phase1-step2-flow"
```

The fixture preserves the accepted source Resource/Evidence/Context/baseline/Drive-binding values, literal Japanese/emoji text and reference-only unknown content. It supplies `run-input-local-001`, `run-local-001`, completed known manual-operator provenance with no failures and `material:準備@1` with two grounded sections. Producer/key is `textus-knowledge-lake` / `proposal-local-001`. Submit `fixture-submit-001` yields simulated Unknown with null refs. Query `fixture-query-001` yields simulated Confirmed for the same key, `fixture-receipt-001` and `fixture-candidate-001@1`.

Successful primary revisions are seed 1, preparation 2, proposal 3, Submit reservation 4, Unknown recording 5, Query reservation 6 and Confirmed recording 7. There is one immutable Run/material/proposal and two attempts. No TKW store exists.

The authored behavioral specifications use AnyWordSpec, GivenWhenThen and should matchers:

```text
sbt --batch "testOnly org.simplemodeling.textus.knowledgelake.local.LocalPreparationStoreSpec org.simplemodeling.textus.knowledgelake.local.PreparationFlowSpec org.simplemodeling.textus.knowledgelake.local.ProposalDeliverySpec"
```

`LocalPreparationStoreSpec` covers virtual empty/reopen, stale CAS, held stable locks, pre/postpublication faults including unsupported atomic move classification, immutable refs/RunInput/snapshots, revision append/exhaustion, corrupt/symlink paths and pending transitions. `PreparationFlowSpec` covers exact selections, frozen history/offline rendering, failed/partial results, preparation conflict/fault outcomes, producer keys, strict JSON, actual repository fixture execution, initialized-store protection and Applied projection failure. `ProposalDeliverySpec` covers pending-before-port, explicit Unknown/exception recovery, correspondence/malformed-result handling, reservation rejection, unrecorded completion/replay reads, terminal results, independent producer keys and missing fixture lookup. Test I/O uses disposable directories and local observer/port fakes, closes channels/locks and cleans temporary trees in finally. It uses no network, processes or timing races.

These are authored executable specifications; this document does not claim validation has run. CNCF SBT validation is parent-owned and serialized through the registered command runner under the machine execution policy. The focused accumulator runs first, then the actual representative command against a fresh output directory.

## Pending integration

Supported native CML/Cozy/CNCF integration remains pending the next required Step. External Drive concurrency/retention/permissions, TKW durable receipt/deduplication/Candidate formation, TEAI transport/authentication, KnowledgeHub synchronization and BoK lifecycle remain owner proposals and unverified. Local source/build version remains `0.1.0-SNAPSHOT`. This slice establishes local preparation/result handling and versioned proposed envelopes; it does not close the Phase or assert external acceptance.
