# TKL Application Model — Initial CML Draft

- Date: 2026-09-23
- Current status: Phase 1 closed on 2026-10-03: three accepted local Steps, independent full review and 251-test final suite complete; external integration remains proposed/unverified.
- Owner: Textus Knowledge Lake
- Update rule: Preserve all Use Case IDs and design meaning; record actual local evidence and explicitly version any later integration contract.
- Scope: Application Use Cases + Phase 1 Application Workflow

## Project-local Phase 1 authority (2026-10-02)

[Phase 1](../phase/phase-1.md) is development confined to TKL. The complete application scenarios below retain external actors, while Phase 1 exercises their boundaries with repository-owned fixtures. No TKW/TEAI/KnowledgeHub/BoK service, external project build or contract acceptance is required. TKL does not replace downstream candidate persistence, review/approval or Admission. Local receipt/Candidate references are simulated results and prove only TKL-side handling.

The [integration register](phase-1-integration-register.md) owns tracking and closing proposals. TKL closure supplies versioned contracts, examples, local evidence and recommended external work; it does not require external implementation. This section supersedes any stronger live-integration reading of the initial use-case draft below.

## Current native-model realization (2026-10-02)

[Native application model v1](../spec/native-application-model-v1.md) specifies the [native CML source](../../src/main/cozy/textus-knowledge-lake.cml), all 31 Use Case IDs, the project-local preparation main flow, actors, command DTO/service and PreparationProposalWorkflow StateMachine. Selected existing constructs are COMPONENT, ACTOR, VALUE, SERVICE/OPERATION, ENTITY-associated StateMachine and nested USE CASE/MAIN FLOW. The previous exact-syntax TODO for these declarations is superseded by the source and spec; actual parser/generator, compiler and generated fidelity evidence preserves all 31 Use Cases, eight states, eight events, ten transitions and required DTO/main-flow/pre/postcondition projections. Released runtime preparation retained that fidelity; eight native cases and the representative CLI passed. See the [proposal and evidence journal](../journal/2026/10/2026-10-02-phase-1-integration-proposals-v1.md).

The accepted Step 1 preparation contract and Step 2 local preparation/delivery flow remain authoritative. The native package consumes those six canonical Scala/JSON models without changing their persistence, producer-scoped proposal identity or uncertain-delivery semantics. UC-KA-05 has local TKL-side handling; receiving TKW integration remains unverified. All 30 future Use Cases are declared planned and their operations remain later scope.

The released build pins sbt-cozy 0.1.16, Cozy 0.3.2.2, CNCF 0.5.2 and Scala 3.3.8. Slice A declares the native surface; Slice B executes the accepted local flow through real generated Component dispatch. Its eight unchanged native tests and CLI passed, with revision 7 and unknown/confirmed observations. Dedicated WORKFLOW support is unverified in the pinned release, so the selected StateMachine expresses the local modeled workflow without claiming automated/Composite execution. Application Capability and UseCaseSlice relationships below remain design intent. All three local Steps are accepted; the sole independent full Phase review and final 251-test suite passed. See the public package for actual source and closure bindings. The versioned owner proposal package carries the accepted native source and actual final evidence bindings.

## Google Workspace backend and processing contexts (2026-10-02)

The first implementation target is Google Workspace-backed TKL. Drive folders hold resources, canonical TKL metadata JSON and PreparedMaterial. Drive Project and Gemini Notebook are realizations of a provider-neutral Knowledge Processing Context, with purpose, source/version references, Existing Knowledge Context, processing history and output lineage. Contexts and storage folders are not fixed one-to-one.

Notebook is available as a modeling option for preparation, analysis and synthesis as well as presentation. Drive Project remains the first implementation priority; Notebook integration is not required for the initial slice. Native local command DTO/CML declarations are specified above; provider automation capabilities remain to be established.

TKL proposes Raw Candidates to TKW and can prepare resources for an existing TKW candidate. TKW retains candidate formation, editing, review, approval and Admission. See [Google Workspace-backed TKL](google-workspace-backed-tkl.md) and [the decision journal](../journal/2026-10-02-google-workspace-backed-tkl-contexts.md).

