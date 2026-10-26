# TKL mobile Material delivery workstream

status=planned
planned_at=2026-10-24
parent=Phase 1 / TKL-M03
spec=../spec/tkl-managed-mobile-material-delivery.md

Extend M03 registration/placement to mobile intake. Do not wait for all OpenClaw
processing or Candidate/admission work. This plan records no implementation or
acceptance and leaves existing history intact.

| ID | Connected delivery | Status |
| --- | --- | --- |
| TKL-D01 | Bind typed destination discovery, delivery preparation, status/reconciliation and finalization to public CNCF contracts; select provider authorization with Core. | OPEN |
| TKL-D02 | Implement one operator-configured Shared Drive route covering Material metadata and originals, identity and registration. Supply the same contract for My Drive shared folders. | OPEN |
| TKL-D03 | Integrate Core/TFAF/App, prove restart and destination-change behavior, and expose the resulting Material through the ordinary query API. | OPEN |

## Acceptance checklist

- [ ] Configuration and Google storage policy exist only on the TKL/operator side.
- [ ] Android obtains a usable destination without entering drive/folder IDs.
- [ ] A real Shared Drive delivery includes metadata/resources and a TKL receipt;
  physical upload alone is not reported as registration.
- [ ] Both destination kinds have contract/adapter tests, including access denied;
  any missing live My Drive proof is explicitly identified.
- [ ] Interrupted finalization/restart and changed configuration preserve the
  original delivery binding; no duplicate registration or silent fallback.
- [ ] Workbench resolves the registered Material; ordinary reads need no OpenClaw.
- [ ] Relevant executable specifications and an integrated independent review
  pass at their actual scope before acceptance. No per-helper review loop.

Sequence: D01 contract -> D02 plus Core transfer -> TFAF/App composition -> D03
connected acceptance. Implement and test the first end-to-end route as one
coherent slice, then close its failure/recovery branches.

Consumers: [Core](../../../textus-flutter-core/docs/phase/tkl-managed-material-delivery-plan.md),
[TFAF](../../../textus-flutter-application-framework/docs/phase/tkl-managed-material-delivery-plan.md),
[App Phase 6.3](../../../../Project2026/nict-editing-studio-app/docs/phase/phase-6.3.md).


## Direct new-file upload and SHA-256 verification — planned delta

Contract and connected acceptance: [direct-upload integrity plan](../../../../Project2026/nict-editing-studio-app/docs/notes/direct-upload-integrity-plan.md).

TKL-D01 freezes the expected digest and reservation/file binding. TKL-D02 implements authenticated preparation with server-selected folder, stored SHA-256/length, service-account native provider calls and independent verification before registration. TKL-D03 closes mismatch/unknown/expiry and connected acceptance. Missing checksum is not success; client-reported success or arbitrary file IDs cannot register resources. Existing Phase 1 / TKL-M03 remains the parent.

- [ ] Implement the owning DU items and affected contract/behavior specifications.
- [ ] Hand off actual evidence and limitations; this planning addition claims no implementation, live upload or Phase closure.
