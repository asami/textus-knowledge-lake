# BoK Knowledge Lake: journal source and index projection

- Date: 2026-10-09
- Status: Design decision

A concrete BoK use case for simplemodeling.org clarified how Google Drive-backed Knowledge Lakes should preserve history and current logical structure.

## Decision

Use an append-oriented dated `journal/` tree as the semantic source of truth. A Material package is a journal directory containing `material.json` and its assets.

Use a mutable `index/` tree as the current logical projection. The index may present `spec`, `design`, `notes`, and `journal` views while referring to Materials physically stored in the dated journal.

The Knowledge Lake must be self-contained: its logical structure and lineage must be reconstructable from journal Materials and metadata without requiring GitHub.

GitHub remains a source/provenance system and the preferred home for version-controlled text/model artifacts, but it is not required to interpret the Lake.

Google Drive version history is accepted as an initial operational recovery mechanism for overwritten index files. It is not the BoK semantic history. A separate index snapshot mechanism is deferred until operational experience demonstrates a need.

## First use case

The initial simplemodeling.org Knowledge Lake will use this model for component-oriented BoK assets such as architecture reference diagrams and generated infographics that are unsuitable or unnecessary for Git version management.

See `docs/notes/google-workspace-backed-tkl.md`.
