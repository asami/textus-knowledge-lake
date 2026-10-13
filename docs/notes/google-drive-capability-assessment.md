# Google Drive Capability Assessment — Phase 2

## Stage Status

- Current status: Documentation-grounded capability assessment dated 2026-10-03, with accepted controlled transport characterization and subsequent bounded released bytecode inspection; production transport fitness and live provider acceptance pending.
- Owner: Textus Knowledge Lake (TKL).
- Update rule: Distinguish official documentation, released artifact inspection, controlled tests and live observations. Update capability and acceptance claims only from the corresponding actual evidence; documentation or signatures alone cannot establish runtime behavior.

This assessment supports the initial TKL-P2-01A documentary investigation and accepted TKL-P2-01B1 controlled transport characterization within [Phase 2](../phase/phase-2.md) and its [acceptance checklist](../phase/phase-2-checklist.md). Subsequent released bytecode observations remain inspection-only. It extends the [Google Workspace design note](google-workspace-backed-tkl.md) while preserving the closed [preparation contract v1](../spec/preparation-contract-v1.md), accepted [local preparation flow](../spec/local-preparation-flow-v1.md) and [native application model](../spec/native-application-model-v1.md). The [dated discovery record](../journal/2026/10/2026-10-03-phase-2-capability-discovery.md) preserves the initial boundary and later evidence. No Step, checklist item or Phase is closed by this assessment.

## Evidence inventory — 2026-10-03

The parent inspected the following official references and released artifacts, then admitted the controlled transport suite and its independent Slice review. The IDs identify bounded documentary, inspection or controlled claims, not live Google receipts. All Google execution remains pending.

