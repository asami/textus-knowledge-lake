# Review: Google Workspace-backed TKL and Knowledge Processing Contexts

- Date: 2026-10-02
- Status: Design review and follow-up proposal
- Reviewed commit: `f12542e4696585be7a73b2ddf4edec922ceb9e18`
- Scope: Google Workspace-backed persistence, Knowledge Processing Context, provenance, TKW handoff

## Review summary

The 2026-10-02 design is internally coherent and gives TKL a clearer boundary. The most important decision is the separation of the persistent Knowledge Lake from the environments in which knowledge processing happens.

- TKL Drive folders are the initial persistent backend.
- Drive Project and Gemini Notebook are realizations of a provider-neutral Knowledge Processing Context.
- Drive JSON is the initial system of record for TKL metadata and PreparedMaterial; a separate TKL database is not required.
- TKL owns resources, evidence, preparation, PreparedMaterial and provenance.
- TKW owns candidate formation, editing, review, approval and Admission.
- KnowledgeHub remains the system of record for established knowledge and projects that knowledge back into TKL as Existing Knowledge Context.

This structure should be retained. In particular, storage, processing context and candidate lifecycle should not be collapsed into one aggregate or one provider-specific abstraction.

## Important follow-up corrections

README, Phase 1 and the CML scaffold should be reconciled early. Old assumptions such as a mandatory TKL database or Notebook being presentation-only are dangerous because implementation agents may treat them as current requirements.

The current note correctly states that folder names and Drive IDs are not canonical TKL identities. This should become an explicit invariant rather than remain only explanatory text.

## Proposed core model

The following conceptual split is recommended.

### Resource

A stable TKL reference to material managed by TKL or an external provider.

Suggested minimum properties:

- `resourceId`: canonical TKL ID;
- `providerRef`: provider type and external identifier;
- `resourceType`;
- `currentVersionRef`;
- `mediaType`;
- `provenanceRef`;
- lifecycle/status information.

A provider path, Drive file ID or folder location is a locator, not the semantic identity.

### Evidence

Evidence is the use of a Resource as evidence in a particular knowledge-processing scope. Resource and Evidence should not be synonyms.

This permits the same Resource to participate in several contexts with different roles, relevance and effective versions.

Suggested properties include `evidenceId`, `resourceId`, role, selected version/snapshot, acquisition time and optional interpretation metadata.

### KnowledgeProcessingContext

This should be a first-class TKL entity or aggregate root representing a persistent logical processing context, not an individual execution.

Suggested structure:

```text
KnowledgeProcessingContext
  contextId
  purpose
  subject / theme
  state
  currentEvidenceSet
  existingKnowledgeContext
  externalContextBindings
  processingPolicy
  createdAt / updatedAt
```

A context may bind to zero or more external contexts such as a Drive Project or Gemini Notebook. The external object must not become the canonical identity of the TKL context.

### ProcessingRun

Separate each actual execution from KnowledgeProcessingContext.

```text
ProcessingRun
  runId
  contextId
  requestedAt
  startedAt
  completedAt
  processor
  actor / agent
  inputSnapshot
  existingKnowledgeSnapshot
  request
  outputRefs
  result
  failure / consequence
```

This distinction is important. `currentEvidenceSet` expresses what the context currently contains, while `inputSnapshot` records exactly what a particular run used. Updating a context must not rewrite historical provenance.

A ProcessingRun is also the natural integration point with CNCF Job / Workflow and later TEAI/OpenClaw execution.

### PreparedMaterial

PreparedMaterial should remain a stable TKL Entity. Each generated version should point to the ProcessingRun that produced it.

Suggested identity relationship:

```text
PreparedMaterial
  materialId
  version
  producedByRunId
  sourceEvidenceRefs
  contentRef
  canonicalJsonRef
  projections
  createdAt
```

A Markdown/Google Docs representation is a projection. It must not silently become the canonical state.

## Identity and version proposal

Use TKL-generated stable IDs independent of providers.

Conceptually:

```text
textus:klake:resource:<id>
textus:klake:evidence:<id>
textus:klake:context:<id>
textus:klake:run:<id>
textus:klake:prepared-material:<id>
```

The exact syntax can be aligned with the existing Textus ID conventions, but the important invariant is that provider migration must not change semantic identity.

Versions should distinguish at least:

1. semantic entity version;
2. provider version/revision;
3. immutable ProcessingRun input snapshot.

These should not be represented by one overloaded `version` field.

## Drive JSON persistence proposal

For the initial backend, prefer small independently updateable canonical JSON records rather than one large metadata document.

A logical layout could be:

```text
Metadata/
  resources/
  evidence/
  contexts/
  runs/
PreparedMaterial/
KnowledgeContext/
```

This is a logical contract, not a required folder naming convention.

