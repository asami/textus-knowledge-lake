# Phase 2: EAI-Mediated Material Ingestion

status=planned
planned_at=2026-11-02
owner=textus-knowledge-lake
checklist=inline TKL2-01..05 below

## Goal and baseline

Integrate EAI-managed mail and storage transfer with TKL's existing Material
identity, owner/tenant authorization, metadata, journal and admission. Deliver
mail Excel first, then adopt the EAI provider boundary for mobile direct upload.

Source: [EAI ingestion proposal](https://github.com/asami/textus-knowledge-lake/blob/7da761b5bab59ae654fd833acbb652616d3da54a/docs/journal/2026/2026-11-02-eai-ingestion-development-proposal.md)
and [integration direction](https://github.com/asami/textus-knowledge-lake/blob/7da761b5bab59ae654fd833acbb652616d3da54a/docs/journal/2026/2026-11-02-knowledge-hub-eai-material-ingestion.md).

This is a successor integration delivery. Preserve [Phase 1](phase-1.md),
[TKL-M03](material-access-and-processing-plan.md) and
[TKL-D01..D03](tkl-managed-mobile-material-delivery-plan.md) scopes, acceptance
and histories. Their checked/unchecked states are not rewritten here. Do not
make the current mobile route wait for EAI. Adopt the new boundary only once
the relevant producer contracts and migration/regression proof exist; all of
OpenClaw processing is not a prerequisite.

## Responsibility and contracts to align

TKL owns Material semantics, client-issued Material IDs, owner authorization,
logical destination policy, authoritative registration state and admission.
EAI owns provider credentials/API calls, physical destination/session mechanics,
private staging, delivery evidence and temporary/orphan cleanup. CNCF owns
external execution isolation and lifecycle. Preserve TKL's logical-to-provider
correspondence through explicit EAI references; never move logical identity into
Drive file paths or give clients operator storage configuration responsibilities.

Align these existing authorities before coding:

- [Material reference v1](../spec/material-reference-v1.md)
- [Registration Operations v1](../spec/material-registration-operations-v1.md)
- [Mobile Material delivery](../spec/tkl-managed-mobile-material-delivery.md)
- [Existing direct-upload integrity plan](https://github.com/KnowledgeHubProject/nict-editing-studio-app/blob/main/docs/notes/direct-upload-integrity-plan.md)

TKL2-01 specifies the EAI prepare/status/finalize/registration handoff using
actual public types and existing reservations. Do not invent a second TKL
registration ledger or treat EAI transport state as Material admission. Freeze
the relationship between provider transfer, TKL journal and cleanup ownership.

## Connected order

| Step | Delivery | Producer prerequisite |
| --- | --- | --- |
| TKL2-01 | Contract and current-state inventory; identify reused M03/D01..03 behavior and migration gaps | EAI2-01 and existing TKL public contracts |
| TKL2-02 | Mail original Excel -> one admitted Material -> Workbench original/provenance read | Joint delivery with EAI2-02 and the required private-artifact subset of EAI2-03; CNCF's accepted bounded sandbox route |
| TKL2-03 | Mobile control -> TKL -> EAI -> Drive, direct mobile bytes -> Drive, completion -> TKL | EAI2-04 and shared Core transfer contract |
| TKL2-04 | Restart/reconciliation, integrity, changed destinations and explicit relay fallback | EAI2-05; preserve original reservation bindings |
| TKL2-05 | Real-provider and Workbench acceptance, review and deployable handoff | Both joined routes and their owning evidence |

Contract work can proceed together; implementation of effects uses delivered
producer capabilities. Use a complete working route and its executable specs
first, then close failure/recovery branches. Do not wait for CNCF active/standby,
Component Repository distribution or all EAI providers.

TKL2-02 and EAI2-02 are one connected implementation/acceptance slice: neither
requires the other's entire Phase to close before integration. Shared evidence
is attributed to the actual producer/consumer boundary rather than copied into
conflicting completion ledgers.

## Stage: EAI ingestion delivery

Stage Status:
- Current status: OPEN
- Owner: TKL
- Update rule: Close only from TKL2-01..05 and KI-01..07 evidence; this plan itself accepts no implementation.

- [ ] TKL2-01: Promote the handoff/migration contract into design/spec and map
  each M03/D01..03 obligation to reused behavior or a specific migration gap.
  Define provider evidence, unknown states and cleanup ownership without duplicate ledgers.
- [ ] TKL2-02: Mail Excel registration preserves original content, trusted owner
  mapping and provenance; EAI delivery verification precedes Material admission.
- [ ] TKL2-03: Real Shared Drive direct upload preserves client-issued Material
  IDs, reserved file identity, stored expected size/SHA-256 and private placement.
  Android/iPhone obtain preparation through the common Core/Framework route;
  no provider-specific configuration or server credentials move into the app.
- [ ] TKL2-04: Wrong file/owner/size/digest, missing required checksum, partial or
  unknown completion, repeat finalization, expiry, destination change and orphans
  have tested outcomes. TKL relay fallback preserves equivalent authorization,
  reservation, integrity and audit; no silent switch to another destination.
- [ ] TKL2-05: Required executable tests, real mail/Drive and mobile transfer
  evidence, Workbench original/provenance reads and integrated independent review
  pass. Record actual versions, supported destinations/platforms and limitations.

## Executable acceptance

| ID | Given / When | Then |
| --- | --- | --- |
| KI-01 | EAI presents verified private Excel delivery for a trusted mapped owner | Original workbook becomes one Material with message/attachment provenance; unauthenticated sender does not establish ownership |
| KI-02 | Android/iPhone prepares and uploads large media | Existing Material ID/reservation is retained; bytes flow directly to Drive and admission waits for independently verified provider evidence |
| KI-03 | Client reports success with arbitrary/wrong file ID, size, digest, tenant or placement | No Material admission; absent required checksum is not interpreted as success |
| KI-04 | Finalization response is lost, then retried after restart | Reconcile existing registration/transfer; one logical admission, no duplicate upload or original overwrite |
| KI-05 | Operator changes destination or an abandoned transfer is reclaimed | Existing reservations retain their destination binding; cleanup cannot delete admitted originals or another owner's objects |
| KI-06 | Explicit relay fallback or My Drive shared-folder configuration is selected | Same logical contract and ownership checks; Shared Drive failures never silently choose My Drive |
| KI-07 | Workbench opens mail/mobile Material with OpenClaw stopped | Original and available source/provenance evidence resolve; no Candidate/Information is silently approved |

Measure a real multi-GB direct upload with bounded buffering and evidence that
media bytes do not transit TKL/EAI. Keep costly/live checks separate from small
deterministic specifications; record actual Android/iPhone and destination
coverage rather than treating one platform fixture as both.

## Consumer and closure boundaries

[EAI Phase 2](https://github.com/asami/textus-enterprise-application-integration/blob/main/docs/phase/phase-2.md)
and [CNCF Phase 118](https://github.com/asami/goldenport-cncf/blob/main/docs/phase/phase-118.md)
own upstream implementation. [Workbench acceptance](https://github.com/asami/textus-knowledge-workbench/blob/main/docs/phase/eai-material-consumer-plan.md)
uses ordinary Material queries and preserves its human review gate. KnowledgeHub
Information admission and knowledge formation are not moved into TKL/EAI.

Handoff actual public API/configuration, producer versions, consumer evidence and
remaining limits. Reuse accepted baseline evidence only where applicable. Keep
required release checks and integrated review; no per-helper review cycle.
No bulk original migration/deletion, broad provider platform or forced client
cutover is authorized by this planning change.

## Retained security acceptance during EAI migration — 2026-11-02

TKL2-01/03/04/05 must retain the
[UC-01..04 delivery contract](../spec/tkl-managed-mobile-material-delivery.md#untrusted-client-admission-and-access--2026-11-02)
and the existing mobile workstream evidence.

- [ ] UC-01..04 migration regression: no provider/server secrets in clients;
  cross-user identity/reference substitution denies disclosure/mutation;
  forged or unverified completion cannot admit Material; authenticated users
  remain subject to distinct Operation and resource authorization through EAI
  and service-account execution.

Provider transfer reports are accepted only through the actual trusted EAI
contract and bound TKL reservation. A direct client claim cannot impersonate
that evidence. Reuse prior acceptance for unchanged boundaries and test migration
gaps; these conditions do not make EAI a prerequisite for App 6.10/7.

## Preserve standard business defaults — 2026-11-02

- [ ] SB-01..03 within TKL2-01/03/05: EAI migration preserves the
  [standard storage/policy contract](../spec/tkl-managed-mobile-material-delivery.md#standard-storage-and-configurable-policy--2026-11-02).
  Provider changes do not require clients to configure security guards or
  weaken private storage, owner policy or registration. Clearly separate
  supported strengthening from unavailable external equipment/services.

This qualifies migrated routes with existing evidence, not a new general
security-configuration implementation or offline-storage dependency.
