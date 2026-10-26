# Material Package v1 starts from the BoK architecture case

- Date: 2026-10-09
- Status: Working design

The simplemodeling.org BoK Knowledge Lake provides the first concrete case for a generic TKL Material Package.

The package specification is deliberately minimal. A directory with `material.json` is a Material Package; primary assets and derived artifacts may be grouped under `assets/` and `derived/`.

Material is explicitly separated from PreparedMaterial. Material represents meaningful source/working data as stored in the Lake. PreparedMaterial remains the preparation-result Entity used by candidate extraction. KAR remains a PreparedMaterial export/transport artifact.

History is provided by the dated journal rather than a new Material version mechanism. Revised Materials normally become new journal entries and may use lineage such as `supersedes` or `derivedFrom`.

The first conformance example is the Textus AI Operations Architecture package. Operational use of this case should drive additions or corrections to v1.

See `docs/notes/material-package-v1.md`.