### Review follow-up: logical context versus execution

KnowledgeProcessingContext is a persistent TKL entity with current evidence selection and externalContextBindings. ProcessingRun records one execution's fixed input and existing-knowledge snapshots, processor/actor, result, partial failure and output references. Context updates do not rewrite historical Runs. PreparedMaterial versions identify their producing Run; the input manifest and retained source content are distinct, with explicit retention and known/unknown provenance boundaries.

RawCandidateProposal carries a producer-scoped proposalId and versioned material/evidence references. TKW receipt and its Candidate identity are recorded separately. Identical retries must return the same receipt/candidate mapping; conflicting content under the same key must not silently overwrite it. CandidatePreparationRequest uses requestId and tkwCandidateRef to correlate the requested preparation, Run and returned material. JSON revision checks require a verified write-concurrency guarantee. These contracts are to be specified with the persistence adapter and TKW. See [review follow-up decisions](../journal/2026-10-02-google-workspace-backed-tkl-review-follow-up.md).

## Modeling intent

Model TKL Use Cases and Workflows separately in CML.

- Use Case: The goal a user wants to achieve using TKL
- Application Capability: An application capability that realizes a Use Case
- Workflow: An execution model that coordinates capabilities to achieve the goal

Phase 1 does not implement every Use Case. The Application Model defines future uses; its first Use Case Slice locally implements and verifies generation/export of a Raw Knowledge Candidate from Evidence and TKL-side external-boundary result handling. Actual Workbench connection is included in the closing integration proposals.

## Actors

- Knowledge Worker — A user who requests Knowledge Candidate discovery/proposals and examines their evidence
- Textus Knowledge Workbench — The downstream application that receives Raw Knowledge Candidates and performs Formation / Edit / Review / Approval
- Resource Provider — An Evidence provider such as Google Workspace or Slack
- Knowledge Processor — LLM / deterministic processor / KnowledgeHub-oriented processor

TEAI / OpenClaw are generally integration mechanisms rather than Actors.

## Application Use Cases

### Knowledge Acquisition

#### UC-KA-01 Propose Raw Knowledge Candidate — Phase 1 Primary

Goal:
Extract a Raw Knowledge Candidate worth forming into Knowledge from distributed Evidence, propose it with its Evidence, and hand it to Textus Knowledge Workbench.

Primary Actor:
Knowledge Worker

Supporting Actors:
Textus Knowledge Workbench, Resource Provider, Knowledge Processor

Main scenario:

1. The Knowledge Worker requests a knowledge candidate proposal.
2. TKL assembles the Knowledgeization Scope from the purpose and Context.
3. TKL resolves Resources such as Drive / Gmail / Slack.
4. TKL collects and normalizes the required Evidence.
5. TKL generates PreparedMaterial.
6. The Knowledge Processor extracts a Knowledge Candidate from PreparedMaterial.
7. TKL composes the Candidate, Evidence, Context, rationale and unresolved questions into a Raw Knowledge Candidate Proposal.
8. The Knowledge Worker examines the Proposal and Evidence.
9. TKL hands the Raw Knowledge Candidate and PreparedMaterial / Evidence provenance to Textus Knowledge Workbench.
10. Workbench performs Candidate editing, Semantic Mapping, comparison with existing Knowledge, Human Review / Approval and KnowledgeHub Formation.

Postconditions:

- The Knowledge Candidate exists as an evidence-grounded Proposal.
- Provenance is traceable from the Candidate to PreparedMaterial and canonical Evidence.
- The Raw Knowledge Candidate can be handed to the Textus Knowledge Workbench boundary with complete PreparedMaterial / Evidence provenance.

### Knowledge Acquisition supporting use cases

- UC-KA-02 Explore Candidates by Theme — Explore Knowledge Candidates for a specified theme
- UC-KA-03 Discover Candidates from New Evidence — Discover candidates from new Evidence
- UC-KA-04 Inspect Candidate Evidence — Inspect the evidence supporting a Candidate
- UC-KA-05 Deliver Candidate to Knowledge Workbench — Deliver a Raw Candidate to Workbench

