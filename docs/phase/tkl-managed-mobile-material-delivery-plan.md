# TKL mobile Material delivery workstream

Successor: [Phase 2 EAI ingestion](phase-2.md) (planned 2026-11-02) aligns the
EAI provider boundary with D01..D03. This is not an extra gate on the current
mobile workstream. Preserve existing reservation/integrity acceptance and
client-issued Material IDs; move provider mechanics only with a qualified handoff.

status=closed
completed_at=2026-10-27
planned_at=2026-10-24
parent=Phase 1 / TKL-M03
spec=../spec/tkl-managed-mobile-material-delivery.md

Historical introduction, 2026-10-24: extend M03 registration/placement to mobile intake. Do not wait for all OpenClaw
processing or Candidate/admission work. The bounded native implementation is now
in progress under App Phase 6.3; this plan claims no acceptance and leaves
existing history intact.

| ID | Connected delivery | Status |
| --- | --- | --- |
| TKL-D01 | Bind typed destination discovery, delivery preparation, status/reconciliation and finalization to public CNCF contracts; select provider authorization with Core. | CLOSED — accepted by current App Phase 6.3 completion; original source and evidence limits retained |
| TKL-D02 | Implement one operator-configured Shared Drive route covering Material metadata and originals, identity and registration. Supply the same contract for My Drive shared folders. | CLOSED — accepted by current App Phase 6.3 completion; original source and evidence limits retained |
| TKL-D03 | Integrate Core/TFAF/App, prove restart and destination-change behavior, and expose the resulting Material through the ordinary query API. | CLOSED — accepted by current App Phase 6.3 completion; original source and evidence limits retained |

## Current follow-on completion — 2026-10-27

This bounded App Phase 6.3 contribution is accepted at source `75227b5`
and closed by the App's current ordinary completion. Original criteria and DU
obligations below are satisfied at their stated source/controlled/live scopes.
The App's canonical completion/acceptance records own the connected disposition;
detailed execution evidence remains local, outside Git. Earlier source-authoring
and pending-gate prose below is dated history, superseded by this current result.
No unrelated owning Phase, canonical main checkout, public release or production
rollout is marked complete. Failed sessions and consumed execution grants remain
unchanged; no new device/Google action is implied.


## Acceptance checklist

- [x] Configuration and Google storage policy exist only on the TKL/operator side.
- [x] Android obtains a usable destination without entering drive/folder IDs.
- [x] A real Shared Drive delivery includes metadata/resources and a TKL receipt;
  physical upload alone is not reported as registration.
- [x] Both destination kinds have contract/adapter tests, including access denied;
  any missing live My Drive proof is explicitly identified.
- [x] Interrupted finalization/restart and changed configuration preserve the
  original delivery binding; no duplicate registration or silent fallback.
- [x] Workbench resolves the registered Material; ordinary reads need no OpenClaw.
- [x] Relevant executable specifications and an integrated independent review
  pass at their actual scope before acceptance. No per-helper review loop.

Sequence: D01 contract -> D02 plus Core transfer -> TFAF/App composition -> D03
connected acceptance. Implement and test the first end-to-end route as one
coherent slice, then close its failure/recovery branches.

Consumers: [Core](../../../textus-flutter-core/docs/phase/tkl-managed-material-delivery-plan.md),
[TFAF](../../../textus-flutter-application-framework/docs/phase/tkl-managed-material-delivery-plan.md),
[App Phase 6.3](../../../../Project2026/nict-editing-studio-app/docs/phase/phase-6.3.md).

## Direct new-file upload and SHA-256 verification — planned delta

Contract and connected acceptance: [direct-upload integrity plan](/Users/asami/src/Project2026/nict-editing-studio-app/docs/notes/direct-upload-integrity-plan.md).

TKL-D01 freezes the expected digest and reservation/file binding. TKL-D02 implements authenticated preparation with server-selected folder, stored SHA-256/length, service-account native provider calls and independent verification before registration. TKL-D03 closes mismatch/unknown/expiry and connected acceptance. Missing checksum is not success; client-reported success or arbitrary file IDs cannot register resources. Existing Phase 1 / TKL-M03 remains the parent.

- [x] Implement the owning DU items and affected contract/behavior specifications.
- [x] Hand off actual evidence and limitations; this planning addition claims no implementation, live upload or Phase closure.

Selected-checkout adoption: this section imports the canonical TKL planned delta
on 2026-10-25 under the literal App contract above. Existing progress/history
remains unchanged. CNCF's optional typed SHA-256 metadata foundation is in
progress and not accepted; full DU-01 remains open. This pass changes TKL
planning only. DU-02 authenticated immutable expected digest, retained file
binding and independent SHA-256 gate before registration remain pending.
DU-03 bounded Core streams/shared UI and DU-04 connected tests, integrated
independent review and finitely authorized live proof remain pending. Existing
metadata/length completion is reused but does not establish SHA-256 verification.

## EAI handoff (2026-11-02)

See [TKL–EAI ingestion development proposal](../journal/2026/2026-11-02-eai-ingestion-development-proposal.md). The proposal is now scheduled in [Phase 2](phase-2.md); existing Phase acceptance and closure remain unchanged.
## Explicit untrusted-client acceptance — 2026-11-02

TKL-D01..03 include the
[UC-01..04 delivery conditions](../spec/tkl-managed-mobile-material-delivery.md#untrusted-client-admission-and-access--2026-11-02).
Preserve existing evidence; close only the actual missing coverage.

- [ ] UC-01: destination/preparation/status responses contain no server keys or
  broadly privileged credential; bounded transfer capabilities stay out of logs.
- [ ] UC-02: cross-user owner/Material/original/reservation substitutions deny
  access or mutation; list/search and access-reference issuance reveal no
  private originals. Explicitly permitted sharing still succeeds.
- [ ] UC-03: no-upload success, wrong reserved contents and unavailable required
  provider proof cannot register Material; cross-user completion is refused.
  Positive verified upload and repeated finalization reconcile one registration.
- [ ] UC-04: valid login cannot invoke forbidden Operations or use an allowed
  Operation to exceed Material rights, even with server provider credentials.

Coordinate with App ESA610-05/06/08, ESA7-01/05 and Server ESS5-02/05.
CNCF owns generic enforcement, Core owns the shared client, and TKL owns these
reservation/admission decisions. This is integrated acceptance, not four new
review loops or a reopening of completed historical scopes.

## Small-business baseline acceptance — 2026-11-02

- [ ] SB-01..03 within TKL-D01..03: with ordinary provider provisioning, storage
  is private by default and UC admission/access works without mobile technical
  settings. Supported sharing choices match provider/link behavior; operator
  duties and optional external equipment are explicit.

Contract: [standard storage policy](../spec/tkl-managed-mobile-material-delivery.md#standard-storage-and-configurable-policy--2026-11-02).
Reuse current acceptance; no new offline-media/HSM prerequisite.

## Runtime-profile acceptance — 2026-11-02

Within TKL-D01..03, apply [profile isolation](../spec/tkl-managed-mobile-material-delivery.md#runtime-profile-isolation--2026-11-02).

- [ ] MODE-02..05: qualify real participant demo preparation/finalization and
  source access with distinct users and private destination; no client-selected
  mode, fake completion or provider credential widens access.
- [ ] Preserve account/destination bindings on retry and environment changes;
  synthetic provider/reset paths cannot touch real participant originals.

Reuse existing UC/SB/direct-upload evidence. This bounded November demo work
does not wait for EAI Phase 2 and does not reopen completed historical scopes.
