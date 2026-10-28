# Material evidence observation v1

Owner: `org.simplemodeling.textus.KnowledgeLake`, Material service.
Status: implementation contract; validation and acceptance pending.

## Public contract

`getMaterial` accepts the existing exact `materialRef` and an additive
`evidenceResourceIds` collection. Omission or an empty collection retains the
existing metadata-only result, with no `observedEvidence` field and no original
read. A nonempty collection has at most 128 distinct logical resource IDs, each
nonempty, at most 256 characters and without controls. Selection order is
retained. The caller supplies neither a source version nor a physical reference.

After the selected mode's ordinary Material authorization, TKL resolves every
requested original before opening any original. A missing resource is
`resourceNotFound`; malformed selections are `argumentInvalid`. Bootstrap,
Registered and Packages remain separate, with their existing authorization and
failure disclosure. There is no fallback or implicit registration/materialization.

An explicit observation adds `observedEvidence`, one `MaterialEvidenceObservation`
per selected original, containing:

| Field | Meaning |
| --- | --- |
| `evidenceRef` | TKL-issued, opaque, resource-qualified logical evidence identity |
| `materialRef` | Exact requested logical Material identity |
| `resourceId` | Exact selected logical original identity |
| `revision` (optional) | Exact source version observed unchanged before and after the complete original read |
| `retrievalState` | `available` for a confirmed versioned read, otherwise `unverified` when source version is unavailable |
| `limitation` | Explicit limits of this observation; never a retention or historical-read promise |

The evidence identity is `urn:textus:klake:evidence:<opaque-incarnation>`.
The qualifier is lowercase SHA-256 of the UTF-8 compact JSON string array
`["textus.material-evidence-identity/1", materialRef, resourceId, storeName,
connectionId, resourceKey, parentKey]`, with every non-ASCII UTF16 code unit
escaped as lowercase `\uXXXX` before UTF-8 encoding, so even retained isolated
surrogates cannot alias through an encoder's replacement character. This producer-owned digest qualifies
resource identity only, never content or source revision. It prevents a later
mapping to another physical original from reusing the same evidence identity
merely because two files have the same file-scoped version number. The same
logical pair and admitted binding produces the same identity. No raw or
reversibly encoded private mapping is returned. Clients retain the issued
identity unchanged; they do not calculate or interpret it. Hash collisions are
not treated as an absolute mathematical impossibility. The source `revision`
remains the exact provider version, never this identity digest.

## Observation semantics

TKL uses its admitted managed FileStore, under the original caller, to stat the
exact bound original, read and completely consume its materialized binary Bag,
then stat the same original again. Every stat must confirm exact reference,
File kind, configured parent, media and any declared length. Every known stat
length and Bag length must equal the actual consumed byte count. URL-backed or
nonbinary Bags, read/close failures and inconsistent bytes fail with fixed
`resourceInvalid`, without leaking backing paths or stream error text.

If both source versions are present, valid and equal, TKL returns that exact
version and `available`. Different known versions fail with fixed
`resourceInvalid`; no partially observed list is returned. If either version is
absent, TKL returns `unverified` and omits `revision`, even if the other stat has
a version. The absence is not filled from registration/Candidate revisions,
time, hash, etag guesses or a locally invented snapshot. Invalid supplied source
version text fails rather than being normalized. Provider/custody failures keep
their native structured Conclusions.

This is an observation around a completed read, not a version-pinned retrieval,
an immutable snapshot, proof of future accessibility, or retained historical
bytes. An `available` observation says which source version corresponded to
that read within the provider's version semantics. It does not certify the
whole Material manifest, derived resources or later independent reads. The
result always states this limitation. No observation is stored as a new TKL
Entity or provider write. Consumers explicitly bind the result to their own
native reviewed target; acquiring it does not Review or Approve a Candidate.

## Compatibility and privacy

Search, metadata-only `getMaterial`, and private binary `readOriginal` retain
their existing behavior. Registered metadata remains usable without a provider
connection; only explicit evidence observation requires original access.
Package metadata continues its existing bounded manifest reads and never reads
an original without this selection. Connection use and Material authorization
are independent. Evidence exposes logical selectors and the provider-neutral
version only, with no physical IDs, paths, connection/subject IDs, credentials,
raw original bytes or remote error messages. Consumers keep original access on
the existing private/no-store binary route.

The added CML collections preserve existing wire input/result omission and
`createC` reader behavior. Generated Scala positional constructors gain the
new collection parameter at this development SNAPSHOT; consumers using those
constructors must regenerate/adapt source. Binary constructor compatibility
is not claimed. No generated DTO source is maintained by hand.

Paired executable specifications: `MaterialEvidenceObservationSpec`,
`MaterialReferenceOperationSpec`, `MaterialPackageReferenceSpec` and
`MaterialRegisteredOriginalSpec` (native PostgreSQL authorization and retained
registration/materialization compatibility).
