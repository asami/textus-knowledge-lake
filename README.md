# Textus Knowledge Lake (TKL)

- Current status: Phase 1 closed on 2026-10-03: three accepted local Steps, independent full review and 251-test final suite complete; external integration remains proposed/unverified.
- Owner: Textus Knowledge Lake
- Update rule: Record actual accepted source and validation evidence; preserve v1 contracts and distinguish local fixtures from external acceptance.

Textus Knowledge Lake manages provider-neutral resources, evidence, knowledge-processing contexts, preparation and traceable candidate proposals. **TKL itself is the Knowledge Lake.** Google Workspace is its first production backend target.

Drive folders hold source/working artifacts and canonical TKL metadata JSON / PreparedMaterial. KnowledgeProcessingContext describes a persistent purpose and selected evidence; Drive Project and Gemini Notebook are possible external realizations. ProcessingRun records one execution and its fixed inputs, results and lineage. The exact Notebook product/API is an adapter-design question.

## Responsibility boundaries

| Owner | Responsibility |
| --- | --- |
| TKL | Resource / Evidence identity, Context / Run, preparation, PreparedMaterial, provenance, RawCandidateProposal and existing-information projections |
| Google Workspace / other providers | Physical resource representations and external processing facilities |
| TKW | Candidate identity, formation, editing, review, human approval and Admission requests |
| KnowledgeHub | Canonical Information and its versions |
| TEAI / OpenClaw | Enterprise integration and physical/tool-mediated access where used |
| BoK | Curated knowledge publication |

RawCandidateProposal is a TKL proposal, separate from a TKW Candidate. TKL does not directly review/approve or admit a Candidate into KnowledgeHub.

```text
Provider resources
  -> TKL Resource / Evidence
  -> KnowledgeProcessingContext / ProcessingRun
  -> PreparedMaterial(version)
  -> RawCandidateProposal
  -> TKW formation / edit / review / approval
  -> KnowledgeHub Information
  -> TKL existing-information projection for later preparation
```

This diagram describes the eventual application relationship. Actual external integration is distinct from the project-local Phase 1 acceptance described below.

## First backend and processing contexts

The first production target uses a Drive folder or folder set. Small independent JSON records represent TKL entities; a separate TKL database is not mandatory initially. Stable TKL IDs, entity revisions, provider revisions and retained input snapshot identities have different meanings.

Drive Project is the first external processing-context target. Manual processing and import of preparation results are supported design paths. Gemini Notebook is an optional context for preparation, analysis, synthesis and production, with concrete capabilities to be verified during integration.

Source resources are referenced by default. Retain/materialize required input content according to policy; a provider revision reference alone does not prove permanent content availability. Record unretained or unconfirmed inputs explicitly.

JSON is the lossless representation of PreparedMaterial. Markdown is a derived projection for people and LLMs. Historical Run inputs and material versions remain stable when the current Context changes.

## Phase 1: project-local development

[Phase 1](docs/phase/phase-1.md) implements and validates the TKL-owned preparation/proposal path using repository-owned fixtures and executable specifications:

```text
Small Drive-shaped source fixtures
  -> Resource / Evidence -> Context / fixed-input Run
  -> preparation-result import -> persisted PreparedMaterial
  -> JSON / Markdown -> RawCandidateProposal export
  -> local boundary fixture for TKL result/correlation handling
```

Closure requires TKL-local behavior, reproducible local validation, accurate documentation, independent review and concrete external integration proposals. It does not require another project's build, service, release or contract acceptance, live Google credentials, TKW Candidate creation, KnowledgeHub Admission or a BoK publication decision.

A simulated receipt / Candidate reference proves TKL-side handling only. Durable downstream receipt, concurrent duplicate prevention and production Admission remain requirements for their owners. TKL does not implement a substitute downstream service.

The accepted Step 1 [preparation contract](docs/spec/preparation-contract-v1.md) supplies the six canonical models, strict lossless JSON and offline Markdown projection. The accepted Step 2 [local preparation flow](docs/spec/local-preparation-flow-v1.md) supplies immutable local atomic/CAS persistence, fixed-input Runs, manual-result import, producer-scoped proposals and reserved delivery with fixture result/correlation handling. These local contracts do not establish production Google or TKW acceptance.

