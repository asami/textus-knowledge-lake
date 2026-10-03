# KnowledgeLake local user guide

- Current status: Retained focused native tests and CLI demonstrate the local route. CAR conformance edits await fresh validation and independent complete Step acceptance; final Phase acceptance remains pending. The prior strict checker failed.
- Owner: Textus Knowledge Lake
- Update rule: Keep examples aligned with canonical specs, fixture and released pins. Update acceptance only from actual evidence; preserve initialized stores and label external guarantees unverified.

Use this guide to run the repository fixture through the generated native component and inspect persisted preparation/proposal outputs. The [reference manual](index.md) describes the command, errors and boundaries; the [README](../../../../README.md) describes ownership and Phase 1 scope.

## Prerequisites and preparation

Use the repository's SBT 1.12.6 and installed native tooling with the released pins: Scala 3.3.8, sbt-cozy 0.1.16, Cozy 0.3.2.2 and CNCF 0.5.2. Keep the committed [build](../../../../build.sbt), [project metadata](../../../../project.yaml), [launcher configuration](../../../../conf/cozy/launcher.yaml) and [generation configuration](../../../../conf/cozy/config.yaml). This local route needs no Google credentials, live receiver or sibling source build.

Set the process working directory to the `textus-knowledge-lake` repository root. The native entry uses that exact development root and its prepared source/runtime metadata. Supply the [UTF-8 local-flow v1 fixture](../../../../fixtures/tkl/local-flow-v1.json) and select a fresh, absent store/output directory. Do not create a substitute runtime descriptor or edit generated sources. Preparation through the pinned tooling generates and validates runtime metadata.

From that root, prepare the development runtime:

```text
sbt --batch cozyPrepareRuntime
```

CNCF execution policy routes SBT through the registered serialized runner. These are portable reproduction commands, not a claim that this guide has run a new validation. See the [native startup contract](../../../../docs/spec/native-application-model-v1.md#local-development-startup) for the complete setup and restrictions.

## First success with a fresh store

If `target/tkl-phase1-native-example` is absent, run:

```text
sbt --batch 'runMain org.simplemodeling.textus.knowledgelake.nativeapp.NativeFlowMain fixtures/tkl/local-flow-v1.json target/tkl-phase1-native-example'
```

If that directory has been consumed, choose another explicit fresh, absent directory in the command. Never delete or reset an initialized store to force success. The operation deliberately refuses existing initialized state and preserves its history.

The generated command receives required strings `fixturePath` and `storeDirectory`. It reads the fixture as UTF-8, persists the local atomic/CAS flow and writes exports. Native success returns exact report JSON in `FixtureExecutionResult.reportJson`; the CLI prints it with the `TKL_NATIVE_FLOW_RESULT=` marker. Earlier retained fixture execution reached revision 7 with `unknown` then `confirmed` observations; the current conformance edits still require fresh parent-owned validation.

## Interpret the output

The report identifies persisted `storeRevision`, `runId`, `materialRef`, producer/proposal key, delivery outcomes and paths to exports. Inspect the selected directory:

| File | Meaning |
| --- | --- |
| state.json | Persisted canonical local state and delivery history |
| preparation-bundle.json | Lossless JSON export of persisted canonical values |
| prepared-material.md | Derived human/LLM material projection |
| proposal-request.json | Versioned proposal request export |

The fixture contains retained Japanese text and reference-only/unknown inputs. Preserve their original revisions and lineage when reading the [preparation contract](../../../../docs/spec/preparation-contract-v1.md) and [local flow specification](../../../../docs/spec/local-preparation-flow-v1.md). `confirmed` is a supplied fixture observation: the assurance remains `fixture-only; external guarantees unverified`. It proves no real TKW Candidate, receipt durability, approval, Admission or publication.

## Troubleshooting

| Observation | Action |
| --- | --- |
| Preparation/startup metadata failure | Confirm the repository-root working directory and pinned source/configuration, then follow the native spec's released preparation instructions. Preserve the diagnostic; do not fabricate metadata or change runtime guards. |
| fixture-read, invalid fixture or invalid-path | Check the explicit path, readable UTF-8 content and local-flow v1 shape against the repository fixture/spec. Keep failed-store evidence. |
| already-initialized | Preserve that store. Select a different fresh absent directory for a new fixture execution. |
| argumentMissing/400 | Supply both required fixturePath and storeDirectory fields for a native request. The CLI requires exactly two path arguments. |
| busy or revision-conflict | Preserve state and inspect the competing local access and observed revision under the local flow contract. Do not reset the store. |
| projection-write or report-encode after persistence | Inspect writeOutcome and observedRevision. Applied means canonical state may already be committed; retain state/exports and investigate permissions or encoding diagnostics before separately scoped recovery. |
| operationNotFound/404 for CRUD/future operations | Use only LocalPreparationService.executeFixture; other modeled operations are outside this local command surface. |

Normal handle cleanup closes the runtime and removes only its owned ephemeral bootstrap directory. It never removes the fixture, selected store/exports, project or prepared runtime evidence. Write outcomes may be NotApplied, Applied or Unknown; a failure message alone does not prove rollback or absence of effects.

Direct local filesystem persistence and temporary bootstrap files are deliberate fixture/provider boundaries, not production provider acceptance. CAR publication, warehouse, previous-release ABI compatibility and live Help/Web/MCP routes remain unverified. Consult the [integration register](../../../../docs/notes/phase-1-integration-register.md#tkl-int-06) for the exact strict-lint/tooling observations and the [owner proposal package](../../../../docs/journal/2026/10/2026-10-02-phase-1-integration-proposals-v1.md) for separate external work and pending release binding.
