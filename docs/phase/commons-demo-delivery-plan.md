# TKL: Mail Material Registration and Reference Delivery

status=planned
execution_started=false
updated_at=2026-11-15
availability_date=2026-12-11

## Purpose and responsibility

TKL owns logical Material identity, original/resource correspondence, source evidence and authorized reads. EAI retrieves and transports mail; TKL registers it in the Knowledge Lake. Commons derives knowledge without changing originals.

The [responsibility/formation specification](../../../../Project2026/nict-knowledgehub-commons/docs/spec/knowledge-responsibilities-and-formation-routes.md) governs the shared boundaries.
The [Commons tracking register](../../../../Project2026/nict-knowledgehub-commons/docs/strategy/related-project-tracking.md) coordinates dependencies with Phase 1 RB-01..05 and D1-P01..07.
This is a bounded owner workstream. Existing numbered Phase states, accepted checklists and unrelated work remain unchanged.

## Existing contract and implementation boundary

Existing Material reference/registration contracts and the in-progress referenced-original changes are inputs to inventory. Staged code and an untracked plan are not accepted or released capabilities. The existing EAI ingestion Phase emphasizes Excel attachments; mail bodies/threads require an explicit extension.

Start from the [native contract or delivery ledger](phase-2.md). Additional Studio development/new integration remains on hold.
Studio and Heritage Curator direct editing is a lifecycle comparison, not a new application implementation dependency.

## Pre-release work

Table 1 maps deliverables to prerequisites and observable acceptance. Dates are coordination targets, not execution evidence.
CD-COM-01 accepts the combined binding inventory before effects; each owner's first item can be prepared in parallel.

| Task | Target | Dependencies | Deliverable | Acceptance |
| --- | --- | --- | --- | --- |
| CD-TKL-01 | 2026-11-20 | Existing producer contracts; no CD prerequisite | Bind mail-body/thread registration, identity, resources, provenance and source-version representation with EAI and Workbench. | Agree with EAI on a mixed-thread example and the correspondence between batch progress, registration intent and confirmed Material reference for CD-COM-01. Specify actual input/result/lookup contracts, same-intent behavior, original granularity and statement locators. Unknown version/span evidence stays unknown. Preserve current Material versus PreparedMaterial distinctions and existing registration consumers. |
| CD-TKL-02 | 2026-11-25 | CD-COM-01 | Implement the selected private mail Material registration and durable original mapping. | A mixed-thread input retains eligible originals, quotations and partial thread correspondence. Excluded sibling bodies are absent from durable resources. Registration acknowledges only actually saved original/resources and native registration state. Same-intent delivery maps to the same Material; changed content follows an explicit source-version/new-registration rule. No duplicate provider or registration ledger is added. |
| CD-TKL-03 | 2026-11-28 | CD-TKL-02; CD-EAI-03 | Expose the registered mail through ordinary authorized Material search/metadata/original reads and prove reopen. | Across two bounded deliveries, verify distinct eligible originals and stable references after same-intent retry. Workbench/Commons can read the same saved original and provenance after restart without EAI credentials or preparation. Unauthorized/unavailable/unknown-version results remain explicit; raw originals never become team Web assets. |
| CD-TKL-04 | 2026-12-08 | CD-TKL-03; CD-COM-02 | Verify source trace and original preservation across extraction, correction, retry and recovery. | Multiple Candidate occurrences resolve to actual Material/resource/source references. The original survives Commons/Workbench editing unchanged; pending/failed delivery and lost acknowledgements do not become successful Material reads. |

## Owner status ledger

Current status: OPEN. Execution has not started. All acceptance evidence: pending.
The following checkboxes are the authoritative task state for this workstream; the Commons register is a dated coordination snapshot.

- [ ] CD-TKL-01
- [ ] CD-TKL-02
- [ ] CD-TKL-03
- [ ] CD-TKL-04

Check an item only after its actual behavior, applicable executable checks, producer/consumer compatibility and independent review pass at the delivered scope.
Record the selected artifact/configuration boundary, validation result and evidence reference beside that item. Reuse valid owner evidence; retain native release gates.
Keep real originals, accounts, addresses, membership, source IDs, credentials, private properties and live logs outside Git. Tracked tests/examples use synthetic data.
Only sanitized summaries and non-sensitive evidence references belong in these documents. Private evidence references are logical locators, not secret URLs or actual source IDs.

## Joined-route acceptance and release boundary

The selected input is the operator's Gmail mail with designated NICT participants in From/To/Cc,
2026-01-01 inclusive to 2027-01-01 exclusive, under private configured timezone and finite budgets.
Credentials are store references in Git-excluded properties. The earlier ten-header inspection is a sample, not a batch limit or Material-ingestion result.
EAI uses TKL registration; Commons extracts/matches/groups knowledge from saved Material and presents Candidates through the common foundation/Workbench.
Human review is on the private operator route. The team Web is read-only and contains only reviewed authorized reference projections.
Originals and operator capabilities do not reach the browser. Native authentication and community Authority are checked at their bound operations.
Formal Information admission, publication selection and BoK adoption are separate; unconnected projections are labeled truthfully.

Targets are working integration by December 4, actual content/access/recovery verification by December 8,
and Commons Phase 1 completion by December 10 for team use on December 11. This plan starts no runtime, ingestion or native Session.
Report missing prerequisites to the owner and the Commons register with their impact; do not substitute a direct Commons-to-Gmail connector.

## After December 11

Additional providers/media, mobile migration, OpenClaw preparation and broader retention/cleanup follow their owning plans. The whole EAI/TKL migration is not required for the selected Gmail body/thread route.

The [later S1 backlog](../../../../Project2026/nict-knowledgehub-commons/docs/notes/s1-update-integration-backlog.md) retains unadopted update contracts.
Planning and partial integration do not close that backlog or entire related Phases.