Each canonical record should contain:

- schema/version identifier;
- TKL stable ID;
- entity revision;
- timestamps;
- provider references where applicable;
- provenance references.

For updates, use optimistic concurrency based on the previously read revision/provider version. Do not use content hashes as an application-level concurrency or identity mechanism. Provider-native revision/ETag-like facilities or an explicit monotonically increasing entity revision are preferable.

A partial failure must leave enough state to distinguish at least requested, running, succeeded and failed processing. External output creation and TKL metadata update cannot be assumed atomic.

## Provenance proposal

Provenance should be modeled as explicit relationships rather than a free-form log.

The minimum useful chain is:

```text
Resource(version)
  -> Evidence(snapshot)
  -> ProcessingRun
  -> PreparedMaterial(version)
  -> RawCandidateProposal
```

For generated content, record processor/model/tool identity when known, request or operation identity, human intervention when relevant, and the exact input snapshot available to TKL.

Do not claim reproducibility for information that Drive Project, Notebook or another external processor does not expose. Provenance should represent known facts and explicitly allow unknown details.

## TKW handoff proposal

Define an explicit handoff contract rather than passing an arbitrary PreparedMaterial object.

A conceptual `RawCandidateProposal` should contain:

```text
proposalId
candidateKind
preparedMaterialRefs
evidenceRefs
provenanceRef
suggestedContent
sourceContextId
sourceRunId
createdAt
```

TKW should create or update its own candidate identity after accepting the proposal. The proposal is not itself a TKW Candidate.

For Candidate preparation, use a correlation/reference to the existing TKW candidate:

```text
CandidatePreparationRequest
  tkwCandidateRef
  resourceRefs
  purpose
  requestedProcessing
```

and return PreparedMaterial/provenance linked to that request. This keeps TKL from taking ownership of the TKW candidate lifecycle.

## Knowledge feedback proposal

KnowledgeProjection should be treated as a versioned projection of KnowledgeHub knowledge into TKL, not a copied authoritative knowledge store.

A ProcessingRun should record the exact KnowledgeProjection snapshot/reference used. This allows later explanation of which established knowledge was available when a new candidate was produced.

Incremental projection remains the normal operation. Periodic rebuild/replacement can compact duplicates and obsolete projections without changing KnowledgeHub authority.

## Aggregate boundary proposal

A reasonable first implementation is:

- Resource: independent entity/aggregate depending on CNCF persistence needs;
- KnowledgeProcessingContext: aggregate root for current context configuration and bindings;
- ProcessingRun: append-oriented execution entity, preferably immutable after terminal completion except operational annotations;
- PreparedMaterial: independent versioned entity;
- KnowledgeProjection: projection/read-oriented entity;
- RawCandidateProposal: handoff entity/message.

Avoid making one large `KnowledgeLake` aggregate containing all resources and runs. It would create unnecessary contention and make Drive-backed persistence awkward.

## Phase 1 concrete slice

The first executable vertical slice can be made deliberately small:

```text
register Resource
 -> select Evidence
 -> create KnowledgeProcessingContext
 -> bind Drive Project
 -> start ProcessingRun
 -> record immutable input snapshot
 -> perform/manual-assist Drive preparation
 -> import output
 -> create PreparedMaterial
 -> create RawCandidateProposal
 -> hand off to TKW
```

Automation of Drive Project itself does not need to be complete initially. A manually performed external processing step is acceptable if TKL records the request, input snapshot, imported output and provenance boundary correctly. This tests the domain contract before investing in provider automation.

## Suggested order of specification work

1. Define invariants and IDs for Resource, Evidence, Context, Run and PreparedMaterial.
2. Define canonical JSON schemas and entity revision/concurrency rules.
3. Define KnowledgeProcessingContext versus ProcessingRun lifecycle/state machines.
4. Define provenance links and immutable run snapshots.
5. Define RawCandidateProposal and CandidatePreparationRequest contracts with TKW.
6. Reconcile README, Phase 1 and CML scaffold with the 2026-10-02 decisions.
7. Implement the small Drive-backed vertical slice.
8. Add Notebook, Gmail, Slack and deeper automation only after the core contract works.

## Review conclusion

The current direction is suitable as the architectural baseline. The key next step is not to add more provider integrations, but to turn the newly established separation of storage, processing context, execution and candidate lifecycle into explicit domain contracts.

The highest-value refinement is the introduction of `ProcessingRun` distinct from `KnowledgeProcessingContext`. It gives TKL a stable place to record immutable input snapshots, provenance, execution result and partial failure while allowing a processing context to evolve over time.

The second priority is the TKW handoff contract. Once `RawCandidateProposal` and candidate-preparation correlation are explicit, the TKL/TKW ownership boundary becomes implementable rather than only architectural.
