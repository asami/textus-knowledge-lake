# Phase 1 — Project-local Acceptance Checklist

- Current status: Items 1–8, 9 and 11 have the local/static basis below; items 10 and 12 remain pending. Partial completion does not close Phase 1.
- Owner: Textus Knowledge Lake
- Update rule: Preserve all 12 original acceptance items. Check remaining items only after actual full review/final validation and accepted native commit/tree binding at release.

Authority: [Phase 1](phase-1.md). All items below concern TKL-local delivery; unchecked items are pending and are not evidence of implementation.

- [x] Provider-neutral Resource / Evidence / Context / Run / PreparedMaterial / Proposal contracts and versioned JSON are specified.
- [x] Representative local fixture flow produces persisted PreparedMaterial and an evidence-grounded RawCandidateProposal.
- [x] JSON round-trip and Markdown projection are verified without external source access.
- [x] Stable identities, distinct revisions, immutable historical inputs and retention/unknown-provenance semantics are verified.
- [x] Local persistence update conflicts and partial failures have observable, tested outcomes; Drive guarantees remain explicitly unverified.
- [x] TKL proposal identity, result correlation, rejection/conflict and timeout/retry handling are verified against a local boundary fixture.
- [x] Supported native CML models the TKL-owned Use Case / Workflow; missing shared capabilities are recorded without local substitutes.
- [x] A repository-owned validation command runs without another source checkout, running external applications or live credentials.
- [x] README / notes / CML / journal agree on ownership, scope and actual assurance.
- [ ] Independent review and required local validation evidence are recorded.
- [x] Integration register statuses and unresolved external guarantees are reconciled.
- [ ] Closing proposals include owners, exact TKL contract/source versions, sample payloads, acceptance examples, open questions and follow-up work.

Actual TKW receipt/Admission, TEAI delivery, KnowledgeHub changes, BoK publication and live Workspace verification are external follow-up proposals, not checklist completion prerequisites.

## Recorded completion basis

Items are numbered by their original order above; their scope is unchanged.

| Items | Closure basis |
| --- | --- |
| 1, 3, 4 | Accepted Step 1 `2511186de891e3eb40863bd14d898eca8701ef47`: [canonical preparation/JSON contract](../spec/preparation-contract-v1.md); accepted Step 2 preserves fixed-input history and provenance. |
| 2, 5, 6 | Accepted Step 2 `5778cfcad83f5a8353d91b1a29f1bf4adbb431f4`: [local-flow contract](../spec/local-preparation-flow-v1.md), 172-test accumulator and actual local CLI; [native 8/8 and CLI evidence](../journal/2026/10/2026-10-02-phase-1-integration-proposals-v1.md#observed-local-evidence) corroborates persisted revision 7 and unknown/confirmed handling. |
| 7, 8 | [Native spec](../spec/native-application-model-v1.md) and [portable runtime command](../spec/native-application-model-v1.md#local-development-startup): actual parser/compiler/fidelity, released preparation, eight native cases and representative CLI passed without other checkouts/services/live credentials. Dedicated WORKFLOW remains unverified and recorded under TKL-INT-06. |
| 9 | Static documentation basis: README, translated application note, unchanged CML, native spec and public package describe the same local fixture scope, ownership and assurance; no external acceptance is asserted. Parent static verification remains required. |
| 11 | Static reconciliation basis: [register](../notes/phase-1-integration-register.md) and package /1 provide all seven stable IDs, exact versions, real local evidence, external limits, open questions, owner next work and resume conditions. Parent static verification remains required. |
| 10, 12 | Unchecked: complete Step validation/independent review/native acceptance commit and final Phase full review/full validation/release binding are pending. Package content is authored, but its actual accepted native commit/tree and final evidence must still be supplied at release. |

See [release binding](../journal/2026/10/2026-10-02-phase-1-integration-proposals-v1.md#release-binding-and-remaining-acceptance). Earlier focused success cannot substitute for complete Step/Phase acceptance; this partial checklist is not Phase closure.
