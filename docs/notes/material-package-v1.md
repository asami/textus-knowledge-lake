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
  "id": "textus:material:simplemodeling.org:components/textus-control-center/journal/2026/10/2026-10-09-ai-operations-architecture",
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


## Canonical logical identifiers

Material Package v1 uses logical-location identifiers. No global Material registry is required.

Knowledge Lake identity:

```text
textus:klake:<lake>
```

Material identity:

```text
textus:material:<lake>:<logical-path>
```

Example:

```text
textus:klake:simplemodeling.org

textus:material:simplemodeling.org:components/textus-control-center/journal/2026/10/2026-10-09-ai-operations-architecture
```

The logical path is relative to the Knowledge Lake root and is provider-neutral. It is not a Google Drive physical path or folder ID, even when the initial adapter maps it directly onto a Drive folder hierarchy.

Including namespaces such as `components/` and `shared/` in the logical path allows a Lake to grow without changing the identifier grammar.

For v1, Material identity intentionally follows logical location. A future location-independent identity layer may be introduced if real use cases require Material relocation while preserving identity. Such a future layer would resolve to the v1 Material locator rather than replacing the Knowledge Lake path model prematurely.


## Naming note: possible MAR terminology

The package/export acronym for Material is intentionally not fixed in v1.

Historically TKL has used KAR terminology around PreparedMaterial transport/export artifacts. With the current distinction between raw/working **Material** and processed **PreparedMaterial**, a Material-oriented archive/package name such as **MAR** may be semantically clearer because Material is source material rather than Knowledge itself.

This is an open naming question, not a decision:

- keep `Material Package` as the normative v1 term for now;
- do not rename KAR or introduce MAR into implementation contracts yet;
- when archive/export requirements for Material become concrete, compare KAR and MAR roles and decide whether they are separate formats or whether terminology should be revised.

Operational experience with the simplemodeling.org BoK Knowledge Lake should inform the decision.

## Route-independent Material references

The canonical `textus:material:<lake>:<logical-path>` identifier is also the application integration reference for Material.

A producer may store the same logical Material Package through different physical/integration routes, for example:

```text
authenticated client -> Google Workspace adapter -> Knowledge Lake

client -> TEAI -> OpenClaw -> TKL/Google Workspace -> Knowledge Lake
```

The route MUST NOT change the Material identifier or package semantics.

Application components should persist and exchange the logical Material URN rather than Google Drive IDs or URLs. When an application needs an asset, it resolves the Material URN through TKL and then accesses the required asset through the configured provider adapter.

For Editing Studio integration, large original media normally stays in the Knowledge Lake. The application server receives a Material URN and structured application data, and resolves originals only when processing requires them.

The concrete lake identifier for an NICT Knowledge Lake remains open; specifications should use `<lake>` until that naming decision is made.
