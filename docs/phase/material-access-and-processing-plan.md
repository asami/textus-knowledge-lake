# Material access and OpenClaw processing delivery plan

status=planned
planned_at=2026-10-21
owner=textus-knowledge-lake
spec=../spec/material-access-and-processing.md

TKL-M03 includes the [mobile delivery workstream](tkl-managed-mobile-material-delivery-plan.md)
added on 2026-10-24: TKL-owned configuration, preparation and registration for
Android; Shared Drive first, with My Drive shared folders using the same contract.
Its D01-D03 checklist supplies the mobile acceptance portion of M03.

M01/M02 reference contract: [Material reference v1](../spec/material-reference-v1.md).
Semantic selectors, first-Material bootstrap and TKL-owned OAuth handoff are
specified there. Generated public bindings, executable implementation and
acceptance remain pending; the OPEN entries below are not closed by drafting it.

This is a Phase 1 workstream with its own delivery checklist. First deliver the
reference path consumed by Workbench; do not require all OpenClaw processing or
downstream KnowledgeHub admission before that handoff.

| ID | Delivery | Status |
| --- | --- | --- |
| TKL-M01 | Bind typed Material search/metadata/URN/access Operations, normal CNCF component composition, persistence and provider mappings. | OPEN |
| TKL-M02 | Implement one actual Drive-backed Material search -> metadata -> original-access route and hand it to Workbench. | OPEN |
| TKL-M03 | Move logical package registration, identity and placement out of Studio; reuse applicable provider code through the TKL boundary. | OPEN |
| TKL-P01 | Bind TKL registration and request/result/resumption to the existing OpenClaw continuation protocol; implement one processing route. | OPEN |
| TKL-P02 | Prove processing results are visible through M02's ordinary reference API, with shared identity/provenance and no worker requirement for reads. | OPEN |

M01/M02 precede the first real Studio/Workbench reference acceptance. M03 is
required before Studio's package output can be accepted on the new route.
P01/P02 may proceed independently after the common model is bound; they do not
gate M02. Do not implement a general multi-provider platform for this delivery.

## Checklist and validation

- [ ] Exact public Operations and shared Material model are specified; no
  placeholder CML or private-class call is treated as a released capability.
- [ ] One actual stored Material is searched/resolved/accessed via Workbench
  and displayed in Studio, including unavailable/unauthorized behavior.
- [ ] Reads work without OpenClaw and never launch preparation implicitly.
- [ ] Registration preserves existing originals and available provenance;
  same-reference ambiguity has a TKL-owned contract. No bulk corpus migration
  or destructive cleanup is implied by moving implementation responsibility.
- [ ] One OpenClaw continuation round trip publishes a result visible through
  the same ordinary TKL query boundary; unfinished work is not shown complete.
- [ ] Run relevant executable specifications and one connected independent
  review per delivered slice; share cross-project evidence at its actual scope.
  Existing required release checks remain applicable, not per-helper repeats.

Dependencies: [Workbench consumer plan](../../../textus-knowledge-workbench/docs/phase/knowledge-editing-integration-plan.md),
[Studio Phase 3](../../../../Project2026/nict-editing-studio/docs/phase/phase-3.md).
Missing generic continuation/provider capabilities are dependency requirements,
not permission to implement substitutes in TKL. This plan changes no source,
registration, live account, acceptance record or Goal state.