The [acceptance checklist](docs/phase/phase-1-checklist.md) records completed local Phase acceptance. The [native application model v1](docs/spec/native-application-model-v1.md) now declares all 31 Use Cases, the local main flow, StateMachine and command DTO/service surface in CML. The selected ENTITY-associated StateMachine route has actual parser/compiler/generated fidelity evidence preserving 31 Use Cases, eight states, eight events, ten transitions and required DTO/main-flow/pre/postcondition projections. All eight native cases and the representative CLI passed through real generated Component dispatch, producing revision 7 with unknown/confirmed fixture observations. All three local Steps are accepted; the sole independent full Phase review and final 251-test suite passed. See the public package for actual source and closure bindings.

## Planned Google Workspace delivery sequence

| Phase | Owned delivery | Status | Expected duration | Acceptance |
| --- | --- | --- | --- | --- |
| [Phase 2](docs/phase/phase-2.md) | Real Drive acquisition, canonical persistence, conflicts and recovery; settled manual-interface handoff | Planned; not started | 7h | [Storage checklist](docs/phase/phase-2-checklist.md) |
| [Phase 2.1](docs/phase/phase-2.1.md) | Actual manual processing/import, native material/proposal exports and complete end-to-end acceptance | Planned; not started | 6h | [Manual/aggregate checklist](docs/phase/phase-2.1-checklist.md) |

The 2026-10-03 split retains Phase 2 as the first unit and adds Phase 2.1 after it; no later Phase is renumbered. Implementation and live verification have not started. Complete `sbt --batch test` validation is `final-only`, owned by Phase 2.1 for both members. Each member retains focused validation, live acceptance for its scope, independent review, Step acceptance and local closure. See the [original definition and applied split](docs/journal/2026-10-03-phase-2-definition.md).

## Native generation build

The root build uses released sbt-cozy 0.1.16, Cozy 0.3.2.2 and CNCF 0.5.2 with Scala 3.3.8. [Project metadata](project.yaml), [launcher settings](conf/cozy/launcher.yaml) and [generation settings](conf/cozy/config.yaml) pin the native build without a development checkout or external service. Circe/ScalaTest remain in the build; ordinary Maven packaging is retained.

Generate managed native sources and compile from this repository:

```text
sbt --batch cozyGenerate compile
```

CNCF command execution uses the registered serialized SBT runner. Released runtime preparation retained generated fidelity; focused native 8/8 and CLI success establish local normal dispatch and persisted results. Dedicated WORKFLOW/Composite execution remains unverified in the pinned release. See the [native spec and portable runtime command](docs/spec/native-application-model-v1.md#local-development-startup) and [public proposal/evidence journal](docs/journal/2026/10/2026-10-02-phase-1-integration-proposals-v1.md) for the exact observed results and completed local acceptance.

## Integration management

The [integration register](docs/notes/phase-1-integration-register.md) tracks owner, contract/version, TKL evidence, external unverified guarantees, open questions and follow-up work for each boundary. The [TKL-P1-Integration-Proposals/1 package](docs/journal/2026/10/2026-10-02-phase-1-integration-proposals-v1.md) now supplies exact sample payloads/fixtures, acceptance examples and recommended receiving-project work. Its accepted native commit/tree and actual full-review/full-validation evidence are bound below; external production integration remains unverified.

Provider adapters map the semantic model onto external capabilities. TEAI/OpenClaw may perform transport, invocation/delegation and continuation where appropriate; these mechanisms do not redefine the TKL domain model. No provider-specific physical integration or shared infrastructure is invented to bypass a missing upstream capability.

## Design records

- [Project-local Phase 1 decision](docs/journal/2026-10-02-phase-1-project-local-boundary.md)
- [Google Workspace backend and contexts](docs/notes/google-workspace-backed-tkl.md)
- [Architecture](docs/notes/architecture.md)
- [Application model](docs/notes/application-model.md)
- [Review follow-up](docs/journal/2026-10-02-google-workspace-backed-tkl-review-follow-up.md)

## Phase 1 accepted source and final evidence

Accepted native Step 3: `06c144b91111c408a0d0699178bffad66a7252fd`; source tree: `a3d54dd9cd55b96c640cfc00ab2d64d68c6d6bfc`. Independent full Phase review `TKL-P1-full-review-result/1` passed with zero blockers. Final `sbt --batch test` (`tkl-p1-final-full-suite-1`) passed all 251 tests in six suites, with zero failed/canceled/ignored/pending tests, zero aborted suites, exit 0 and released serial lock. The eight native scenarios and retained CLI revision 7 establish local fixture behavior. Closure reference: `tkl-phase-1-closure@1`; the distinct local release commit records this binding in its Git message. External guarantees remain unverified.
