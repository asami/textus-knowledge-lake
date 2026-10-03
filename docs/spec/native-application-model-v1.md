# Native application model v1

- Current status: Supported native route generated, compiled and fidelity-checked; released preparation, eight native tests and representative CLI passed. Complete Step validation/review/acceptance commit and final Phase review/full validation/release pending.
- Owner: Textus Knowledge Lake
- Update rule: Preserve normative model/runtime contracts and tool pins; update acceptance only from actual evidence and accepted commit/tree. External guarantees remain owner proposals.

[The native CML source](../../src/main/cozy/textus-knowledge-lake.cml) declares `COMPONENT KnowledgeLake` in package `org.simplemodeling.textus.knowledgelake`. Its selected existing constructs are COMPONENT, ACTOR, VALUE, SERVICE/OPERATION, ENTITY-associated StateMachine and nested USE CASE/MAIN FLOW. Slice TKL-P1-03A has generation, compilation and generated-model fidelity evidence for the corrected native surface. Slice B supplies the generated application adapter and normal runtime entry, with actual normal native dispatch and persisted success/failure verified by all eight native tests and the representative CLI. Complete Step 3 validation/independent review/acceptance commit and final Phase review/full validation/release remain pending. See the [public proposal/evidence journal](../journal/2026/10/2026-10-02-phase-1-integration-proposals-v1.md).

## Actors

| Actor | Kind | Modeled role |
| --- | --- | --- |
| KnowledgeWorker | human | Requests preparation and examines evidence/proposals |
| TextusKnowledgeWorkbench | external | Future receiving application, represented here by local result fixtures |
| ResourceProvider | external | Future provider, represented here by supplied local resources/evidence |
| KnowledgeProcessor | external | Future processing facility, represented here by deterministic/manual results |

External actors have no implemented external operations. TKW owns Candidate identity, formation, editing, review/approval and Admission. Fixture receipts/Candidate refs are opaque data and prove only TKL-side handling.

## Use Case inventory

Every Use Case has primary actor `KnowledgeWorker`. UC-KA-01 models the current project-local fixture/manual-result scenario. The other 30 declarations retain these exact IDs and summaries/goals, have status `planned`, and explicitly describe later scope. UC-KA-05 has accepted TKL-side local delivery handling; its real TKW integration remains unverified and planned.

| ID | Summary / goal | Scope |
| --- | --- | --- |
| UC-KA-01 | Propose Raw Knowledge Candidate | Current local scenario |
| UC-KA-02 | Explore Candidates by Theme | Planned |
| UC-KA-03 | Discover Candidates from New Evidence | Planned |
| UC-KA-04 | Inspect Candidate Evidence | Planned |
| UC-KA-05 | Deliver Candidate to Knowledge Workbench | Planned receiving integration |
| UC-KA-06 | Explore Candidates Against Existing Knowledge | Planned |
| UC-RE-01 | Research Theme Across Sources | Planned |
| UC-RE-02 | Trace Historical Context | Planned |
| UC-RE-03 | Compare Views | Planned |
| UC-RE-04 | Analyze Consensus | Planned |
| UC-RE-05 | Extract Open Questions | Planned |
| UC-RE-06 | Answer from Evidence | Planned |
| UC-PA-01 | Review Recent Changes | Planned |
| UC-PA-02 | Review New Evidence | Planned |
| UC-PA-03 | Identify Active Discussions | Planned |
| UC-PA-04 | Review Open Issues | Planned |
| UC-PA-05 | Generate Project Digest | Planned |
| UC-PA-06 | Notify Significant Change | Planned |
| UC-CP-01 | Generate Report | Planned |
| UC-CP-02 | Generate Slide Deck | Planned |
| UC-CP-03 | Generate Infographic | Planned |
| UC-CP-04 | Generate Mind Map | Planned |
| UC-CP-05 | Generate Audio / Video | Planned |
| UC-CP-06 | Generate LLM Context | Planned |
| UC-KF-01 | Synchronize Knowledge Context | Planned |
| UC-KF-02 | Project Knowledge to Preparation Workspace | Planned |
| UC-KF-03 | Refresh Knowledge Context after Formation | Planned |
| UC-KF-04 | Trace Knowledge Formation | Planned |
| UC-KF-05 | Append Knowledge Projection Delta | Planned |
| UC-KF-06 | Rebuild Knowledge Context | Planned |
| UC-KF-07 | Publish Current Knowledge Context | Planned |

