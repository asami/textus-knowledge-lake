# EAI ingestion Phase planning

Date: 2026-11-02
Status: planned; no implementation or acceptance

[Phase 2](../../../phase/phase-2.md) formalizes the November 2 proposal from
GitHub revision `7da761b5bab59ae654fd833acbb652616d3da54a`. It is a separate
successor delivery, preserving Phase 1, M03 and D01..D03 states and acceptance.
The migration explicitly maps those obligations rather than duplicating or
silently broadening the active mobile ledger.

First align the typed TKL/EAI contract, deliver private mail Excel as a Material,
and prove Workbench original/provenance access. Then adopt EAI-managed mobile
direct upload and complete reconciliation/relay acceptance. Client Material IDs,
file reservation and expected SHA-256 remain authoritative inputs; missing
required provider evidence cannot become upload success.

TKL retains identity, owner, journal and admission. EAI manages physical transfer,
provider credentials and temporary cleanup. Workbench still owns Candidate human
review, and KnowledgeHub's Information admission is unchanged. Existing originals
and successful mobile behavior must survive the provider-boundary change.

TKL2-01..05 remain OPEN. This planning task neither syncs unrelated GitHub changes
nor starts runtime execution, bulk migration, publication or a release.
