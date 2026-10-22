# Phase 1 — Knowledge Lake Material Access and Processing Reference Flow

status=planned
updated_at=2026-10-21
spec=../spec/material-access-and-processing.md
checklist=material-access-and-processing-plan.md

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