## UC-KA-01 local scenario

The primary Use Case declares separate scalar `SUMMARY`, `PRECONDITION` and `POSTCONDITION` sections so that its scenario and conditions are explicitly projected. Their meaning and the ten-step main flow remain unchanged. Actual generation and fidelity inspection of the corrected source preserve these projections; runtime preparation must retain that fidelity.

KnowledgeWorker triggers execution with an explicit UTF-8 fixture path and local store directory. Supporting actors are TextusKnowledgeWorkbench, ResourceProvider and KnowledgeProcessor, represented by fixtures/manual results. Preconditions are a valid [local-flow v1 fixture](../../fixtures/tkl/local-flow-v1.json), an empty local store, and explicitly supplied IDs, versions, processing result and boundary observations. No live Google, TKW, TEAI, KnowledgeHub or BoK invocation occurs.

The nested `MAIN FLOW local_preparation` describes the accepted local behavior:

1. Validate the fixture and empty local store.
2. Seed versioned Resources, Evidence and Context, including binding metadata and existing-information baseline.
3. Fix Run inputs and existing-information baseline without rewriting historical snapshots.
4. Import the deterministic/manually supplied preparation result.
5. Persist the Run and PreparedMaterial version together.
6. Compose an immutable proposal with producer-scoped key and material/evidence lineage.
7. Reserve delivery durably before fixture invocation.
8. Record Unknown, then query/retry using the same producer/proposal key and exact correlation rules.
9. Export canonical bundle JSON, derived Markdown and proposal request JSON.
10. Return the report from actual persisted state, with the native adapter carrying the exact `LocalFlowJson.encodeReport` output in `FixtureExecutionResult.reportJson`; normal native dispatch has passed focused test/CLI validation; complete Step acceptance remains pending.

Postconditions are persisted material/proposal and correlated delivery observations with immutable Run/input/evidence lineage, exports and an actual persisted report. The repository fixture reaches revision 7 through seed, preparation, proposal, Submit reservation, Unknown recording, Query reservation and Confirmed recording. Fixture confirmation does not establish receiving-service durability, duplicate prevention, Candidate lifecycle, approval or Admission.

## Command surface

`VALUE FixtureExecutionCommand` has required string fields `fixturePath` and `storeDirectory`. `VALUE FixtureExecutionResult` has the required string field `reportJson` (multiplicity `1`). `SERVICE LocalPreparationService` declares `OPERATION executeFixture`, `TYPE COMMAND`, `INPUT TYPE FixtureExecutionCommand` and `OUTPUT TYPE FixtureExecutionResult`.

The observed pinned Cozy 0.3.2.2 parser rejects raw scalar `OUTPUT TYPE string`; it accepts declared named values or CNCF predefined results. The CNCF 0.5.2 predefined catalog has no `StringResult` or `JSONResult`. Build 4 generated and compiled the named `FixtureExecutionResult` value, but its generated class extends `RecordPresentable`; the released value projector does not realize the source `EXTENDS OperationResult` assumption. The corrected declaration therefore retains the named payload and required report field without claiming physical OperationResult inheritance. Subsequent actual generation, compilation and fidelity inspection passed for that corrected source. Real normal native execution subsequently passed all eight cases and the representative CLI; complete Step/Phase acceptance remains pending.

