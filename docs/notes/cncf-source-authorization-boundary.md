# Knowledge Lake Source Authorization Boundary

Status: initial projection
Updated: 2026-10-16
Canonical reference: asami/goldenport-cncf Textus Security Architecture / Entity Authorization / APK Untrusted Client note

Knowledge Lake treats Lake/Material/source identifiers as references, not authority. Access to a Material or original source is evaluated through the owning CNCF authorization path and the source system's own admitted authority.

A server possessing a stronger Google Drive/service credential must not act as an unrestricted confused deputy: a caller-provided Drive file ID, URI or Material URN alone is insufficient to authorize retrieval. The server must establish the caller/operation/source relationship or execute an explicitly authorized bounded system/proxy operation.

Where Knowledge Lake entities use the CNCF canonical internal DSL, Entity authorization is inherited through the UnitOfWork chokepoint. Direct source/provider access remains a separate external-resource authority boundary and must not be inferred from Entity visibility alone.
