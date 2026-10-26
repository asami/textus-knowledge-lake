# Knowledge Lake Source Authorization Boundary

Status: initial projection
Updated: 2026-10-16
Canonical reference: asami/goldenport-cncf Textus Security Architecture / Entity Authorization / APK Untrusted Client note

Knowledge Lake treats Lake/Material/source identifiers as references, not authority. Access to a Material or original source is evaluated through the owning CNCF authorization path and the source system's own admitted authority.

A server possessing a stronger Google Drive/service credential must not act as an unrestricted confused deputy: a caller-provided Drive file ID, URI or Material URN alone is insufficient to authorize retrieval. The server must establish the caller/operation/source relationship or execute an explicitly authorized bounded system/proxy operation.

Where Knowledge Lake entities use the CNCF canonical internal DSL, Entity authorization is inherited through the UnitOfWork chokepoint. Direct source/provider access remains a separate external-resource authority boundary and must not be inferred from Entity visibility alone.


## Journal / staging security boundary

The Editing Studio ingestion route intentionally allows textus-flutter-core to
upload a newly created Raw Material package directly to the Google Drive-backed
Journal/Staging area so large image/audio/video payloads do not need to transit
a TKL application server.

This direct data-plane upload is not general Google Drive or Knowledge Lake
authority.

Security invariants:

- Flutter Core allocates the Material ID and uses the same ID for Raw Material
  and CNCF Material Data.
- Material Data refers to the Raw Material by a TKL URN; knowledge of the URN
  is not access authority.
- Client upload authority is bounded to the permitted Journal/staging ingestion
  route and does not imply arbitrary Lake read/update/list, another subject's
  journal access, or managed-source access.
- TKL remains the authorization/control boundary for reading, resolving,
  processing, updating or admitting managed Knowledge Lake resources.
- Provider-specific Drive IDs are physical implementation data, not authority.
- TKL must not use stronger Drive/service credentials as a confused deputy for
  an arbitrary caller-supplied URN/file ID.
- Journal content is untrusted/staged input until applicable TKL validation,
  provenance and authorization/admission processing accepts it.

This separates the large-payload data plane from the Knowledge Lake
control/authorization plane without routing media bytes through TKL merely for
security.

## URN and ownership authorization rule

The TKL URN intentionally reflects the canonical Lake/Journal layout sufficiently
to support deterministic resolution and recovery. Security design must preserve
that property rather than inserting per-user path segments solely to implement
authorization.

User/organization ownership is represented by Material owner/createdBy and
CNCF/TKL relationship attributes. Neither the presence of an owner string in
material.json nor knowledge of a URN grants authority.

On registration/intake, ownership claims from the Raw Material/Material Data are
validated against the authenticated SecuritySubject and the admitted operation.
On later access, TKL/CNCF authorization evaluates the managed Material/resource
relationship; it does not infer permission from URN path syntax.

This keeps canonical storage/recovery layout, logical identity, ownership and
authorization as distinct concerns while still allowing the URN to be a useful
recoverable locator.
