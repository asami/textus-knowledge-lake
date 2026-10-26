# Phase 1 — Knowledge Lake Material Access and Processing Reference Flow

status=planned
updated_at=2026-10-21
spec=../spec/material-access-and-processing.md
checklist=material-access-and-processing-plan.md

Updated scope — 2026-10-24: [mobile Material delivery](tkl-managed-mobile-material-delivery-plan.md)
extends TKL-M03 with operator-owned destination configuration and Android
delivery instructions. No mobile folder-ID setup is required; Workspace Shared
Drive is the first live target. Include that workstream's checklist when closing
M03; OpenClaw is not a prerequisite for the mobile route.

This reconciliation replaces the old ingestion-only direction. It records no
implementation or acceptance and resets no existing execution history.

## Goal

Provide one logical Knowledge Lake component with ordinary Material access for
Workbench and processing through OpenClaw continuation. Both use the same
Material identity, metadata and provider correspondence. Deliver the ordinary
reference route first under the [workstream checklist](material-access-and-processing-plan.md).

## Reference flow

```text
Editing app -> Workbench -> TKL Material query -> Drive-backed original
OpenClaw <-> TKL continuation processing -> same Material query model
TKL proposal/references -> Workbench Candidate/human approval -> KnowledgeHub
```

## Scope

### Architecture

- define Material search/metadata/URN/access and registration contracts;
- compose TKL as a public CNCF component dependency of Workbench;
- retain provider-neutral identities/provenance and TKL-owned physical mapping;
- separate ordinary reads from OpenClaw continuation processing;
- retain Candidate lifecycle/human approval in Workbench and canonical
  Information admission in KnowledgeHub.

### Google Workspace reference

- use Drive as the first physical provider behind TKL;
- search and access one real Material through Workbench and Studio;
- retain source/provenance across one selected preparation result;
- preserve existing originals; no blanket copying or bulk corpus migration.

### OpenClaw continuation integration

- register TKL with OpenClaw using the existing continuation protocol;
- bind a concrete work request, result and resumption through CNCF;
- preserve processing correlation, provenance and unfinished/result distinction;
- do not require OpenClaw for ordinary reads or invent a second continuation
  engine. Reuse TEAI integration only where the selected mechanism requires it.

### TKL processing workflow

- select one collection/preparation/organization task;
- resolve its source Material and delegate/continue required work;
- retain the result and its provenance under the shared Material model;
- expose that result through the same ordinary access Operations;
- hand proposals/references to Workbench without automatically approving or
  admitting Information.

## Non-goals

Phase 1 does not attempt to:

- reproduce NotebookLM in TKL;
- ingest every raw Google Drive file;
- implement broad source summarization/Q&A in TKL;
- support every Google Workspace API;
- duplicate TEAI integration infrastructure;
- duplicate CNCF runtime facilities;
- fully automate all semantic/curation decisions.

## Completion criteria

Close this Phase only from the [M01-M03/P01-P02 checklist](material-access-and-processing-plan.md):
one real reference route, TKL-owned registration/placement and one OpenClaw
processing/resumption result visible through the shared query model. Accept and
hand off the reference slice before the full processing Phase is complete.
KnowledgeHub admission and BoK publication are downstream obligations, not
additional gates on this TKL delivery.


## Google reference implementation priority

Phase 1 should prioritize the Google preparation route based on **Drive + Drive Project + Gemini in Drive** where live Google integration is introduced.

Gemini Notebook integration is not required for Phase 1 closure. It is an optional later presentation/production integration and must not expand the Phase 1 scope.


## Editing Studio Managed Candidate Material Set (2026-10-06)

Phase 1 must define a minimum TKL-compatible Material Set representation for managed candidate sources produced by applications such as NICT Editing Studio. The first producer is allowed to materialize the set directly into Google Drive before a TKL API exists, but the stored structure must be ingestible and operable by TKL without bulk migration.

Minimum logical content:

- stable Material Set identity and manifest/metadata;
- original source files such as book/page photographs and audio;
- derived artifacts such as transcript and capture-derived ISBN/title/annotation metadata;
- provenance connecting derived artifacts to original sources;
- producer/application and candidate identity references;
- media/type metadata sufficient for TKL Resource/Evidence mapping.

The physical Google Drive folder/file layout is an implementation mapping, not canonical TKL identity. Avoid mandatory duplication of large originals. Existing Drive objects may become TKL-managed resources through stable references.

Drive + Drive Project + Gemini in Drive remains the standard initial Google preparation route. Gemini Notebook remains optional for curated investigation, synthesis or presentation where useful; the Material Set must not require Notebook to be valid. Gemini-facing documents/views may be projections while canonical management metadata remains in the TKL-defined representation.

Acceptance/planning must include a concrete Editing Studio-style fixture containing photographs, audio, transcript and capture-derived metadata and demonstrate that TKL can identify the set, its evidence/provenance and its preparation inputs.
