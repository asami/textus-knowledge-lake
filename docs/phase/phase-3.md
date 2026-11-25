# Phase 3: Contextual Retrieval Integration

status=planned
planned_at=2026-11-25
owner=textus-knowledge-lake
checklist=inline TKL3-01..05 below

## Goal
Provide authorized, versioned source access and durable derived-artifact integration for [SIE contextual retrieval](https://github.com/asami/semantic-integration-engine/blob/HEAD/docs/phase/contextual-retrieval-plan.md). Preserve Phase 1/2 scope and priorities. This plan does not accept implementation.


## Acceptance
Given an authorized source version, derived artifacts resolve to exact source spans and dependencies without changing originals. Given a changed or revoked dependency, stale artifacts cannot bypass current access policy. Given repeated processing, no duplicate logical artifact is created. Generated context never admits Candidate or Information. Workbench/KnowledgeHub retain their existing admission responsibilities. No new canonical RDF assertions arise solely from generated context.

## Dependencies
Coordinate contract work with SIE; do not require full Phase 2 closure for independent fixtures. Existing ingestion priorities remain unchanged.

## Stage: Contextual retrieval delivery

Stage Status:
- Current status: OPEN
- Owner: textus-knowledge-lake
- Update rule: Close only from checklist and executable acceptance evidence; this plan itself accepts no implementation.

- [ ] TKL3-01: Inventory existing source, authorization, provenance and persistence contracts; agree SIE handoff and storage ownership before coding.
- [ ] TKL3-02: Provide bounded authorized source and relationship reads with stable source/version references; preserve original bytes and existing admission boundaries.
- [ ] TKL3-03: Establish durable references to generated context, embeddings and RDF projections; distinguish authoritative records from rebuildable indexes; record source dependencies, model/prompt versions and generation time.
- [ ] TKL3-04: Support idempotent registration, invalidation, reindexing and revocation; prevent cross-tenant leakage through derived artifacts or graph expansion.
- [ ] TKL3-05: Joint SIE acceptance with deterministic fixtures and measured retrieval evaluation; document cost, latency and limitations.

## Executable acceptance

| ID | Given / When / Then acceptance |
| --- | --- |
| CR-01 | Authorized source read resolves exact version/span; originals unchanged. |
| CR-02 | Wrong owner or revoked source denies access, including derived text and graph traversal. |
| CR-03 | Repeated registration or restart produces one logical artifact; changed dependencies invalidate stale indexes. |
| CR-04 | Generated context cannot approve Candidate/Information or create canonical assertions. |
| CR-05 | Joint fixture records retrieval metrics, cost and actual backend versions. |

## Completion criteria

All checklist obligations and applicable executable specifications pass. Record actual producer/consumer versions, deterministic fixture results, representative evaluation, limitations and integrated review. Promote agreed contracts to spec/design before implementation. No priority change or implementation acceptance is implied.
