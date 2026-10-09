# Material Package v1

- Date: 2026-10-09
- Status: Initial working specification
- Purpose: Minimal package format for operating a Google Workspace-backed Knowledge Lake
- Evolution policy: Use concrete cases first; extend only when operational needs appear

## Position

Material is the meaningful source/working-data unit stored in a Knowledge Lake before knowledge-preparation processing.

```text
Material
  -> preparation / processing
  -> PreparedMaterial
  -> Raw Knowledge Candidate
  -> Textus Knowledge Workbench
```

Material and PreparedMaterial are distinct.

- **Material** is a package of source or working assets with enough metadata to understand the package inside the Knowledge Lake.
- **PreparedMaterial** is a TKL Entity produced by preparation/processing and suitable for downstream candidate extraction. It has the stronger entity/version/lifecycle semantics already defined by TKL.
- KAR remains a transport/export artifact for PreparedMaterial and is not the storage format of a Material package.

## Package rule

A directory containing `material.json` is a Material Package.

Recommended initial shape:

```text
YYYY-MM-DD-<material-name>/
├── material.json
├── assets/
│   ├── ...
│   └── ...
└── derived/
    └── ...
```

Only `material.json` is structurally mandatory. `assets/` and `derived/` are conventional directories and may be omitted when empty.

### assets

Contains the primary assets that constitute the Material: images, PDFs, SVGs, audio/video, documents, snapshots, exports, etc.

### derived

Contains artifacts derived from other assets in the same Material, for example an explanatory infographic generated from a semantic reference diagram.

A derived artifact must identify its source relationship in `material.json` when that relationship matters for interpretation.

## material.json v1

The v1 contract intentionally stays small.

Example:

```json
{
  "schema": "textus.material/1",
  "id": "textus-control-center:2026-10-09:ai-operations-architecture",
  "title": "Textus AI Operations Architecture",
  "createdAt": "2026-10-09T10:00:00+09:00",
  "component": "textus-control-center",
  "classifications": ["design", "architecture"],
  "assets": [
    {
      "path": "assets/textus-ai-operations-architecture.svg",
      "role": "reference-diagram",
      "mediaType": "image/svg+xml"
    },
    {
      "path": "derived/textus-ai-operations-architecture-infographic.png",
      "role": "infographic",
      "mediaType": "image/png",
      "derivedFrom": ["assets/textus-ai-operations-architecture.svg"]
    }
  ],
  "provenance": [
    {
      "type": "git",
      "repository": "asami/textus-control-center",
      "path": "docs/design/textus-ai-operations-architecture.svg"
    }
  ]
}
```

## Fields

### Required in v1

- `schema`: must be `textus.material/1`.
- `id`: stable identity of this Material package.
- `title`: human-readable title.
- `createdAt`: creation timestamp.
- `assets`: assets that belong to the package. May be empty for metadata-only cases.

### Recommended

- `component`: owning/primary component or subsystem.
- `classifications`: logical views such as `spec`, `design`, `notes`, `journal`, `architecture`, `reference`, `infographic`.
- `provenance`: source systems and source artifacts.
- asset `role` and `mediaType`.

### Optional lineage

Add only when needed:

- `derivedFrom`
- `supersedes`
- `relatedTo`

Do not introduce content hashes, integrity machinery, duplicate-read comparison, or independent version machinery merely to manage Material history.

## History and update rule

The dated Knowledge Lake journal is the semantic history. A materially revised package should normally be recorded as a new journal Material rather than overwriting an old package.

The package may use `supersedes` or `derivedFrom` to express lineage.

Material v1 therefore does not require an independent version number. If a future use case requires entity-style versions, define that requirement explicitly rather than duplicating PreparedMaterial semantics.

## Self-contained requirement

A Material Package must remain understandable from the Knowledge Lake itself.

External systems such as GitHub, Slack, Gmail, or websites may be recorded as provenance, but the logical classification, asset roles, and lineage necessary to understand the package must not depend on reading those systems.

## Index projection

The component `index/` is a rebuildable current projection over Material Packages.

The index may classify this Material under design, architecture, journal, etc. The package remains physically stored in the dated journal.

## v1 non-goals

Material Package v1 does not define:

- PreparedMaterial processing semantics;
- Candidate lifecycle;
- admission/review;
- a general archive/container format;
- content integrity/hashing;
- a full ontology for asset roles;
- provider-specific Drive IDs as semantic identity.

These can be added from real operational requirements.

## Conformance example

The first conformance case is the Textus AI Operations Architecture Material under the simplemodeling.org BoK Knowledge Lake.

This case should be used to refine the specification before broad implementation.
