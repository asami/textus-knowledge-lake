# Phase 3: Contextual Retrieval Integration

status=planned
planned_at=2026-11-25
owner=textus-knowledge-lake

## Goal
Provide authorized, versioned source access and durable derived-artifact integration for [SIE contextual retrieval](https://github.com/asami/semantic-integration-engine/blob/HEAD/docs/phase/contextual-retrieval-plan.md). Preserve Phase 1/2 scope and priorities. This plan does not accept implementation.

- [ ] TKL3-01: Inventory existing source, authorization, provenance and persistence contracts; agree SIE handoff and storage ownership before coding.
- [ ] TKL3-02: Provide bounded authorized source and relationship reads with stable source/version references; preserve original bytes and existing admission boundaries.
- [ ] TKL3-03: Establish durable references to generated context, embeddings and RDF projections; distinguish authoritative records from rebuildable indexes; record source dependencies, model/prompt versions and generation time.
- [ ] TKL3-04: Support idempotent registration, invalidation, reindexing and revocation; prevent cross-tenant leakage through derived artifacts or graph expansion.
- [ ] TKL3-05: Joint SIE acceptance with deterministic fixtures and measured retrieval evaluation; document cost, latency and limitations.

## Acceptance
Given an authorized source version, derived artifacts resolve to exact source spans and dependencies without changing originals. Given a changed or revoked dependency, stale artifacts cannot bypass current access policy. Given repeated processing, no duplicate logical artifact is created. Generated context never admits Candidate or Information. Workbench/KnowledgeHub retain their existing admission responsibilities. No new canonical RDF assertions arise solely from generated context.

## Dependencies
Coordinate contract work with SIE; do not require full Phase 2 closure for independent fixtures. Existing ingestion priorities remain unchanged.
