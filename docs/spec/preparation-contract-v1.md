# Preparation bundle v1

`tkl.preparation-bundle.v1` is TKL's self-contained, provider-neutral canonical preparation snapshot. It contains immutable Resource, Evidence, Context, Run, PreparedMaterial and RawCandidateProposal values. It is a local export contract; external acceptance and equivalent provider guarantees remain unverified.

The package is `org.simplemodeling.textus.knowledgelake.model`. `PreparationModel.validate(bundle)` returns `Either[Vector[ModelError], PreparationBundle]`. Each error has `path`, `code`, and `message`. Constructors are structural; the public JSON and Markdown boundaries validate before returning successful output. Validation uses only supplied values. No provider, network, process, database or downstream service is invoked.

## Wire shape and fields

The root object has exactly these required keys, in canonical declaration order: `schema`, `resources`, `evidence`, `contexts`, `runs`, `materials`, `proposals`. `schema` is the literal `tkl.preparation-bundle.v1`; the other values are arrays. All nested fields below are required keys. An optional field is an explicit `null` when absent. Empty arrays are permitted except for material sections and section/proposal citations. Objects reject undeclared fields. JSON object order and whitespace do not establish identity; array order and string contents are preserved.

| Type | Exact fields in canonical order |
| --- | --- |
| VersionedRef | `id: string`, `revision: integer` |
| ProviderLocation | `provider: string`, `externalId: string`, `uri: string or null` |
| Provenance | `status: known or unknown`, `actor: string or null`, `description: string` |
| InputSnapshot | `id: string`, `retention: retained or reference-only`, `content: string or null`, `mediaType: string`, `provenance: Provenance` |
| Resource | `ref: VersionedRef`, `kind: string`, `title: string`, `location: ProviderLocation` |
| Evidence | `ref: VersionedRef`, `resourceRef: VersionedRef`, `providerRevision: string or null`, `snapshot: InputSnapshot` |
| ExternalContextBinding | `id: string`, `kind: string`, `provider: string`, `externalId: string` |
| ExistingInformation | `ref: VersionedRef`, `source: string`, `title: string`, `snapshot: InputSnapshot` |
| KnowledgeProcessingContext | `ref: VersionedRef`, `title: string`, `evidenceRefs: VersionedRef[]`, `bindings: ExternalContextBinding[]`, `existingInformation: ExistingInformation[]` |
| RunInput | `id: string`, `context: KnowledgeProcessingContext`, `resources: Resource[]`, `evidence: Evidence[]` |
| ProcessingFailure | `code: string`, `message: string`, `evidenceRef: VersionedRef or null` |
| ProcessingRun | `id: string`, `input: RunInput`, `processor: Provenance`, `status: completed or partial or failed`, `failures: ProcessingFailure[]` |
| MaterialSection | `title: string`, `text: string`, `evidenceRefs: VersionedRef[]` |
| PreparedMaterial | `ref: VersionedRef`, `producingRunId: string`, `title: string`, `summary: string`, `sections: MaterialSection[]` |
| RawCandidateProposal | `producerId: string`, `proposalId: string`, `producingRunId: string`, `materialRef: VersionedRef`, `title: string`, `summary: string`, `evidenceRefs: VersionedRef[]` |
| PreparationBundle | `resources: Resource[]`, `evidence: Evidence[]`, `contexts: KnowledgeProcessingContext[]`, `runs: ProcessingRun[]`, `materials: PreparedMaterial[]`, `proposals: RawCandidateProposal[]` |
| ModelError (API only) | `path: string`, `code: string`, `message: string` |

Scala uses the closed tags `ProvenanceStatus.Known/Unknown`, `Retention.Retained/ReferenceOnly` and `RunStatus.Completed/Partial/Failed`; their exact wire strings are the lower-case strings in the table. `VersionedRef.revision` is a positive Scala `Long`. IDs and required metadata/descriptors are nonblank. `content` and section `text` may be empty and preserve Unicode, punctuation and newlines exactly. Present optional metadata (`uri`, `providerRevision`, `actor`) must be nonblank. IDs remain opaque, including IDs such as `resource:資料/α`; they are never filesystem paths.

## Identity, retention and lineage

TKL entity IDs are separate from physical provider IDs, provider revisions, entity revisions, snapshot IDs, and Run IDs. Multiple versions of one entity ID are permitted; an exact `(id, revision)` may occur only once in its entity collection. A Run ID is unique and denotes immutable fixed input. Proposal identity is the tuple `(producerId, proposalId)`, allowing independent producers to use the same local proposal ID. No ID, revision, actor, source content or processor-visible input is silently generated or inferred.

Known provenance requires a nonblank actor. Unknown provenance requires `actor: null` and preserves its explanatory description. A retained snapshot requires `content` to be a string, including `""`. A reference-only snapshot requires `content: null`. Reference metadata, a provider URI, and known processor provenance do not prove content retention or prove which input an external processor saw. Unknown provider revisions stay `null`.

Top-level Evidence resolves an exact Resource revision. Current Context selections resolve exact top-level Evidence revisions; selections contain no duplicates. Binding IDs are unique within a Context, and versioned existing-information refs are unique within that Context. Bindings are metadata: a `drive-project` / `google-workspace` binding causes no automatic operation. Existing information is a versioned local KnowledgeHub-shaped baseline; its `source` is metadata and causes no synchronization.

