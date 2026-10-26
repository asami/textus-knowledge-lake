# Material Entity ID and TKL Material URN

Date: 2026-10-10
Decision update: adopt the TKL-scoped URN namespace alongside CNCF Entity IDs.
Status: design decision; implementation pending

## Distinct identifiers

Two different objects and identifiers must not be conflated.

1. **Material Entity ID**: a CNCF UniversalId-conformant identifier, issued by the originating application (including offline Editing Studio App). NICT Editing Studio creates or updates its Material Entity using the received ID without reissuing it. Its typed entity identity follows CNCF's `UniversalId -> EntityId -> MaterialId` model.
2. **TKL Material URN**: `urn:textus:klake:<lake>:material:<logical-path>` identifies a **Material stored in the Textus Knowledge Lake** (the Material Package). This is a TKL logical reference/locator, not the Material Entity ID, and need not conform to CNCF UniversalId's grammar.

A Material Entity can reference a stored TKL Material by its URN; neither identifier is derived from or substituted for the other. An application Material Entity may exist before its content is stored in TKL. The TKL URN is assigned/known when the TKL Material's logical location is established.

## Flow

- Editing Studio App creates Material and issues its CNCF Universal ID.
- When applicable, content is uploaded/stored as a TKL Material Package, addressed by its TKL URN.
- Editing Studio receives the originating Material Entity ID, creates or updates the corresponding Material Entity, and records the TKL Material URN separately as a reference to stored source assets.
- Repeated delivery of the same Entity ID must not create a second Material Entity. The TKL URN must not be used as the Entity's primary key.
- Information Candidate gets a distinct ID and records provenance to the originating Material Entity and/or TKL Material as appropriate.

## Existing TKL contract

Use `material.json.id` for the TKL Material URN under the revised Material Package contract; do not replace it with an Entity ID. The former `textus:material:<lake>:<logical-path>` is legacy and must not be used for newly created packages. If a producer's Material Entity ID needs to be carried with the package, introduce an explicitly named optional field such as `sourceMaterialEntityId` after confirming the serialization/API contract. Avoid ambiguous dual use of `id`.

The chosen URN namespace is `urn:textus:klake:<lake>:material:<logical-path>` (e.g. `urn:textus:klake:nict:material:inbox/book-001`). The `klake` namespace can also identify other TKL-managed resource kinds. This URN is used alongside, never instead of, the CNCF Material Entity ID. A TKL URN's logical-path semantics and any relocation behavior follow TKL's own rules. Do not silently reinterpret the URN as a location-independent Entity ID.

## Implementation checks

- Confirm CNCF ID issuance, serialization, typed IDs and collection semantics against the current implementation, including Phase 52 changes.
- Define the exact association field and ingestion DTO at the TKL/Editing Studio boundary.
- Preserve both identifiers unchanged through retries, background synchronization and provider adapters.
- No content hashing, integrity checks, or duplicate-ID mapping database solely for this integration.

## References

- CNCF: `docs/design/id.md`
- TKL: `docs/notes/material-package-v1.md`
