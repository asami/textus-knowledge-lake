# TKL–EAI Material ingestion integration plan

status=planned
planned_at=2026-11-02
owner=textus-knowledge-lake
parent=Phase 1 / TKL-M03, TKL-D01–D03
references=../journal/2026-11-02-knowledge-hub-eai-material-ingestion.md

## Scope
TKL remains the logical authority for Material ID, owner, metadata, journal, logical destination, registration and admission. EAI owns mail adapters, Drive provider mechanics, sandbox tools, private staging, transfer sessions, operational logs and cleanup. Preserve client-assigned Material IDs and the existing direct-to-Drive mobile upload as the primary **data plane**; the TKL relay is fallback. Do not change Workbench Candidate or KnowledgeHub Information admission authority.

| Item | Owner | Status | Deliverable / acceptance |
| --- | --- | --- | --- |
| TKL-E01 | TKL + EAI | OPEN | Freeze typed handoff for prepare/status/finalize, owner/Material ID, digest, length, reservation and trace correlation. Reconcile with existing TKL-D01 and direct-upload integrity contract. |
| TKL-E02 | EAI | OPEN | Drive adapter prepares restricted resumable upload and resolves physical placement from TKL logical policy; no Drive credential/folder selection in mobile or TKL domain logic. |
| TKL-E03 | TKL + EAI | OPEN | Direct mobile->Drive bytes, completion independently verified against reserved file/size/SHA-256 where supported; TKL alone finalizes registration. Preserve D02/D03 Shared Drive and My Drive scope. |
| TKL-E04 | TKL + EAI | OPEN | Recovery: retries, duplicate requests, unknown completion, expired sessions, orphan objects, destination configuration changes and relay fallback. |
| TKL-E05 | EAI + TKL | OPEN | Mail Excel intake: private attachment handling, provenance, owner resolution, original preservation and Material registration. |
| TKL-E06 | TKL + Workbench + mobile clients | OPEN | End-to-end acceptance: registered Material queryable without OpenClaw; unauthorized/mismatched resources rejected; no binary relay on primary path. |

## Sequencing and non-duplication
E01 aligns with existing D01; E02/E03 extend D02; E04/E06 extend D03. Do not close or replace existing D01–D03 checklists by writing this plan. E05 can progress independently after the common registration contract. Avoid implementing a second Drive provider in TKL; retain existing functionality until the EAI route has passed connected acceptance. Provider limitations for Google Drive upload sessions must be proven, not assumed equivalent to S3 signed URLs.

## CNCF dependency
Use CNCF Operation authorization, execution context and UoW contracts. EAI owns sandboxed external tool execution; TKL does not implement its own generic sandbox. Missing enforcement is tracked in the CNCF dependency plan.
