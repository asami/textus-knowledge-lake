# Stored Material package read v1

Contract date: 2026-10-25
Owner: TKL Material access
Status: implementation contract

This contract adds an explicitly configured read-only view of existing
[Material packages](material-registration-package-v1.md) to
[Material reference v1](material-reference-v1.md). Public Operations, CML,
generated DTOs and ABI remain unchanged. Package availability does not imply
native SQL registration, import, write, preparation or Phase acceptance.

## Trusted package admission

Only exact `material.reference.mode=packages` selects this view. The optional
`material.reference.packages` parameter contains exactly:

```json
{"schema":"textus.knowledge-lake.material-packages/1","materials":[{"materialRef":"urn:textus:klake:lake:material:item","authorizedSubjects":["subject"],"storage":{"provider":"google-drive","connectionId":"connection","storeName":"store","rootKey":"root","parentKey":"parent"}}]}
```

The text limit is 131072 UTF16 units; duplicate semantic object keys and unknown
fields are invalid. There are at most 32 entries. Each entry has 1–128 distinct,
nonempty subjects of at most 256 units without ISO controls. Connection/store
names match `[A-Za-z0-9][A-Za-z0-9._-]{0,127}`; root/parent keys match
`[A-Za-z0-9_-]{1,256}` and differ. Exact Material URNs must roundtrip through
`MaterialLocation.createC`; no spelling is normalized and no provider key is
derived from a URN. Material URNs and `(storeName, rootKey)` pairs are unique.
Invalid configuration yields fixed `configurationInvalid` message
`Material package read configuration is invalid`; diagnostics redact bindings.

Trusted configuration alone establishes package subjects. Remote owner,
createdBy and provenance fields are metadata and never grant authority.
Bootstrap and Registered reject any supplied package parameter, including empty
text. Packages rejects bootstrap catalogs and registered read bindings. Views
never merge or infer one another. Missing package configuration is unavailable;
an explicit empty materials array supports empty search without provider access.
Registration commands require Registered mode in every other view.

## Bounded native reads

After provider-authenticated caller and exact trusted subject admission, TKL
uses CNCF's managed connection and public `FileStoreChildren` capability.
Connection permission remains independently enforced. TKL stats only the
configured root, requiring its exact reference, Collection kind and configured
parent. It enumerates immediate children of that root and required assets and
derived collections only. Every request has limit 50; each collection allows
at most four independently requested pages and 200 total children. Only actual
continuations are followed, including after empty pages. Repeated cursors,
duplicate references across pages and continuation after the budget fail.
All bounded pages are collected before exact-name uniqueness selection.
Duplicate labels are ambiguous; there is no first-match selection or recursion.
Native listing, transport and authentication Conclusions propagate unchanged.

The root must contain exactly one `material.json`, a File of exact
`application/json` MIME with each observed size nonnegative and at most 65536.
A separate stat confirms exact reference, name, parent, kind, MIME and size.
The read must also return application/json and a materialized, non-URL Bag.
Its stream closes on every outcome; reading uses `readNBytes(65537)`, requires
at most 65536 actual bytes, and agrees with every present list/stat/Bag size.
UTF8 decoding reports malformed or unmappable input, and JSON rejects duplicate
semantic keys. Read/encoding/JSON exceptions are contained through Consequence.
Integrity, ambiguity and malformed-package failures use fixed
`resourceInvalid("Stored Material package is incomplete or ambiguous")`,
without bytes, pointers or credentials.

## Manifest and private mapping

The manifest has exactly `schema`, `id`, `title`, `createdAt`,
`classifications`, `provenance`, `owner`, `createdBy`, `assets`. Schema is
`textus.material/1`, ID equals the configured URN literally, createdAt parses
as an Instant, and provenance is a JSON object. Title is nonempty, at most 4096
units and free of ISO controls. Owner/createdBy and each classification follow
the same text rule with limit 256; classifications have at most 128 entries.
There are at most 128 assets and at least one original.

An original has exactly `path`, `role`, `mediaType`, with path
`assets/<resourceId>`. A derived entry additionally has `derivedFrom`, path
`derived/<resourceId>.json` and exact application/json MIME. Resource IDs match
`[A-Za-z0-9][A-Za-z0-9_-]{0,127}`. Roles follow the 256-unit text rule; MIME
uses the existing type/subtype token grammar. Derived lineage is nonempty,
distinct and refers only to declared original paths. Duplicate paths/IDs,
traversal, deeper paths, URI/percent/backslash paths and unknown fields fail;
paths are never normalized.

Exactly one assets Collection must be present beneath root. When derived
entries exist exactly one derived Collection is required too. Every selected
collection confirms root parent and owning store. Each declared entry resolves
by exact leaf name against the collection's complete bounded listing, requiring
one File, exact parent and declared MIME. Metadata/search never read original
or derived bodies. The snapshot privately retains original order, role, MIME,
optional observed length and exact FileStore reference/parent/connection/store.

## Logical Operations and privacy

Search filters trusted subjects before any provider access, counts or paging,
then resolves authorized packages sequentially. Matching uses real manifest
title or exact reference with Locale.ROOT lowercase substring matching. Exact
URN ordering and existing offset/limit/hasMore semantics apply with overflow-safe
slicing. Get resolves an exact authorized binding and exposes only materialRef,
title, original logical resource records and provenance
`[{kind: material-package, reference: <exact URN>}]`. Unknown references return
resourceNotFound; configured unauthorized references return permission denied.
Remote owner/provenance/body/storage fields and physical mappings never escape.

ReadOriginal selects an exact original logical ID from the admitted snapshot;
unknown and derived IDs return resourceNotFound. It privately adapts the selected
original to the existing binary transport, preserving exact stat/parent/MIME/
length confirmation, BinaryBag bytes and private no-store, nosniff, sandbox and
attachment headers. No SQL Entity Store reads/writes, mutation, retry or fallback
are part of this view. Studio and Workbench retain logical routing only.

## Executable specifications

[MaterialPackageReadSpec](../../src/test/scala/org/simplemodeling/textus/knowledgelake/material/MaterialPackageReadSpec.scala)
specifies configuration, bounded enumeration and package integrity.
[MaterialPackageReferenceSpec](../../src/test/scala/org/simplemodeling/textus/knowledgelake/material/MaterialPackageReferenceSpec.scala)
specifies native factory/ActionCall/UOW search, metadata, original binary transfer,
authorization and explicit mode isolation with controlled provider responses.