Candidate comparison with existing Knowledge, modification, Human Review, Approval and Admission are Textus Knowledge Workbench Use Cases.

### Research / Exploration

- UC-RE-01 Research Theme Across Sources
- UC-RE-02 Trace Historical Context
- UC-RE-03 Compare Views
- UC-RE-04 Analyze Consensus
- UC-RE-05 Extract Open Questions
- UC-RE-06 Answer from Evidence

These may terminate at PreparedMaterial or research artifacts without producing KnowledgeHub candidates.

### Project Awareness

- UC-PA-01 Review Recent Changes
- UC-PA-02 Review New Evidence
- UC-PA-03 Identify Active Discussions
- UC-PA-04 Review Open Issues
- UC-PA-05 Generate Project Digest
- UC-PA-06 Notify Significant Change

### Content Production

- UC-CP-01 Generate Report
- UC-CP-02 Generate Slide Deck
- UC-CP-03 Generate Infographic
- UC-CP-04 Generate Mind Map
- UC-CP-05 Generate Audio / Video
- UC-CP-06 Generate LLM Context

Gemini Notebook is one optional provider for these production use cases; it is not required for KnowledgeHub acquisition.

## Application Capabilities

Initial capabilities derived from the primary use case:

- Resource Resolution
- Evidence Discovery
- Scope Assembly
- Evidence Normalization
- Communication Normalization
- Resource Materialization
- Knowledge Preparation
- Prepared Material Management
- Candidate Extraction
- Raw Candidate Proposal
- Candidate Delivery to Knowledge Workbench
- Provenance / Lineage Resolution

## Phase 1 Application Workflow

### WF-KA-01 Raw Knowledge Candidate Proposal Workflow

Realizes:
UC-KA-01 Propose Raw Knowledge Candidate

```text
Start
  |
  v
Determine Knowledgeization Scope
  |
  v
Resolve Knowledge Resources
  |
  v
Collect Evidence
  |
  v
Normalize Evidence
  |
  +--> Communication VO for Gmail / Slack
  |
  v
Create / Resolve KnowledgeProcessingContext
  |
  v
Bind External Processing Context
  |
  v
Create ProcessingRun / Capture Input and Existing Knowledge Snapshots
  |
  v
Prepare Knowledge Material
  |
  v
Persist PreparedMaterial
  |
  v
Extract Raw Knowledge Candidate
  |
  v
Compose Raw Candidate Proposal
  |
  v
Present Candidate + Evidence
  |
  v
Export Proposal to Local Workbench Boundary Fixture
  |
  v
Record TKL Result / Simulated Receipt Correspondence
  |
  v
End
```

## Phase 1 Use Case Slice

The first implementation slice uses project-local fixtures representing a small Drive source set and manually supplied preparation results. The full application target permits manually assisted Drive Project processing. Phase 1 validates TKL behavior independently of provider automation and downstream services; Gmail / Slack, KAR packaging, live feedback and existing-candidate preparation remain follow-on scope.

Given:

- a small set of local Drive-shaped Evidence fixtures;
- versioned Existing Knowledge Context fixture where relevant;
- a TKL processing context with Drive Project binding metadata.

When:

- a Knowledge Worker requests a Knowledge Candidate Proposal for a theme

Then:

- provider resources are represented by stable TKL resource IDs;
- the ProcessingRun records its fixed input references, retention status and Existing Knowledge snapshot;
- a PreparedMaterial Entity is created and persisted;
- PreparedMaterial can be exported losslessly as JSON;
- Markdown can be rendered from the canonical model without re-reading sources;
- at least one Raw Knowledge Candidate Proposal is generated;
- the Proposal includes provenance to its Evidence and PreparedMaterial;
- the RawCandidateProposal is exportable at the proposed Workbench boundary;
- supplied fixture receipts / Candidate references are correlated and recorded by TKL; timeout remains unknown and query/retry preserves the same proposal identity;
- downstream receipt durability and duplicate Candidate prevention are documented TKW requirements, not guarantees proven by this local slice;
- Candidate formation/review/approval is explicitly outside TKL Phase 1.

