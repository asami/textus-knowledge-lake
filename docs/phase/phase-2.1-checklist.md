# Phase 2.1 — Manual Flow and Aggregate Acceptance Checklist

- Current status: Planned; implementation not started
- Defined: 2026-10-03; Split from Phase 2
- Authority: [Phase 2.1](phase-2.1.md); predecessor: [Phase 2](phase-2.md)
- Update rule: Retained original item IDs identify moved unfinished obligations; check only with actual accepted evidence.

## Manual preparation, proposal and complete-flow acceptance

- [ ] P2-C10 — P2-R06: A real manual session uses an identified external context binding and frozen input package; supplied inputs and confirmed processor-visible inputs are recorded separately.
- [ ] P2-C11 — P2-R06: Result import validates exact Run/input/citation/provenance relationships; identical repeat and conflicting repeat outcomes are verified without rewriting finalized Runs or duplicating accepted material.
- [ ] P2-C12 — P2-R07/R08: Native TKL exports stored bundle JSON, Markdown and a producer-scoped v1 proposal with traceable lineage; operation and error behavior are documented, and downstream guarantees remain explicitly unverified.
- [ ] P2-C13 — P2-R09: Phase 1 regression and native tests plus new behavioral adapter tests pass without requiring live credentials in the ordinary test suite.
- [ ] P2-C14 — P2-R09: Sanitized live evidence covers Drive read/write, fresh-process recovery, source-change retention, declared competing-update behavior and a complete manual processing/import/export cycle; simulated faults are labeled separately.
- [ ] P2-C15 — P2-R08/R10: English operator/specification/model documentation, the Phase 2 integration register and versioned closing proposals agree on actual behavior, exact versions, owner follow-ups and resume conditions.
- [ ] P2-C16 — P2-R09/R10: Independent review, required final validation and local acceptance/closure commits are recorded; all six completion criteria are satisfied with no unpersisted accepted follow-up records.

P2-C13 requires the aggregate `sbt --batch test` once for both members; the complete Phase 1 regressions and new adapter suites remain required. P2-C14 combines accepted predecessor storage receipts with this Phase's actual manual cycle. P2-C15 covers the complete original split scope; P2-C16 closes the original six criteria mapped below. These uses consume predecessor evidence rather than owning its unfinished storage items again.

## Completion mapping and evidence

| Phase 2.1 completion criterion | Owned evidence |
| --- | --- |
| 1. Manual material/proposal lineage | P2-C10, P2-C11, P2-C12 |
| 2. Local/full and complete live acceptance | P2-C13, P2-C14 |
| 3. Complete operator/native documentation | P2-C15 |
| 4. Independent full-flow closure and proposals | P2-C16 |

Original pre-split criteria 1 and 2 are owned by Phase 2; original criteria 3..6 are finalized here. All six must be evidenced at final aggregate closure. No implementation/live/full-suite evidence is established by this split. Verify the accepted predecessor release/handoff and ancestry, retain its focused assurance, then bind the aggregate suite to the final frozen release tree.