The actual generated ActionCall returns `ExecUowM[org.goldenport.protocol.operation.OperationResponse]`, and its compiled goldenport-core 0.4.2 dependency provides `OperationResponse.apply(Record)`. The existing [NativeFlowComponent](../../src/main/scala/org/simplemodeling/textus/knowledgelake/nativeapp/ComponentFactory.scala) extends the generated `KnowledgeLakeComponent.Factory`. Its inherited factory hook supplies the application ActionCall, which invokes the accepted `LocalFlowMain.execute` path and returns `OperationResponse(FixtureExecutionResult(exactReportJson).toRecord())`, where `exactReportJson` is the exact successful `LocalFlowJson.encodeReport` output, without parsing, reserializing or normalizing the string. This carries the generated named output payload through the standard RecordResponse boundary. Expected errors retain structured `LocalError` inside `NativeFlowFailure.localError` through `Consequence.failure(Throwable)`, including exact code, message, write outcome and observed revision. Report encoding failure after persistence retains Applied and the actual report revision. The adapter restricts the generated application catalog to `LocalPreparationService`; generated aggregate, view and entity CRUD are excluded. Slice A declares the surface. Compilation and adapter implementation alone cannot establish normal native execution.

## CAR source layout and documentation

The conventional `ComponentFactory.scala` file contains the original `NativeFlowComponent` and `NativeFlowFailure` source unchanged, preserving package, object/class and JVM identities and existing consumers. `project.kind: car` supplies the local CAR classification. Component, service and operation SUMMARY/DESCRIPTION sections supply source help prose; the [reference manual](../../src/main/car/manual/index.md) and [user guide](../../src/main/car/manual/user-guide.md) describe the explicit fixture/store contract. Fresh generation, ordinary CAR lint and independent complete Step review remain pending after this conformance repair; the prior strict checker failed and remains failed. Step/Phase acceptance, published CAR compatibility and live Help/Web/MCP routes are not established by these edits.

Direct host filesystem use is deliberate within this accepted local fixture adapter: `LocalFlowMain` reads the supplied UTF-8 fixture and writes exports, and `LocalPreparationStore` owns local atomic/CAS persistence. `NativeFlowMain` creates and removes only its owned ephemeral bootstrap directory and reads prepared development metadata from the invoking root. These are local provider/bootstrap boundaries, not approval for production provider access or a replacement canonical store. A production provider must separately establish its filesystem/datastore/provider contract. The required `fixturePath`, `storeDirectory` and `reportJson` strings remain the single internal representations; no DTO/ABI or value-type change is implied by lint advice.

## Local StateMachine

`ENTITY LocalPreparationSession` hosts `StateMachine PreparationProposalWorkflow`, with required `id: entityid` and `status: int` attributes. The supported nested State/Transition headings and `- TO ::` / `- ON ::` properties declare the same eight states and ten transitions, with all eight events explicitly declared. The Entity is a native projection host for TKL-local execution; it introduces no canonical DTO, local store or host CRUD requirement.

| From | Event | To |
| --- | --- | --- |
| Uninitialized | seed | Seeded |
| Seeded | prepare | Prepared |
| Prepared | propose | Proposed |
| Proposed | submitUnknown | Unknown |
| Unknown | queryConfirmed | Confirmed |
| Proposed | submitConfirmed | Confirmed |
| Proposed | rejected | Rejected |
| Unknown | rejected | Rejected |
| Proposed | conflict | Conflict |
| Unknown | conflict | Conflict |

This is a modeled application workflow; it does not replace local atomic/CAS and delivery behavior or assert automated/Composite execution. Dedicated `WORKFLOW` support is unverified in the pinned release. Absence of a class alone is not proof that it is unsupported.

## Canonical model ownership

Native declarations consume the six canonical models without redeclaring their DTOs or changing persistence/proposal semantics.

| Canonical model | Authoritative TKL responsibility | Native relationship |
| --- | --- | --- |
| Resource | TKL identity and provider location | Seeded resource input |
| Evidence | Exact resource version, retained/reference-only snapshot and provenance | Selected evidence input |
| KnowledgeProcessingContext | Selection, bindings and existing-information baseline | Context fixed into Run input |
| ProcessingRun | Immutable inputs, processor and observable results/failures | Preparation execution record |
| PreparedMaterial | Material version, producing Run and grounded sections | Persisted output |
| RawCandidateProposal | Producer-scoped key and material/Run/evidence lineage | Export and same-key local delivery handling |

