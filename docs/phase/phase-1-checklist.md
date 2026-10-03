# Phase 1 — Project-local Acceptance Checklist

- Current status: Closed on 2026-10-03
- Owner: Textus Knowledge Lake
- Update rule: Preserve all 12 original acceptance items. Retain checked items only while the actual accepted commits, independent review and final validation support them.

Authority: [Phase 1](phase-1.md). All items below concern TKL-local delivery; all twelve original items have recorded local acceptance evidence.

- [x] Provider-neutral Resource / Evidence / Context / Run / PreparedMaterial / Proposal contracts and versioned JSON are specified.
- [x] Representative local fixture flow produces persisted PreparedMaterial and an evidence-grounded RawCandidateProposal.
- [x] JSON round-trip and Markdown projection are verified without external source access.
- [x] Stable identities, distinct revisions, immutable historical inputs and retention/unknown-provenance semantics are verified.
- [x] Local persistence update conflicts and partial failures have observable, tested outcomes; Drive guarantees remain explicitly unverified.
- [x] TKL proposal identity, result correlation, rejection/conflict and timeout/retry handling are verified against a local boundary fixture.
- [x] Supported native CML models the TKL-owned Use Case / Workflow; missing shared capabilities are recorded without local substitutes.
- [x] A repository-owned validation command runs without another source checkout, running external applications or live credentials.
- [x] README / notes / CML / journal agree on ownership, scope and actual assurance.
- [x] Independent review and required local validation evidence are recorded.
- [x] Integration register statuses and unresolved external guarantees are reconciled.
- [x] Closing proposals include owners, exact TKL contract/source versions, sample payloads, acceptance examples, open questions and follow-up work.

Actual TKW receipt/Admission, TEAI delivery, KnowledgeHub changes, BoK publication and live Workspace verification are external follow-up proposals, not checklist completion prerequisites.

## Recorded completion basis

Items are numbered by their original order above; their scope is unchanged.

| Items | Closure basis |
| --- | --- |
| 1, 3, 4 | Accepted Step 1 `2511186de891e3eb40863bd14d898eca8701ef47`: [canonical preparation/JSON contract](../spec/preparation-contract-v1.md); accepted Step 2 preserves fixed-input history and provenance. |
| 2, 5, 6 | Accepted Step 2 `5778cfcad83f5a8353d91b1a29f1bf4adbb431f4`: [local-flow contract](../spec/local-preparation-flow-v1.md), 172-test accumulator and actual local CLI; [native 8/8 and CLI evidence](../journal/2026/10/2026-10-02-phase-1-integration-proposals-v1.md#observed-local-evidence) corroborates persisted revision 7 and unknown/confirmed handling. |
| 7, 8 | [Native spec](../spec/native-application-model-v1.md) and [portable runtime command](../spec/native-application-model-v1.md#local-development-startup): actual parser/compiler/fidelity, released preparation, eight native cases and representative CLI passed without other checkouts/services/live credentials. Dedicated WORKFLOW remains unverified and recorded under TKL-INT-06. |
| 9 | Static documentation basis: README, translated application note, unchanged CML, native spec and public package describe the same local fixture scope, ownership and assurance; no external acceptance is asserted. Parent static link, scope, original-item and binding verification passed. |
| 11 | Static reconciliation basis: [register](../notes/phase-1-integration-register.md) and package /1 provide all seven stable IDs, exact versions, real local evidence, external limits, open questions, owner next work and resume conditions. Parent static link, scope, original-item and binding verification passed. |
| 10, 12 | Complete: three accepted Step commits; sole independent full Phase review PASS, zero blockers; final `sbt --batch test` 251/251 in six suites; accepted native commit/tree and seven versioned owner proposals bound in the public package. |

See [release binding](../journal/2026/10/2026-10-02-phase-1-integration-proposals-v1.md#release-binding-and-remaining-acceptance). The final full suite and complete independent review supply Phase acceptance; earlier focused evidence remains historical.

## Phase 1 accepted source and final evidence

Accepted native Step 3: `06c144b91111c408a0d0699178bffad66a7252fd`; source tree: `a3d54dd9cd55b96c640cfc00ab2d64d68c6d6bfc`. Independent full Phase review `TKL-P1-full-review-result/1` passed with zero blockers. Final `sbt --batch test` (`tkl-p1-final-full-suite-1`) passed all 251 tests in six suites, with zero failed/canceled/ignored/pending tests, zero aborted suites, exit 0 and released serial lock. The eight native scenarios and retained CLI revision 7 establish local fixture behavior. Closure reference: `tkl-phase-1-closure@1`; the distinct local release commit records this binding in its Git message. External guarantees remain unverified.
