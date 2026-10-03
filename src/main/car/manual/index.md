# KnowledgeLake reference manual

- Current status: Local core and selected native route have retained focused evidence. CAR conformance repair awaits fresh validation and independent complete Step acceptance; final Phase closure remains pending. The prior strict checker failed.
- Owner: Textus Knowledge Lake
- Update rule: Keep canonical contracts and released pins authoritative; record acceptance only from actual validation and accepted source. Distinguish local fixtures from external guarantees.

KnowledgeLake prepares evidence-grounded material and raw candidate proposals with immutable input lineage. This CAR development route executes repository-local UTF-8 fixtures and supplied deterministic/manual results. TKW owns Candidate formation, editing, review/approval and Admission; KnowledgeHub owns canonical Information and BoK owns publication. See the [project overview](../../../../README.md) and [user guide](user-guide.md).

## Released setup and configuration

The prepared development project uses Scala 3.3.8, SBT 1.12.6, sbt-cozy 0.1.16, Cozy 0.3.2.2 and `org.goldenport:goldenport-cncf_3:0.5.2`. [build.sbt](../../../../build.sbt), [project metadata](../../../../project.yaml), [plugin pins](../../../../project/plugins.sbt), [launcher settings](../../../../conf/cozy/launcher.yaml) and [generation settings](../../../../conf/cozy/config.yaml) define setup. The launcher disables development launcher/runtime routes and selects the released Cozy version. Run preparation and native entry from the repository root; installed tools and released artifacts suffice, without a sibling source checkout, credentials or live service. CNCF commands use the registered serialized SBT runner.

The [native specification](../../../../docs/spec/native-application-model-v1.md#local-development-startup) explains `cozyPrepareRuntime`, canonical source descriptor validation, generated runtime metadata, normal startup/security restrictions and public handle cleanup. Generated sources and integrity evidence are tooling outputs and must not be hand-edited. `project.kind: car` classifies the local project; it does not publish an artifact.

## Component and command contract

The [CML source](../../../../src/main/cozy/textus-knowledge-lake.cml) declares `KnowledgeLake` in `org.simplemodeling.textus.knowledgelake`, with help SUMMARY/DESCRIPTION prose for the component, service and operation. The conventional [ComponentFactory.scala](../../../../src/main/scala/org/simplemodeling/textus/knowledgelake/nativeapp/ComponentFactory.scala) preserves the `NativeFlowComponent.Factory` identity and extends the real generated factory. Its application catalog includes `LocalPreparationService` only; modeled entity/state-machine projections add no canonical store or exposed CRUD operation.

| Declaration | Contract |
| --- | --- |
| LocalPreparationService.executeFixture | COMMAND executing the accepted local preparation/proposal flow |
| FixtureExecutionCommand.fixturePath | Required string naming an explicit UTF-8 local-flow v1 fixture |
| FixtureExecutionCommand.storeDirectory | Required string naming a fresh explicit local store/output directory |
| FixtureExecutionResult.reportJson | Required string carrying exact successful `LocalFlowJson.encodeReport` output |

These strings are the existing single internal representations. The named result is `RecordPresentable`, delivered as standard `OperationResponse(Record)`; it does not claim OperationResult inheritance. The adapter delegates [LocalFlowMain.execute](../../../../src/main/scala/org/simplemodeling/textus/knowledgelake/local/LocalFlowMain.scala) without parsing, reserializing or normalizing the encoded report. The [source ABI](../../../../src/main/car/abi-manifest.json) and [source descriptor](../../../../src/main/car/component-descriptor.json) declare the current generated surface and identity.

## Persisted outputs and failures

The [repository fixture](../../../../fixtures/tkl/local-flow-v1.json) seeds versioned inputs, fixes a Run, imports preparation, persists material/proposal and reserves delivery before fixture invocation. Its successful report describes revision 7, exact Run/material/proposal keys, `unknown` then `confirmed` observations and export paths. `state.json` retains canonical local state; `preparation-bundle.json`, `prepared-material.md` and `proposal-request.json` are exports. JSON preserves canonical information and Markdown is derived. The report assurance remains exactly `fixture-only; external guarantees unverified`.

Expected failures preserve `LocalError` in `NativeFlowFailure.localError`, including code, message, `writeOutcome` and `observedRevision`. Missing/unreadable or malformed fixtures, invalid paths and initialized stores are refused. Missing required properties retain framework `argumentMissing/400`; excluded CRUD/future operations retain `operationNotFound/404`. NotApplied, Applied and Unknown describe write visibility. A projection or report-encoding failure can occur after persistence and retain Applied with the actual revision; never interpret it as rollback. Atomic/CAS and uncertainty semantics are defined by the [local flow specification](../../../../docs/spec/local-preparation-flow-v1.md) and [preparation contract](../../../../docs/spec/preparation-contract-v1.md).

The public owning handle closes subsystem/SystemNode/embedding lifecycle. Cleanup on success or startup/activation/dispatch failure removes only the owned ephemeral runtime directory. Fixture, initialized store, exports, project and prepared runtime evidence are never cleanup targets. Preserve them when investigating failures.

## Local provider and external limits

Direct host filesystem access is intentional for the accepted local fixture provider and atomic/CAS store: LocalFlowMain reads the supplied fixture and writes exports; LocalPreparationStore owns local persistence. NativeFlowMain uses the invoking project root and owned temporary bootstrap files. This bounded fixture/bootstrap implementation does not establish production filesystem/datastore/provider admission. Production Google Workspace access, provider retention/conditional updates and processing facilities require separate owner verification.

Fixture receipt/Candidate references prove local correlation handling only. They do not prove authenticated downstream identity, durable receipts, duplicate prevention, Candidate lifecycle, approval or Admission. Dedicated WORKFLOW/Composite and capability projections remain unverified in the pinned release.

The prior strict CAR checker remains failed; fresh ordinary local CAR lint and independent Step/Phase acceptance are pending. Source help prose and manuals do not establish live Help/Web/MCP behavior. No published CAR, warehouse or prior-release ABI baseline acceptance is claimed. The [TKL-INT-06 tooling record](../../../../docs/notes/phase-1-integration-register.md#tkl-int-06) retains the ABI identity display, Scala dependency probe and unverified latest-plugin lookup observations under the exact pins; these observations do not mandate upstream implementation before local closure. The [owner proposal package](../../../../docs/journal/2026/10/2026-10-02-phase-1-integration-proposals-v1.md) retains pending native release binding.