A RunInput embeds its Context, selected Evidence, and exactly those Resource revisions selected by that Evidence. Embedded refs resolve internally. The Evidence set equals the embedded Context's selection set. Resource, Evidence, and baseline refs, metadata, retention and provenance are themselves validated. Snapshot IDs are unique across Evidence and existing-information inputs in each Run. A new current Context, resource version, title, external binding or baseline cannot mutate historical input; historical input does not need to resolve against the current top-level collections. Run input order is preserved.

A completed Run has no failures. Partial and failed Runs have at least one observable failure with a nonblank code/message. Optional failure Evidence citations resolve within that Run's input. Input retention is independent of processor provenance. Failed Runs cannot produce PreparedMaterial; a partial Run can produce material while retaining its observable failure descriptors.

Material versions refer to an existing producing Run and have at least one section. Every section cites at least one exact Evidence revision in that Run, without duplicate citations. A proposal refers to an existing material revision and the same producing Run, cites only Evidence in that Run, and covers every material citation. Proposal citations are nonempty and unique; valid extra citations from the producing Run are permitted. A proposal creates no receipt, downstream Candidate, approval or Admission.

[The draft-2020-12 schema](../../schemas/tkl/preparation-bundle-v1.schema.json) specifies required keys, types, positive revisions, closed enums, additional-property rejection and local retention/provenance/status conditions. Cross-record reference resolution, identity uniqueness, selection equality, snapshot collisions, producing-Run correspondence, failed-Run material prohibition and citation coverage are enforced by Scala validation beyond structural JSON Schema. The Scala decoder also enforces the `Long` range. Schema conformance alone does not establish complete validity.

## Encoding and projection

`PreparationJson.encode(bundle): Either[Vector[ModelError], String]` validates and deterministically emits fields in DTO declaration order with explicit nulls and stable array order. `PreparationJson.decode(text): Either[Vector[ModelError], PreparationBundle]` rejects malformed JSON, missing/extra keys, wrong types, unknown tags, unsupported format versions and invalid model/lineage values with structured errors. Wire errors use `json-syntax`, `json-decode` or `schema`; domain errors identify invalid paths using `blank`, `revision`, `duplicate`, `unresolved`, `retention`, `provenance`, `selection`, `status`, `failed-run`, `empty`, `lineage` or `coverage`. Encoding/decoding is lossless for semantic values, including exact text and nulls; original JSON indentation and object order are not preserved.

`PreparedMaterialMarkdown.render(bundle, materialRef): Either[Vector[ModelError], String]` validates and resolves the exact material revision. It projects the material title, summary and literal section text, material revision, producing Run/status, frozen Context revision, RunInput snapshot identity, processor provenance, bindings, existing-information baseline and citations. Each Evidence citation includes its revision, exact Resource revision, physical provider identity, provider revision known/unknown, input snapshot ID, retention and input provenance. Unknown provenance and reference-only inputs are visible; the projection does not claim analysis of unretained content. Metadata is escaped for Markdown/HTML and metadata newlines are escaped; material text remains literal. Missing material revisions produce a structured `unresolved` error. Markdown is a human projection and is not a lossless interchange format or a canonical identity representation.

## Local example and executable specifications

[The complete fixture](../../src/test/resources/tkl/preparation-bundle-v1.json) contains two revisions of `resource:資料/α`, Evidence revisions 1 and 2, retained text `日本語の資料 🌱` with newlines, reference-only Evidence with unknown actor/provider revision, a Drive Project binding and existing-information baseline revision 3. `run-local-001` embeds its complete fixed inputs. Its known processor provenance identifies the fixture operator and explicitly describes manually supplied data without live execution. `material:準備` revision 1 cites both inputs. The raw proposal uses `producerId: textus-knowledge-lake` and `proposalId: proposal-local-001`.

For example, reference-only metadata is represented as:

```json
{
  "id": "snapshot:資料/α-2",
  "retention": "reference-only",
  "content": null,
  "mediaType": "text/plain",
  "provenance": {
    "status": "unknown",
    "actor": null,
    "description": "The source actor and processor-visible content are unknown."
  }
}
```

`PreparationModelSpec` authors Given/When/Then scenarios for fixed history, empty retention, unknown metadata, invalid identities and lineage, observable partial/failed Runs and failed material prevention. `PreparationJsonSpec` authors fixture roundtrip, deterministic ordering, strict keys/tags/types/schema, malformed input, boundary validation and offline Markdown scenarios. These are executable specifications; this document does not claim that validation has run.

## Evolution and pending boundaries

V1 rejects additional fields at every DTO level. Future incompatible or additive fields require another explicit schema version and decoder; there is no automatic migration. Changes to an entity revision are not changes to the wire schema version. There is no management/content hashing.

Runtime store, conflict, retry and lifecycle behavior remain pending TKL-P1-02. Supported native CML and Cozy/CNCF integration remain pending the required native Step. This minimal domain build does not replace native integration. Provider I/O, transport, actual persistence, downstream receipts/Candidate lifecycle and Admission are outside this slice. Drive retention/concurrency, TKW acceptance, TEAI delivery, KnowledgeHub synchronization and other external guarantees remain proposed and unverified.
