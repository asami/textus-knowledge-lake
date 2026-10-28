# TKW Phase 3 Material evidence producer

The existing TKW Phase 3 Session admitted KnowledgeLake dependency development
after the human's `ApproveDependencyScope — TKLを追加して続行` decision.
The dedicated branch is `codex/tkw-phase3-material-evidence`, based on
`3619fbff6d443daeb6f2b553baec9967493a9a07`. Existing main is retained.

The producer extends existing `getMaterial` with an explicit logical resource
selection. Metadata-only reads remain unchanged. Opt-in observations use the
existing managed FileStore, complete original-byte consumption and source
stat confirmation before/after. Source versions remain provider observations,
not registration/Candidate revisions or invented snapshots. Missing source
versions remain unverified. The source-version observation cannot promise
historical version-pinned access or retention.

The parent chose a deterministic TKL-owned opaque resource-incarnation identity
qualifier to distinguish rebinding to a different physical original even when
file-scoped provider versions coincide. A canonical private-binding identity
digest qualifies evidenceRef only. It is not a content hash, source revision
or retention claim, and physical keys/connection IDs are not exposed.

Behavioral specifications cover the native Bootstrap, Registered/PostgreSQL and
Packages routes and the managed read's complete byte/stream/version semantics.
This record describes authored changes only: no build, focused test,
independent review, publication or acceptance has yet been performed by this
implementation worker. The owning sm-workflow runtime coordinates those gates.
No real Drive writes, new materialization, credential/ACL changes, main
integration, push or external publication are part of this work.

## Runtime preparation diagnosis

The parent reported that a fresh `cozyPrepareRuntime` failed in generation with
`No name in table.`; empty generated-output provenance was a subsequent error.
The retained wrapper stderr includes the actual terminal record with
`sbt_exit=1`, `wrapper_exit=1` and `process=terminated`.

Source tracing identified a pre-existing CML authoring defect: the trailing
Material Package note followed the last `MaterialRegistrationResult` Attribute
table without ending that Attribute section. The schema parser also parses
Attribute section text as key/value definitions; the note includes `Material:`,
`PreparedMaterial:` and `KAR:` without an attribute `name`. Such an anonymous
record reaches the parser's `No name in table.` failure. The baseline CML has
the same placement; this is not evidence of a malformed new table header or
an incompatible generator coordinate.

The limited correction places the intact note in a sibling `DESCRIPTION`
section, preserving all declared fields and note content. Source tracing
supports this diagnosis; successful native regeneration and focused validation
are still required to establish the repaired execution outcome. The worker did
not run generation, validation or provider operations.

The parent's subsequent preparation002 generated 24 Scala sources successfully
after the heading correction, confirming that limited CML diagnosis. Compilation
then reported E050 at `MaterialServiceFactory.scala:253`: the foldLeft initial
`Consequence.success` expression's multiline type ascription was parsed as an
additional parameter application. The correction supplies the accumulator type
explicitly on `foldLeft` and leaves the empty accumulator and selection behavior
unchanged. This compile result is retained as a failure; qualification of the
corrected Scala expression remains pending. The implementation worker changed
only that source expression and this record, without running generation/tests
or editing generated sources.

## Development runtime preparation after the limited corrections

The third ordinary `cozyPrepareRuntime` preparation succeeded: 24 CML-generated
sources were reused, 40 Scala sources compiled, and the standard development
classpath/manifest were written. The registered wrapper retained exit 0 and
process-terminated evidence. Earlier parser and Scala compilation failures remain
recorded; neither success nor correction erases those attempts. No tests, provider
access, publication, commit or remote operation ran in this preparation.

The parent native Session remains on its actual P3-01 RepairStep (1/10), caused by
the first consumer check finding missing development-runtime evidence. The
producer and consumer focused checks and independent review still have to pass
through that Session before Step acceptance. This preparation is not Phase PASS.