[Preparation contract v1](preparation-contract-v1.md) and [local preparation flow v1](local-preparation-flow-v1.md) remain authoritative for Scala/JSON, atomic/CAS and uncertain-delivery correlation. The native surface introduces no new domain lifecycle or store.

## Released toolchain and generation

| Input | Pin |
| --- | --- |
| Scala | 3.3.8 |
| SBT (`project/build.properties`) | 1.12.6 |
| sbt-cozy | org.goldenport:sbt-cozy:0.1.16 |
| Cozy runtime / delegate Coursier version | 0.3.2.2 |
| CNCF compile dependency | org.goldenport:goldenport-cncf_3:0.5.2 |

[project.yaml](../../project.yaml) declares native component/package metadata and the compile dependency. [build.sbt](../../build.sbt) enables CozyPlugin on the explicit root, retains Circe/ScalaTest and Maven Central and adds the official SimpleModeling Maven repository. The public [launcher](../../conf/cozy/launcher.yaml) disables development launcher/runtime routes and pins the release. [Generation configuration](../../conf/cozy/config.yaml) aligns CNCF at 0.5.2. The existing SBT build continues to consume Maven dependencies; the top-level CAR packaging metadata classifies the project-owned local development artifact and limits runtime compatibility to CNCF 0.5.2. This metadata does not publish a CAR or establish behavioral acceptance.

The initial reproducible generation/compilation command from this repository is:

```text
sbt --batch cozyGenerate compile
```

CNCF execution policy routes it through the registered serialized SBT runner. Installed tools and released artifacts suffice; no sibling source checkout, external source build, `publishLocal`, plugin installation, live service or credentials are required. `cozySkipUnchangedGeneration := false` requests fresh generation. Managed sources are compiler inputs and must not be edited or staged. Standard tool-produced source/runtime integrity provenance is artifact data, never Phase authority/binding/review identity. No custom digest or substitute generator is supplied.

The existing actual parser/generator, compiler, retained CML sidecar and generated-source inspection evidence establishes generation/compilation/fidelity only. Output preserves all 31 UC IDs, the typed main flow and primary summary/pre/postconditions, all eight states and ten StateMachine transitions with their event mapping, the service input DTO and the required result reportJson field. Runtime preparation may regenerate managed artifacts, so the complete generated source set and metadata must be inspected again for that same fidelity. The standard OperationResponse payload and real dispatch success/failure passed the eight native scenarios and representative CLI after released runtime preparation; generated fidelity remained unchanged. Complete Step and Phase acceptance remain pending. Silently ignored sections fail native acceptance and return to parent PLAN. Actually demonstrated shared missing capabilities belong in [TKL-INT-06](../notes/phase-1-integration-register.md); no local substitute generator/framework is admitted.

## Local development startup

Run preparation, native tests and the CLI with the process working directory set to the prepared `textus-knowledge-lake` project root. [NativeFlowMain](../../src/main/scala/org/simplemodeling/textus/knowledgelake/nativeapp/NativeFlowMain.scala) takes `Path.of(".").toAbsolutePath.normalize()` as that development root. It does not search for a project or packaged fallback. The normal subsystem descriptor binds the canonical component coordinate `org.simplemodeling.textus:KnowledgeLake:0.1.0-SNAPSHOT`. [The source ABI](../../src/main/car/abi-manifest.json) declares the complete generated model surface: the component, `LocalPreparationService.executeFixture`, both required-field value types and `LocalPreparationSession` attributes, with no component ABI dependencies. The CAR development classification, source component descriptor and source ABI are project-owned startup inputs, while the accepted canonical DTOs and local store remain authoritative.

The current CML has no `STYLE` declaration. Its generated `target/cozy/model-metadata.json` exists but omits the top-level `componentStyle`. For this route, released Cozy 0.3.2.2 requires [the source component descriptor](../../src/main/car/component-descriptor.json): canonical JSON with integer `schemaVersion` 3 and exactly the component namespace `org.simplemodeling.textus`, ID `KnowledgeLake` and version `0.1.0-SNAPSHOT`. This supplies the supported startup metadata without selecting a new ComponentStyle or changing domain or native operation contracts.

