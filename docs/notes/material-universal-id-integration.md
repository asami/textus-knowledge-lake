# Material identity across TKL and NICT Editing Studio

Date: 2026-10-10
Status: design decision; implementation pending

## Decision

Material created by any producer (including the offline Editing Studio App) receives a CNCF UniversalId-conformant identifier at creation. The ID is preserved in the TKL Material Package `material.json.id`, across transport, and when NICT Editing Studio creates or updates its Material Entity. Editing Studio MUST NOT reissue a new ID on ingestion. A repeat submission with the same Material ID addresses the same Material Entity, subject to normal update semantics.

Use the CNCF typed-identity hierarchy `UniversalId -> abstract EntityId -> concrete MaterialId` for the receiving Material Entity. Concrete serialization, namespace and collection ownership must follow the current CNCF ID implementation and its Phase 52 evolution, rather than inventing a second grammar.

The existing TKL v1 `textus:material:<lake>:<logical-path>` string is a *logical locator*, not a CNCF UniversalId. It MUST NOT be passed off as a conformant entity identifier. Keep it as a separate locator/resolution field during transition. Material identity must remain stable if the package moves. Provider IDs, folder paths and business keys do not generate entity IDs.

## Lifecycle

1. Producer issues Material ID once, including offline.
2. TKL stores the ID and associates it with its logical package locator and assets.
3. Editing Studio receives the ID and creates or updates Material Entity with that ID.
4. Candidate is a distinct entity with its own ID and a reference to source Material ID.
5. PreparedMaterial remains a distinct TKL entity and retains provenance references.

## Integration contract

- Preserve ID exactly through upload, retries, background synchronization and transport adapters.
- A duplicate delivery does not create a second Material Entity.
- Treat conflicting content/updates using explicit application update policy; do not add content hashes or bespoke integrity machinery.
- Resolve original assets through TKL rather than embedding provider-specific IDs in entity identity.
- Verify the current CNCF UniversalId issuance/parse/serialize APIs and collection semantics before coding; do not hardcode an illustrative ID format.
- Existing packages with path-derived IDs require an explicit transition decision, not silent reinterpretation.

## Related sources

- CNCF: `docs/design/id.md` (typed identity, namespace, collection, Phase 52).
- TKL: `docs/notes/material-package-v1.md` (existing path-based Material locator).
