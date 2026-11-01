# Knowledge Hub: EAI-mediated Material ingestion

Date: 2026-11-02
Status: proposed integration architecture / handoff

## Decision
Keep **Textus Knowledge Lake (TKL)** as the logical Material registration, owner and admission authority. Use **Textus EAI** for mail/storage adapters, transfer sessions, sandboxed tools, private artifact staging, logs, retries and cleanup. Google Drive remains a physical storage provider; do not expose its details or credentials to TKL business logic.

## Use case A: mail Excel
Mail source -> EAI mail adapter -> private attachment extraction/validation -> TKL Material registration/admission -> EAI-managed storage delivery. Preserve the original workbook as Material; parse or scan only inside constrained execution. Sender address alone is not a trusted owner identity. Record provenance, message/attachment identifiers and idempotent registration.

## Use case B: Android/iPhone images and video
**Control plane**: Mobile -> TKL (Material ID, owner, authorization) -> EAI (Drive adapter, upload session and storage policy) -> Google Drive.
**Data plane (primary)**: Mobile -> Google Drive directly, using an EAI-prepared restricted resumable upload session. Binary media does not transit TKL/EAI servers. **Fallback**: TKL relay upload for environments where direct upload fails. Client-assigned Material ID is retained per existing design.

After upload, client reports completion to TKL; TKL/EAI verify actual Drive object identity, size, integrity/format as feasible and private destination before Material admission. Expired/abandoned sessions and orphan objects require recovery policy. A Drive resumable session URI is sensitive; Drive is not interchangeable with S3 presigned URLs, and provider-side vs post-upload restrictions must be distinguished.

## Boundaries
TKL owns Material semantics, ID, owner, journal and admission; EAI owns transport mechanics, temporary artifacts, storage provider API and operational evidence. TKL Journal records registration state and provenance; EAI execution trace correlates with Material ID. Knowledge Workbench remains responsible for Candidate review; do not shift that into EAI.

## Acceptance
- Email Excel ingestion without leaking raw data to public storage.
- Mobile multi-GB upload directly to Drive without server binary relay.
- TKL admission only after verified upload; safe retry/duplicate handling.
- Expiration/cleanup of failed sessions and temporary data; correct tenant ownership.
- Relay fallback with equivalent authorization and audit.

## Follow-up
Coordinate TEAI contract and CNCF sandbox phase work; update mobile clients after API agreement. This journal does not claim that these flows are already implemented.
