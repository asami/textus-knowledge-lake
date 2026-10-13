# Phase 2 — Drive Backend Acceptance Checklist

- Current status: Planned; implementation not started
- Defined and split: 2026-10-03
- Authority: [Phase 2](phase-2.md); successor: [Phase 2.1](phase-2.1.md)
- Update rule: Check only with actual evidence; original manual/full-flow items moved to Phase 2.1 exactly once.

## Capability, resources and persistence

- [ ] P2-C01 — P2-R01/R08: The test account, source folder boundary, storage root, authentication facility, permissions, supported formats and limits are explicitly selected and documented.
- [ ] P2-C02 — P2-R02/R04/R06: Actual read/export/write capabilities, the writer model and concrete Drive Project manual workflow are verified; required capability gaps are resolved before dependent implementation.
- [ ] P2-C03 — P2-R03/R04/R05: Versioned adapter contracts specify record granularity, canonical authority, publication/read consistency, version conflicts, durable operation identity and recovery without silently changing v1 exports or local CAS guarantees.
- [ ] P2-C04 — P2-R01: Bounded enumeration, pagination and shortcut handling preserve stable Resource identity across rediscovery and supported rename/move scenarios; unsupported/inaccessible items are observable.
- [ ] P2-C05 — P2-R02: A real Google Docs document and UTF-8 text file are acquired with explicit representation/provenance, preserved Japanese text/newlines, and correct retained/reference-only semantics.
- [ ] P2-C06 — P2-R02/R03: Historical Run inputs and retained content remain available after a test source or current Context changes; no provider reference is reported as proof of retention.
- [ ] P2-C07 — P2-R03: Canonical records and derived bundle/Markdown output are recovered from Drive in a fresh process without the previous local cache; malformed or incomplete records fail explicitly.
- [ ] P2-C08 — P2-R04: Competing writes based on the same accepted version cannot silently lose an update under the declared writer model; actual publication and coordination limitations are recorded.
- [ ] P2-C09 — P2-R05: Controlled failures and available live observations verify nonapplication/application/unknown outcomes, restart reconciliation, duplicate/orphan detection and projection failure after canonical persistence.

## First-unit acceptance and handoff

- [ ] P2-C17 — Focused storage behavior, affected Phase 1/native regressions and real storage acceptance pass; controlled faults and observed provider guarantees are labeled separately.
- [ ] P2-C18 — English storage/operator/adapter documents and the versioned manual context/package/import handoff are complete; storage integration proposals name actual versions and external-owner follow-ups.
- [ ] P2-C19 — Independent review, Step acceptance, closure ledger and local release are recorded; complete validation is explicitly deferred to Phase 2.1 with focused assurance and a pending aggregate gate.

## Completion mapping and evidence

| Phase 2 completion criterion | Owned evidence |
| --- | --- |
| 1. Real source identity, retention and provenance | P2-C01, P2-C02, P2-C04, P2-C05, P2-C06 |
| 2. Canonical storage, publication and recovery | P2-C03, P2-C07, P2-C08, P2-C09 |
| 3. Focused/local and live storage behavior | P2-C17 |
| 4. Storage and manual-interface handoff | P2-C18 |
| 5. Independent focused-assurance closure | P2-C19 |

No implementation/live verification is established by this split. Original P2-C10..16 occur only in Phase 2.1; cite this Phase's accepted storage receipts there rather than reassigning completed work. Full validation owner is PHASE-2.1, sequence PHASE-2 -> PHASE-2.1, method sbt-full-suite.