## Provider mapping for the NICT pilot

This mapping is implementation-specific and not part of the CML semantic model.

```text
Google Drive        -> File Resource Provider
Gmail               -> Mail Communication Provider
Slack               -> Conversation Provider
Drive Project       -> Saved context for Gemini in Drive preparation
Gemini in Drive     -> Preparation processor
Gemini Notebook     -> Alternative Knowledge Processing Context (preparation/analysis/synthesis/presentation)
Textus Knowledge Workbench -> Raw Candidate formation/review/approval consumer
KnowledgeHub        -> downstream Knowledge runtime reached through Workbench
```

## CML implementation direction

The selected native declarations are now recorded in the source and native spec above. The following broader relationships remain conceptual follow-up design intent; they do not assert additional supported syntax or implemented operations.

Expected relationships:

```text
UseCase
  realizes goal
  requires ApplicationCapability*

ApplicationWorkflow
  realizes UseCase
  invokes ApplicationCapability*

ApplicationCapability
  is provided by Component/Operation/Workflow capability

UseCaseSlice
  selects scenario path + executable specification
```

Any later syntax must follow verified SimpleModeling/CML definitions and must not be invented locally in TKL. Native generation/compiler preservation and actual operation execution have local acceptance evidence from the accepted Step, independent full Phase review and final suite. Future constructs require separate verified capability evidence.


## Knowledge Workbench boundary (2026-09-23)

The previous Phase 1 wording that TKL sends a reviewed Candidate directly to KnowledgeHub is superseded.

TKL performs discovery and preparation and produces a **raw knowledge candidate / candidate proposal**. Human formation, editing and approval belong to a reusable Knowledge Workbench layer.

```text
Evidence
  -> TKL
  -> PreparedMaterial
  -> Raw Knowledge Candidate
  -> Knowledge Workbench
  -> KnowledgeFormationProposal
  -> Human Approval
  -> KnowledgeHub
```

For the NICT project, TKL therefore integrates primarily with the Knowledge Workbench candidate boundary rather than directly with KnowledgeHub Admission.

The Phase 1 primary use case remains candidate proposal, but its postcondition is now: the proposal can be handed to Knowledge Workbench with complete evidence/provenance. Direct KnowledgeHub admission is not a TKL responsibility.

## Existing Knowledge feedback use case

Raw Candidate discovery must use existing Knowledge context when relevant.

Add supporting use case:

- **UC-KA-06 Explore Candidates Against Existing Knowledge** — synchronize/resolve relevant KnowledgeHub Knowledge and use it as preparation context when extracting Raw Knowledge Candidates.

Workflow refinement:

```text
Determine Scope
 -> Resolve Evidence
 -> Resolve Existing Knowledge Context
 -> Normalize / Project Context
 -> Prepare (Existing Knowledge + New Evidence)
 -> PreparedMaterial
 -> Raw Candidate Proposal
 -> Knowledge Workbench
```

The Google reference implementation projects relevant existing Knowledge into Drive/Drive Project so Gemini in Drive can perform preparation with both the current Knowledge baseline and new Drive/Gmail/Slack Evidence.


## Knowledge Feedback application use cases

Knowledge Feedback is a first-class application-use-case group, not merely an internal synchronization workflow.

### Existing use-case impact

Existing TKL use cases must reason against current Knowledge when relevant.

- **UC-KA-01 Propose Raw Knowledge Candidate** — preparation uses Existing Knowledge Context + New Evidence. Proposal should identify related/baseline Knowledge and characterize the observed difference where possible: new, update, support, conflict, or relation.
- **UC-RE-01 Research Theme Across Sources** — include relevant current Knowledge as a research context/source.
- **UC-RE-02 Trace Historical Context** — trace current Knowledge back through Candidate/PreparedMaterial/Evidence where available.
- **UC-RE-03 Compare Views** — compare Evidence-derived views with current Knowledge as well as with each other.
- **UC-RE-04 Analyze Consensus** — compare current admitted Knowledge with consensus visible in recent Evidence.
- **UC-RE-05 Extract Open Questions** — check whether an apparent open question is already resolved by current Knowledge.
- **UC-PA-01 Review Recent Changes** — distinguish ordinary resource change from change that may affect current Knowledge.
- **UC-PA-06 Notify Significant Change** — prioritize Evidence that supports, updates, conflicts with, or creates relations to current Knowledge.

