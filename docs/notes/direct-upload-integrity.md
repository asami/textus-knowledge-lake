# Direct-upload content integrity

Status: planned; not accepted.

TKL-D01 freezes the expected digest and reservation/file binding. TKL-D02 implements authenticated preparation with server-selected folder, stored SHA-256/length, service-account native provider calls and independent verification before registration. TKL-D03 closes mismatch/unknown/expiry and connected acceptance. Missing checksum is not success; client-reported success or arbitrary file IDs cannot register resources. Existing Phase 1 / TKL-M03 remains the parent.

Canonical cross-project contract: [plan](../../../../Project2026/nict-editing-studio-app/docs/notes/direct-upload-integrity-plan.md).

SHA-256 here verifies transferred binary content only. Do not use it for update
tracking, management-state identity or unchanged-input certification. Preserve
existing runtime work and completed evidence; implement only the remaining delta.
