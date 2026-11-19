# Referenced-original Material integrity

Status: implemented contract; runtime qualification and consumer acceptance are pending.

An admitted local source is read through the CNCF observed-file operation using
an explicit finite byte limit. A baseline is derived from the complete bytes
actually returned, including for empty content; a caller-supplied digest is not
a baseline. A later complete read is compared with that immutable SHA-256
baseline. A mismatch is changed and does not replace the baseline. Missing
bytes or a missing baseline cannot establish a match. A matching digest says
only that the obtained bytes match: it does not establish source incarnation,
read stability, approval, or readiness of earlier derived evidence.

The source version and stability are independent observations. Portable CNCF
reads may return Unknown even when path metadata is equal; Changed must remain
Changed. The opaque provider incarnation is not serializable, so reconstruction
cannot infer identity continuity from a saved path or equal metadata.

The source and destination workspace roots must be checked for separation
before destination writes. The check is point-in-time; each file operation still
performs its own authorization and binding checks. Original bytes are never
persisted in the destination. Native Entity registration, authorized source
binding, metadata persistence, public routing and reconstruction are required
before this specification can be claimed as delivered.

## Host configuration and operation input

Select `material.reference.mode=registered` and set component parameter
`material.reference.local.configuration` to the JSON below. Do not set the
copy registration configuration at the same time. The workspace execution
plan must independently grant source ReadOnly and destination Manage access to
the component. The finite maximum is per original and must cover the largest
selected file; the example accommodates the 36,931,060-byte catalog.

```json
{"schema":"textus.material-local-reference/1","id":"heritage-local",
 "allowedSubjects":["operator"],"lake":"heritage","logicalParent":"reports",
 "destinationWorkspace":"heritage-destination","maximumBytes":40000000,
 "bindings":[{"id":"report-414","workspace":"heritage-source","path":"report.pdf"},
             {"id":"catalog","workspace":"heritage-source","path":"catalog.pdf"}]}
```

Each contribution original has the ordinary `resourceId`, `role`, `mediaType`
and `byteLength`, and this exact source reference (using its own admitted
binding values):

```json
{"schema":"textus.material-source-binding/1","bindingId":"report-414",
 "workspace":"heritage-source","path":"report.pdf"}
```

Create each Material using the existing `createMaterialRegistration` operation
with a `textus.material-contribution/1` JSON contribution, retain its returned
`registrationId`, and invoke `materializeMaterial` with that ID. Inspect
`getMaterialRegistration` after an uncertain result; do not blindly replay.
The normal generated CLI exposes these operation names and arguments through
the owning host. Once registered, use `getMaterial` with the returned
`materialRef` and optional `evidenceResourceIds`, or `readOriginal` with the
`materialRef` and `resourceId`. `getMaterial` reports contentState and the
independent sourceIdentity and readStability observations. The binary route
returns matching bytes with corresponding `X-Material-*` headers from the
same read; unavailable and changed reads fail. It does not produce approval or
old-evidence readiness.

Materialization writes `<issued-location-leaf>.json` into the destination
workspace using CreateNew. It contains the contribution, logical reference and
reference metadata, including private workspace/path mappings; protect that
workspace accordingly. The native Entity document also retains the baseline
and exact binding, so a reconstructed component can reload it. If an
acknowledgement or an effect fails, the record may remain Materializing or the
JSON may already exist; do not infer success or automatically repeat effects.