### UC-KF-01 Synchronize Knowledge Context

Goal:
Keep TKL's preparation context aligned with current canonical KnowledgeHub Knowledge.

Main scenario:

1. Detect or request changed/relevant Knowledge.
2. Resolve Knowledge identity and version.
3. Create/update provider-neutral KnowledgeReference / KnowledgeProjection.
4. Store/update the projection in TKL.
5. Mark superseded/stale projections as appropriate.
6. Make the current projection available to Preparation.

### UC-KF-02 Project Knowledge to Preparation Workspace

Goal:
Make relevant KnowledgeProjection content usable by an external preparation processor.

Google reference scenario:

```text
TKL KnowledgeProjection
 -> Google Workspace Adapter
 -> Google Docs/files
 -> Drive Project
 -> Gemini in Drive
```

The external representation is not authoritative Knowledge.

### UC-KF-03 Refresh Knowledge Context after Formation

Goal:
Close the acquisition loop after a candidate is formed/approved.

```text
TKL Candidate
 -> TKW
 -> KnowledgeHub Formation
 -> formed Knowledge identity/version
 -> TKL KnowledgeProjection refresh
 -> next Preparation
```

### UC-KF-04 Trace Knowledge Formation

Goal:
Allow a Knowledge Worker to trace formed Knowledge back through formation/candidate/preparation to canonical Evidence where available.

Expected trace:

```text
Knowledge
 -> FormationProposal / Candidate
 -> PreparedMaterial
 -> EvidenceReference
 -> canonical provider resource
```

### Feedback capabilities

Initial capabilities:

- Knowledge Change Detection
- Knowledge Reference Resolution
- Knowledge Projection
- Knowledge Context Synchronization
- Preparation Workspace Projection
- Projection Version/Staleness Management
- Formation Trace Resolution


## Knowledge Projection synchronization use cases

Knowledge Feedback includes both incremental and replacement-style synchronization.

- **UC-KF-05 Append Knowledge Projection Delta** — append new/update/supersede Knowledge changes to the TKL preparation context.
- **UC-KF-06 Rebuild Knowledge Context** — replace/compact the accumulated context with a clean snapshot derived from canonical current KnowledgeHub state.
- **UC-KF-07 Publish Current Knowledge Context** — synchronize current snapshot + post-snapshot deltas to a preparation workspace such as Google Drive/Drive Project.

Rebuild is a normal lifecycle operation because long-running append-only contexts accumulate obsolete versions, duplicates and superseded Knowledge. Candidate preparation should consume a clear current baseline.

Reference workflow:

```text
KnowledgeHub changes
 -> append KnowledgeProjectionDelta*
 -> TKL current context
 -> periodic/on-demand rebuild from KnowledgeHub current state
 -> KnowledgeContextSnapshot
 -> publish snapshot + later deltas
 -> Gemini/other preparation
```

## Phase 1 accepted source and final evidence

Accepted native Step 3: `06c144b91111c408a0d0699178bffad66a7252fd`; source tree: `a3d54dd9cd55b96c640cfc00ab2d64d68c6d6bfc`. Independent full Phase review `TKL-P1-full-review-result/1` passed with zero blockers. Final `sbt --batch test` (`tkl-p1-final-full-suite-1`) passed all 251 tests in six suites, with zero failed/canceled/ignored/pending tests, zero aborted suites, exit 0 and released serial lock. The eight native scenarios and retained CLI revision 7 establish local fixture behavior. Closure reference: `tkl-phase-1-closure@1`; the distinct local release commit records this binding in its Git message. External guarantees remain unverified.