`src/main/car/component-descriptor.json` and `src/main/car/abi-manifest.json` are repository-owned source metadata. The existing released sbt-cozy `cozyPrepareRuntime` task prepares the development artifact: it validates the source descriptor's canonical schema and exact project coordinate, copies it into `target/cncf.d/component-descriptor.json`, writes `target/cncf.d/runtime-classpath.txt` and generates `target/cncf.d/car-runtime-manifest.json`. The released framework checks corruption and stale inputs for exactly `target/cncf.d/runtime-classpath.txt`, `target/cncf.d/component-descriptor.json` and `src/main/car/abi-manifest.json`, with their integrity evidence in that tool-produced runtime manifest. Target/runtime evidence must be generated/prepared through the released tooling; application code supplies no runtime manifest or digest. All tools and dependencies are released. No sibling source build, shared warehouse, CAR publication, external service or credentials are required.

Startup retains explicit normal subsystem selection through an owned temporary `subsystem.yaml`. Authentication conventions and fallback privilege are disabled; the fixed local subject `tkl-local-user` has user security level, empty roles and empty capabilities. The standalone/standard bindings remain scoped to `knowledge-lake-local/default`. `textus.repository.component.dev.dir` points only to the invoking TKL root. `textus.component.dev.dir` stays empty so that development descriptor precedence cannot replace the explicit normal security descriptor. Other repository/discovery/import settings remain disabled or empty, with collaborator discovery limited to the owned empty runtime directory.

Normal startup validates the genuine canonical development artifact before activating the application implementation. The released selection gives a discovered `component-dev-dir` artifact priority 500 and an Embed component priority 0, so the factory callback alone cannot establish the required parent-loaded implementation. The callback captures the generated application Factory component with its unchanged Embed provenance. After `runtime.initializeHandle` succeeds, public `Subsystem.upsertC(Vector(component))` installs that captured component and performs framework bootstrap, context/service binding and real resolver reconstruction. This preserves the exact parent-loaded `LocalError`/`NativeFlowFailure` type and the `LocalPreparationService`-only catalog. A missing callback is a configuration failure. Activation must complete before the unchanged Fixed-only and one-generated-component guards and actual `executeOperationResponse(request)` dispatch. No private framework API or synthetic dispatch path is used.

