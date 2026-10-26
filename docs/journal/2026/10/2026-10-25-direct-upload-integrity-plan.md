# Direct-upload integrity development plan

Status: planned; no implementation or live acceptance performed.

The user requested a plan for server-authorized new-file direct upload into an
allowed Drive folder, followed by independent content-hash verification. The
selected route uses backend service-account credentials and per-file resumable
URLs, never a general Google token on the phone.

TKL-D01 freezes the expected digest and reservation/file binding. TKL-D02 implements authenticated preparation with server-selected folder, stored SHA-256/length, service-account native provider calls and independent verification before registration. TKL-D03 closes mismatch/unknown/expiry and connected acceptance. Missing checksum is not success; client-reported success or arbitrary file IDs cannot register resources. Existing Phase 1 / TKL-M03 remains the parent.

[Contract, sequence and pending acceptance](../../../../../../Project2026/nict-editing-studio-app/docs/notes/direct-upload-integrity-plan.md).

Existing App Phase 6.3/direct-provider work is reused. Current tests and live
observations do not by themselves prove SHA-256 validation. No active worktree,
queue, credentials, deployment, live allowance, commit or release was changed
by this documentation update.
