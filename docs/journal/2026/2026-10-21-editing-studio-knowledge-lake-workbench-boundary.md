# Editing Studio / Knowledge Lake / Knowledge Workbench Responsibility Boundary

Date: 2026-10-21
Status: Active

## Context

While implementing a route for Editing Studio to refer to the Knowledge Lake backed by Google Drive, the responsibility boundary among Editing Studio, textus-knowledge-lake (TKL), textus-knowledge-workbench (TKW), and Google Workspace/Drive needs to be made explicit.

The key distinction is between **referring to Google Drive as physical storage** and **referring to the Knowledge Lake as a logical domain service**. If Editing Studio directly understands Drive folder layout, Material placement rules, prepared-data locations, or Knowledge Lake URNs, Knowledge Lake-specific knowledge leaks into Editing Studio and the subsystem boundaries become unstable.

The current conceptual flow remains:

```text
Material -> Information Candidate -> Information -> Knowledge
```

## Decision

### textus-knowledge-lake (TKL)

TKL owns the logical Knowledge Lake interface.

Its responsibilities include:

- identification of Material;
- Knowledge Lake URN handling;
- Material index and metadata;
- search and retrieval of Material;
- mapping between logical Material and its physical representation;
- management of raw Material and prepared Material locations;
- abstraction of Google Workspace/Google Drive as the current Knowledge Lake backend;
- exposure of Knowledge Lake operations to clients without requiring them to understand Drive layout.

Therefore, a client asking for Material should normally access TKL rather than the Google Drive API directly.

### textus-knowledge-workbench (TKW)

TKW is a **work environment over Material**, not the generic access gateway to the Knowledge Lake.

Its responsibilities are centered on:

- exploring Material;
- selecting Material relevant to knowledge creation;
- human-in-the-loop inspection and editing;
- producing or preparing Information Candidates from Material;
- maintaining an index needed for workbench activities.

TKW may use TKL to access Material. Other applications do not have to pass through TKW merely to access the Knowledge Lake.

### Editing Studio

Editing Studio owns full-fledged editing work around Information Candidates and Information.

Editing Studio may refer to Material as source/reference information during editing. Such access should normally be:

```text
Editing Studio
    -> TKL
        -> Google Workspace / Google Drive
```

Editing Studio should not normally need to know:

- Knowledge Lake folder conventions;
- where raw/prepared Material is physically stored;
- how Gemini-generated preprocessing artifacts are laid out;
- how Knowledge Lake URNs map to Drive resources.

These are TKL concerns.

### Google Workspace / Google Drive

Google Drive is the current physical storage layer of the Knowledge Lake.

It stores, for example:

- raw Material;
- large original assets;
- prepared Material;
- artifacts produced by Gemini in Drive or other preprocessing;
- other files managed as Knowledge Lake resources.

Google Drive itself is not the logical Knowledge Lake API.

## Standard access route

The standard logical access route is:

```text
Editing Studio ----+
                   |
TKW ---------------+--> TKL --> Google Workspace / Drive
                   |
other clients -----+
```

This allows TKL to remain the stable logical boundary even if physical storage or preprocessing mechanisms change in the future.

## Direct Drive access exception

Direct access to Google Drive is acceptable for physical data-transfer cases, especially large original files where routing the byte stream through TKL would be unnecessary.

For example:

```text
Editing Studio ------------------> Google Drive
       |                            ^
       +---------- TKL -------------+
          logical registration /
          metadata / location
```

This is a physical-transfer optimization, not an alternative Knowledge Lake domain interface.

TKL should continue to own the logical identity, metadata, URN, and location relationship of the uploaded Material.

## Responsibility rule of thumb

A useful boundary test is:

| Operation | Primary owner |
| --- | --- |
| Identify, search, locate, or retrieve Material | TKL |
| Store Material bytes/files | Google Drive |
| Explore Material and turn it into a Candidate | TKW |
| Perform full editing of Candidate / Information | Editing Studio |
| Move/synchronize/preprocess physical data | TEAI / OpenClaw and related integration mechanisms |

## Important consequence

The route currently being implemented from Editing Studio to the Knowledge Lake is valid and desirable, provided that its logical endpoint is TKL rather than Google Drive.

In other words:

```text
Editing Studio -> Knowledge Lake
```

should mean:

```text
Editing Studio -> TKL -> Google Drive
```

rather than embedding Knowledge Lake storage conventions in Editing Studio.

## Remaining design issue: transition ownership

There is still some intentional overlap between TKW and Editing Studio around Candidate creation/editing.

The next design step should explicitly define ownership for the transitions:

```text
Material
   |
   | Material -> Candidate
   v
Information Candidate
   |
   | Candidate -> Information
   v
Information
   |
   | Information -> Knowledge
   v
Knowledge
```

For each transition, define:

- primary owning subsystem;
- applications allowed to initiate it;
- required validation/commit conditions;
- where provenance is recorded;
- whether the transition creates a new entity or updates an existing one;
- how human-in-the-loop approval participates.

This transition-ownership matrix should be used to refine the TKL API and to clarify the remaining boundary between TKW and Editing Studio.

## Architectural principle

**Storage is not the Knowledge Lake interface.**

Google Workspace/Drive is an implementation mechanism for storing Knowledge Lake resources. TKL provides the logical Knowledge Lake boundary. TKW and Editing Studio are clients with different work responsibilities over the Material -> Candidate -> Information -> Knowledge lifecycle.
