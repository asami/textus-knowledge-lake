# Knowledge Lake / Knowledge Hub EAI integration — development proposal

Date: 2026-11-02
Status: proposal in journal, NOT an active Phase

## Scope
TKL is Material identity/owner/metadata/journal/registration/admission authority. EAI owns mail and Drive integration mechanics, private staging, tool execution orchestration, transfer session, physical placement, operational evidence and cleanup; generic sandbox execution belongs to CNCF. Workbench Candidate review and KnowledgeHub Information admission remain unchanged.

## Mobile route (retain existing architecture)
Control: Android/iPhone -> TKL -> EAI -> Google Drive session preparation.
Primary bytes: Android/iPhone -> Google Drive directly, not through TKL/EAI.
Completion: mobile -> TKL; TKL/EAI verify reserved file identity, size, SHA-256 where supported, owner and private placement before TKL admission. TKL relay is fallback, not primary. Preserve client-issued Material IDs and existing TKL-D01–D03 acceptance, including Shared Drive first and My Drive shared folders.

## Mail route
Mail -> EAI adapter -> private attachment processing -> TKL Material registration -> EAI-managed persistence. Preserve original Excel, provenance and trusted owner mapping. Treat attachment as untrusted; scanning/parsing in CNCF-managed sandbox. Do not assign Owner from unauthenticated sender claims.

## Proposed development slices
1. Align EAI-TKL typed prepare/status/finalize/registration handoff with existing M03 and D01–D03, Material reference and upload integrity contracts; avoid duplicate state ledgers.
2. Replace provider-specific implementation boundary incrementally without breaking current direct upload; prove real Drive resumable session semantics and credential restrictions.
3. Verify direct upload before admission; handle ambiguous completion, retries, wrong file/digest, expired sessions, orphan cleanup, destination changes and relay fallback.
4. Add mail Excel ingestion through the same Material registration contract, preserving original and provenance.
5. End-to-end acceptance: mobile large media without binary relay; mail Excel Material; Workbench resolves originals without OpenClaw; authorization and duplicate rejection.

## Phase migration
Map onto existing Phase 1 / TKL-M03 / D01–D03 after reviewing formal phase convention. Do not close, overwrite or broaden in-progress acceptance by adding this proposal. EAI and CNCF dependencies must be explicit before implementation. This journal supersedes the temporary phase-directory planning draft, not existing authoritative phase documents.
