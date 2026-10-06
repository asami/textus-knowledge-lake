# Editing Studio Original Material integration

date=2026-10-06
status=design-note

## Context

NICT Editing Studio mobile application captures potentially large original material such as book/page photographs and audio recordings. The server Editing Studio uses these originals for full editing, transcript correction and bibliographic enrichment.

The existing TKL model already distinguishes Managed Candidate Source and explicitly includes mobile book photographs, audio, transcripts and capture-derived metadata. Therefore Editing Studio originals do not require a new TKL source category.

## Decision

Initial Editing Studio implementation may store original material directly in Google Drive through its own provider-neutral OriginalMaterialStore boundary. Editing Studio persists an OriginalMaterialRef plus metadata/provenance rather than large media bytes in ordinary application records.

Full TKL integration is not a prerequisite for this first implementation.

Future integration maps Editing Studio original material to the existing TKL Managed Candidate Source path:

Editing Studio Annotation Candidate -> OriginalMaterialRef -> Google Drive material -> TKL Managed Candidate Source -> preparation / PreparedMaterial as required.

The same Drive object may remain the physical material; integration does not imply copying all originals into a second storage tree.

## Boundary

- Google Drive is the initial physical backend.
- Editing Studio owns capture/full-editing application semantics.
- TKL owns Knowledge Lake resource/evidence/preparation semantics once the material is admitted to TKL management.
- Provider-specific Drive folder/file layout is not the semantic contract.
- Avoid mandatory duplication of large photo/audio originals.
- No integrity/hash ledger is introduced merely because the material is original evidence.

## Follow-up

When Editing Studio/TKL integration is implemented, define stable reference and metadata mapping between OriginalMaterialRef and TKL Managed Candidate Source/Resource identity, including lifecycle and access behavior. Preserve the existing provider-neutral TKL model.


## Revision: materialize TKL-compatible Material Set from the start

The initial direct-Drive implementation is refined: Editing Studio should not treat Drive as an unstructured blob store. It should materialize each submitted source package in the minimum TKL-compatible Material Set representation from the beginning, even while the write path is implemented directly with Google Drive APIs.

This allows later control to move from Editing Studio -> Drive to Editing Studio -> TKL -> Drive without bulk data migration. TKL is expected to operate the raw/original material through its Managed Candidate Source/Resource/Evidence model.

The Material Set should be directly useful to Google Workspace processing. Gemini in Drive is part of the standard preparation route. Gemini Notebook can consume selected Material Sets when deeper curated investigation/synthesis is useful, but Notebook is optional and does not define storage identity or validity.