| ID | Evidence and supported claim | Limit and resulting obligation |
| --- | --- | --- |
| CAP-P2-01 | [Drive downloads and exports](https://developers.google.com/workspace/drive/api/guides/manage-downloads): blob text uses `files.get` with `alt=media`; Google Workspace documents use `files.export`. | API contract only; actual acquisition must be observed through the chosen adapter. |
| CAP-P2-02 | [Supported export formats](https://developers.google.com/workspace/drive/api/guides/ref-export-formats): Google Docs supports `text/plain`. | Initial candidate representation; Japanese and newline fidelity remain live acceptance requirements. |
| CAP-P2-03 | [files.export reference](https://developers.google.com/workspace/drive/api/reference/rest/v3/files/export): the response is bytes and exported content has a 10 MB limit. | Enforce TKL's own configured bounded profile; distinguish valid empty content from read failure. |
| CAP-P2-04 | [files.list reference](https://developers.google.com/workspace/drive/api/reference/rest/v3/files/list): discovery exposes `nextPageToken` and `incompleteSearch`. | Query only selected folders, consume bounded pagination and surface incomplete results. An empty page with a next token does not end enumeration. |
| CAP-P2-05 | [Create files](https://developers.google.com/workspace/drive/api/guides/create-file): ordinary blob creation can use generated file IDs; retrying an ID already successfully created returns 409 Conflict instead of another file. Google Workspace creation does not generally support pre-generated IDs. | Probe ordinary JSON blob creation and durable retry identity. This contract alone does not prove a TKL publication protocol. |
| CAP-P2-06 | [files.update reference](https://developers.google.com/workspace/drive/api/reference/rest/v3/files/update): Drive v3 updates use PATCH. | The inspected reference does not establish TKL's required conditional-write behavior. JSON revisions, provider versions, HTTP syntax and other Google APIs do not prove Drive CAS. |
| CAP-P2-07 | [Manage revisions](https://developers.google.com/workspace/drive/api/guides/manage-revisions): purgeable blob revisions can be discarded. | A provider history reference alone cannot satisfy TKL retention; retain selected acquired text in TKL-owned versioned canonical records. |
| CAP-P2-08 | [Drive authorization scopes](https://developers.google.com/workspace/drive/api/guides/api-specific-auth): `drive.file` authorizes files used or shared with the app on a per-file basis. | A folder ID does not prove access to all selected existing sources. Verify the credential facility, granted scopes and source/storage access without exposing credentials. |
| CAP-P2-09 | [Google Drive Projects](https://support.google.com/drive/answer/16684520?hl=en): Gemini in Drive groups sources and provides a conversation context; eligible plans are required and project creation uses the web. Source locations remain unchanged. | The reference establishes neither immutable source revisions, complete processor-visible inputs nor an automation API. Use a frozen TKL package and record the actual project reference and offered versus observed inputs. Actual manual execution belongs to Phase 2.1. |
| CAP-P2-10 | Released signature and class-name inspection of `org.goldenport:goldenport-cncf_3:0.5.2` and `org.goldenport:goldenport-core_3:0.4.2`, detailed below. | This historical inventory alone demonstrated neither Google authentication, UTF-8 fidelity nor HTTP execution; later controlled execution is recorded separately in CAP-P2-11. |
| CAP-P2-11 | Actual 17-case loopback [ReleasedHttpDriverSpec](../../src/test/scala/org/simplemodeling/textus/knowledgelake/drive/ReleasedHttpDriverSpec.scala) on CNCF 0.5.2/core 0.4.2 passed 17/17 on 2026-10-03, with an independent clean TKL-P2-01B1 Slice review. | Controlled transport characterization only; production fitness, native Drive dispatch and actual Google acceptance remain pending. |
| CAP-P2-12 | Subsequent bytecode inspection of released `HttpDriverFactory`, `McpStreamableHttpResponse`, `JavaMcpStreamableHttpExchange` and `McpStreamableHttpBodyReader` in CNCF 0.5.2, detailed below. | Inspection-only; the MCP exchange was neither invoked nor adopted as a Google adapter, and the inspected classes do not resolve all gaps or establish absence of every usable released facility. |

### Released HTTP boundary

Observed `org.goldenport.cncf.http.HttpDriver` signatures expose `get`, `post`, `put` and `postBag` with caller headers. No PATCH method is on that interface. `org.goldenport.cncf.http.UrlConnectionHttpDriver` implements it. Observed `org.goldenport.http.HttpResponse` signatures in core 0.4.2 expose `status`/`code`, `header`/`headerValue`, `bag`, `getString` and `getBinary`.

A class-name inventory of these two released JARs found no Google/OAuth-specific names. This does not establish absence of all usable facilities. The concrete PATCH interface gap must be kept separate from any claim about Google's capabilities or every possible released facility. Caller headers and response access are useful inspection facts; they establish neither authentication nor conditional writes nor successful content decoding.

Preserve released native dispatch and investigate/use this existing HTTP boundary before any new transport decision. TKL owns an explicit Drive adapter; generic HTTP remains framework-owned where its capabilities suffice. This Slice admits no shared-source change, SNAPSHOT refresh, direct `java.net` transport bypass, substitute framework or assumed Google automation.

### Controlled transport evidence — 2026-10-03

The actual loopback suite [ReleasedHttpDriverSpec](../../src/test/scala/org/simplemodeling/textus/knowledgelake/drive/ReleasedHttpDriverSpec.scala) exercised the concrete released `UrlConnectionHttpDriver` with `org.goldenport:goldenport-cncf_3:0.5.2` and `org.goldenport:goldenport-core_3:0.4.2`. The command was `sbt --batch "testOnly org.simplemodeling.textus.knowledgelake.drive.ReleasedHttpDriverSpec"`, including `Test/compile`. One suite completed with 17 tests succeeded and zero failed, canceled, ignored, pending or aborted. SBT and the serial wrapper exited 0, and the serial lock was released. No repository-complete suite was run.

The independent TKL-P2-01B1 transport characterization Slice review passed with zero Current Boundary Blockers, Hygiene or Development Candidates. Acceptance covers this controlled Slice; it does not complete TKL-P2-01 or establish Google capability or Phase acceptance.

| Boundary | Controlled observation | Limit for production consumption |
| --- | --- | --- |
| Valid and empty text | HTTP 200 preserves the selected valid Japanese, emoji, LF/CRLF, blank/trailing newlines and exact empty text. | Selected loopback cases do not establish actual Docs/blob fidelity. |
| Binary body | `application/octet-stream` retains original bytes, including NUL and malformed UTF-8, through `Bag`. | This binary observation does not prove strict text decoding. |
| Recognized errors and headers | 401/403/404 preserve code and exact text body; `Retry-After` access is case-insensitive. | Actual provider error handling remains to be evidenced. |
| Collapsed statuses | 409/429/503 and bodyless 204 become code 500. | Lost physical status cannot be reconstructed from the response code. |
| Success helper | 201 retains code 201, but `response.isSuccess` is false because the released helper recognizes only 200. | Successful creation must not be classified solely with this helper. |
| Malformed text | `text/plain` bytes `C3 28` become U+FFFD followed by an opening parenthesis. | The String or its re-encoded bytes cannot reconstruct invalid original bytes or prove strict decoding. |
| Request and controls | POST preserves selected UTF-8 JSON and `Map` headers; the `Property` vector supplies framework controls. | These observations establish neither authentication nor conditional writes. |
| Byte limit | The exact response-byte limit succeeds; a limit one byte below throws `IOException`. | Provider/export bounds still require their own evidence. |
| Redirect and network policy | Disabled 303 redirect retains `Location` without forwarding fake authorization; public-network-only rejects loopback before a request. | These are controlled policy observations. |
| Incomplete exchange | Closing before response headers throws `IOException` and may involve multiple physical attempts. | No exact retry count or provider outcome was asserted. |

### Additional released bytecode evidence — 2026-10-03

Inspection of `org.goldenport.cncf.http.HttpDriverFactory` found that real/url-connection selects the same `UrlConnectionHttpDriver`; fake/nop/loopback alternatives do not prove another production HTTP path.

Inspection-only observations of `org.goldenport.cncf.mcp.client.McpStreamableHttpResponse` show an integer status, and `JavaMcpStreamableHttpExchange` copies the physical `statusCode`. The exchange supplies a UTF-8 request body for POST, while its other arbitrary-method branch uses `noBody`, including PATCH. `McpStreamableHttpBodyReader` uses replacement UTF-8 decoding and exposes no original response-byte payload. The exchange was not invoked or adopted as a Google adapter. These named classes do not resolve all observed gaps, and this bounded inspection does not establish that every released facility lacks support.

Dependent production coding requires a verified released facility or an evidenced safe provider interpretation/reconciliation for the observed gaps. No workaround, narrower supported text subset or CAS guarantee is selected by these observations.

## Parent decisions and preserved contracts

TKL owns meaning, canonical records and versioned retained artifacts in selected Drive folders. Source folders are read-only from TKL. The initial profile uses one TKL instance, one selected storage root and explicit source folders; account-wide discovery is excluded. Drive Project is an external processing context binding rather than canonical storage.

Preserve closed preparation-bundle v1 fields and enums, opaque IDs, exact retained Unicode, newlines and empty text, reference-only null content, explicitly unknown provenance/provider revision and fixed historical Run inputs. Resource revision, Evidence revision, provider revision, snapshot ID and Run ID remain distinct. Preserve accepted local `PreparationStore` CAS and native behavior. A Drive adapter may claim equivalence only with observable evidence.

`text/plain` is the initial Google Docs export candidate. The controlled transport results above do not establish an actual Google read, retention, authentication, same-version competition, persistence or recovery pass; all remain pending. Independently manageable versioned canonical records and retained artifacts are required; their production schema and publication protocol remain unsettled.

### Publication candidate — unselected

Investigate ordinary JSON blob creation using pre-generated IDs, immutable per-record payloads and a single shared publication slot for an accepted predecessor/version. Different writers would need to compete for the same slot; independently accepted next revisions created by each writer would not satisfy the conflict obligation.

This is an investigation candidate, not an adopted writer model, CAS guarantee, complete storage schema or tested implementation. Later design must define how the slot is shared and durable, how references are validated, why readers see a complete logical state, how interruption reconciles and how external modification/deletion is detected or excluded. Naming uniqueness and list order do not arbitrate publication.

Conditional PATCH/If-Match remains unverified. If considered later, it requires stale and same-version competition observed through the exact chosen endpoint. Generic HTTP headers or Calendar behavior cannot prove Drive CAS. An in-process lock alone cannot enforce coordination across independent writers; no unconditional read-then-write fallback is admitted.

## Probe obligations

Each row names the distinguishing observable needed before capability or acceptance can be claimed. Controlled faults and actual provider guarantees must be labeled separately.

| ID | Capability or obligation | Required observable evidence | Requirement/checklist | Current evidence |
| --- | --- | --- | --- | --- |
| PROBE-P2-01 | Authentication and boundaries | Selected account/credential facility, granted scopes, selected source folders and one storage root; successful scoped metadata checks. | P2-R01/R08-A; P2-C01 | Pending live inputs. |
| PROBE-P2-02 | Docs and blob reads | Actual Docs `text/plain` and UTF-8 blob contents, exact Japanese/newline/empty results, explicit errors and export-size bounds; pre/post source metadata observation. | P2-R02; P2-C02/C05 | Pending live. |
| PROBE-P2-03 | Creation and retry | Disposable ordinary JSON with one pre-generated file ID; retry the same ID, read back exact content/parents and report 409 without a second accepted file; preserve the ID across an unknown response. | P2-R03/R04/R05; P2-C02/C08/C09 | Pending live. |
| PROBE-P2-04 | Competing publication | Two independent writers share the same accepted version/publication slot; at most one incompatible state is accepted; the loser sees conflict and read-back is coherent. Verify exact supported enforcement and bypass limits before adopting a writer protocol. | P2-R04; P2-C03/C08 | Pending protocol and live. |
| PROBE-P2-05 | Bounded discovery and stable identity | Explicit folder queries, all bounded pages, `incompleteSearch` handling, shortcut policy, deduplicated provider IDs and stable Resource mapping across supported rename/move and rediscovery. | P2-R01; P2-C04 | Pending controlled and live. |
| PROBE-P2-06 | History and clean restart | Versioned per-record persistence; a source edit creates later Evidence while earlier Run input remains exact; a fresh process reconstructs canonical bundle/Markdown with prior local cache unavailable. | P2-R02/R03; P2-C06/C07 | Pending implementation and live. |
| PROBE-P2-07 | Failure and reconciliation | Controlled preapplication, postapplication lost response, interruption, corrupt/missing envelope/reference, duplicate/orphan and projection failure; restarted operations reuse durable logical IDs and provider refs; no destructive cleanup. | P2-R03/R05; P2-C07/C09 | Pending controlled faults; live guarantees separately labeled. |
| PROBE-P2-08 | Released native/transport consumption | Released CNCF HTTP path handles observed headers/status/body/limits; generated CML/CNCF dispatch exercises acquisition, persistence and restart without breaking the local v1 flow. | P2-R08-A/R09-A; P2-C17 | Accepted controlled HTTP characterization (17/17) and bounded bytecode inspection; production transport fitness and native Drive dispatch pending. |
| PROBE-P2-09 | Manual context handoff | Concrete Drive Project reference/operator procedure, eligible product access, exact frozen package and result-import contract, v1 final statuses and citations, repeated/conflicting result semantics. | P2-R08-A; P2-C02/C18 | Product documentation identified; environment and settled interface pending; actual session Phase 2.1. |

## Live execution inputs — pending

Every execution input below remains open until actual scoped evidence is available. Record nonsecret configuration and observations only; credentials must remain in the chosen credential facility and must not enter prose, source, fixtures, logs or handoffs.

| Input | Needed evidence | Current status |
| --- | --- | --- |
| Account | Explicitly selected account and applicable product access. | Pending. |
| Source folders | Explicit folder IDs and successful scoped access to the selected existing Docs/text sources. | Pending. |
| Storage | One selected TKL storage root with verified write/read access. | Pending. |
| Credential facility | Actual supported authentication facility and its integration with the released native route. | Pending. |
| Scopes | Actual granted scopes sufficient for selected source reads and storage operations; a folder ID or declared scope alone is insufficient. | Pending. |
| Disposable probe objects | Selected Docs and UTF-8 text objects with Japanese/newline/empty cases, and ordinary JSON creation/competition objects confined to the probe storage boundary. | Pending. |
| Drive Project | Eligible product access and exact actual project reference for the later manual handoff. | Pending. |

The account/source/storage/auth question remains unanswered. These inputs are acceptance prerequisites, not a request to place credentials in documentation. Provider artifacts must not be deleted automatically during this work.

## Drive Project and manual handoff

The intended product is provisionally grounded by the Google Drive Projects reference already linked in the [Workspace note](google-workspace-backed-tkl.md). User clarification remains open; the documented product can still be assessed without claiming that the user confirmed its identity.

The [published Drive Projects route](https://support.google.com/drive/answer/16684520?hl=en) is: on the web, use **New > New Project** to select sources; reopen the context through **Projects**; copy an output or export it to a Doc. Eligible plans are required, and source locations remain unchanged. This is a documented operator route; no actual project access or session has been observed here.

TKL must own the frozen package and its exact Resource/Evidence/baseline version selection, Run and operation identities, citations and known/unknown provenance. Record the exact external project reference and distinguish inputs offered by TKL from inputs observed to be used by the processor. The inspected product reference supplies no immutable source revision guarantee, complete processor-visible input evidence or automation API; grouping mutable sources cannot substitute for TKL's retained versioned inputs.

Later Phase 2 contract work must settle the versioned package/result-import interface, closed v1 final Run statuses, identical versus conflicting repeated results, and projection failure after canonical persistence. The [planned Phase 2.1](../phase/phase-2.1.md) owns actual manual-session execution and import implementation. This Slice does not authorize that execution.

## Remaining decision gates

The parent must obtain scoped execution inputs, complete capability probes, resolve production transport fitness through a verified released facility or evidenced safe provider interpretation/reconciliation, and select an evidenced writer/publication/recovery model before dependent production adapter implementation. Storage envelopes and references require explicit versioning while closed v1 exports remain unchanged. The accepted transport suite leaves controlled faults and live storage/native acceptance as later obligations; static document checks cannot establish them.

Phase 2 retains focused validation, affected Phase 1/native regressions, live storage acceptance and independent reviews. Under its recorded final-owner policy, only the repository-complete suite is deferred to Phase 2.1 for the sequence Phase 2 → Phase 2.1. There is no current provider capability pass, full-suite claim or Phase acceptance claim in this assessment.