The [public instance diagnostic history](../journal/2026/10/2026-10-02-phase-1-integration-proposals-v1.md#actual-startup-diagnostic-history) observed 13 framework builtins plus `org.simplemodeling.textus.KnowledgeLake` after startup and public upsert. The captured and normally admitted KnowledgeLake components both had the exact instance key `org.simplemodeling.textus.KnowledgeLake@default`. Public upsert replaced the local-first-loaded implementation with the parent AppClassLoader implementation, and the profile remained Fixed. This disproved the duplicate-identity hypothesis: the remaining builtins caused the sole-component guard failure before operation dispatch.

The [public released CNCF 0.5.2 filter history](../journal/2026/10/2026-10-02-phase-1-integration-proposals-v1.md#actual-startup-diagnostic-history) shows that `GenericSubsystemFactory` creates its descriptor-factory builtin set and excludes components only by complete `Component.name` membership after trimming and lowercasing both the names and exclusion values. The previous short aliases did not match those names; `--no-default-components` also did not remove this explicit descriptor-factory set. The normal descriptor in [NativeFlowMain](../../src/main/scala/org/simplemodeling/textus/knowledgelake/nativeapp/NativeFlowMain.scala) now excludes exactly these 13 observed formal names, in this order:

```text
org.goldenport.cncf.Admin
org.goldenport.cncf.Auth
org.goldenport.cncf.Client
org.goldenport.cncf.Blob
org.goldenport.cncf.Tag
org.goldenport.cncf.Tool
org.goldenport.cncf.Debug
org.goldenport.cncf.Event
org.goldenport.cncf.JobControl
org.goldenport.cncf.Workflow
org.goldenport.cncf.Metrics
org.goldenport.cncf.MessageDeliveryStub
org.goldenport.cncf.Specification
```

The exclusion correction preserves every other normal descriptor value, the fixed local user and security restrictions, and the exact sole-generated-KnowledgeLake guard. It adds no wildcard exclusion, permission or capability. After the exact-name correction, fresh released preparation, all eight unchanged native scenarios and the representative CLI passed. These focused local results do not complete independent Step/Phase acceptance or prove external integration.

The public owning handle closes the subsystem, SystemNode and embedding lifecycle. Handle cleanup, `runtime.closeEmbedding()` and removal of only the owned ephemeral runtime directory apply on startup, activation and dispatch failure as well as success. The existing result/cleanup failure precedence remains intact. Fixture, store/output, project and prepared runtime evidence paths are never cleanup targets.

The reproducible focused preparation and native scenario command is:

```text
sbt --batch cozyPrepareRuntime 'testOnly org.simplemodeling.textus.knowledgelake.nativeapp.NativeFlowSpec'
```

The portable representative CLI example runs from the repository root with a fresh, absent store/output directory:

```text
sbt --batch cozyPrepareRuntime
sbt --batch 'runMain org.simplemodeling.textus.knowledgelake.nativeapp.NativeFlowMain fixtures/tkl/local-flow-v1.json target/tkl-phase1-native-example'
```

Choose another explicit fresh absent store if that example path has been consumed. Do not automatically delete or reset an initialized store. This is an illustrative reproduction command, not the absolute argv of the retained CLI6 evidence. CNCF execution policy routes these commands through the registered serialized SBT runner.

The [public diagnostic/evidence journal](../journal/2026/10/2026-10-02-phase-1-integration-proposals-v1.md#actual-startup-diagnostic-history) retains the actual startup history: attempt 4 failed during preparation before tests because the style-less route lacked its source descriptor; attempt 5 then prepared successfully but all eight cases stopped at the sole-component guard and CLI5 was not reached. Public upsert already had the matching instance key. Exact fully qualified exclusion of the 13 observed builtins resolved the remaining guard failure. Native test attempt 6 completed released preparation, preserved canonical descriptor/integrity inputs and generated fidelity, and passed 8/8 cases (zero failed/canceled/ignored/pending). The actual representative CLI6 then succeeded with store revision 7, one immutable Run/material/proposal, and unknown/confirmed delivery observations. Its assurance is exactly `fixture-only; external guarantees unverified`. Full report/export behavior is exercised by the native tests; the public journal provides exact decoded request/observation samples and a labeled semantic report excerpt.

The eight existing `NativeFlowSpec` scenarios and all their Given/When/Then clauses and assertions remain unchanged:

| Scenario | Required observable result |
| --- | --- |
| Persist and report the repository fixture | Revision 7, exact lineage, report and export bytes |
| Reject an initialized local store | Structured NotApplied refusal with revision 7 and unchanged bytes |
| Retain failure after projection writing fails | Applied failure with readable committed revision 7 |
| Reject malformed fixture JSON | Structured invalid error and no state or output writes |
| Report a missing fixture | Structured fixture-read error and no state or output writes |
| Reject each missing required property | argumentMissing/400 with the exact omitted field name |
| Reject an invalid NUL path | Structured invalid-path error and no state or output writes |
| Reject generated CRUD and future operations | operationNotFound/404 for all nine excluded targets |

Native dispatch against the real generated Component and persisted success/failure behavior have passed the focused eight native tests and representative CLI. The [owner proposal package](../journal/2026/10/2026-10-02-phase-1-integration-proposals-v1.md) and [reconciled register](../notes/phase-1-integration-register.md) are locally documented. Complete Step suite/independent review/acceptance commit and final Phase full review/full validation/release remain pending. The package must be bound to the actual accepted native commit/tree before closure; focused local success is not production integration acceptance.
