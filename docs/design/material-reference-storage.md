# Local referenced-original storage

The existing native registration Entity owns lifecycle, identity, authorization,
and the retained exact logical source binding and observed content baseline.
The host configuration resolves that binding to an admitted CNCF workspace and
literal path. The source declaration includes the exact logical workspace and
path and must match the configured binding both at creation and materialization;
the retained binding must still match on reads. Those values are selectors, not
permissions. The active execution context owns the actual File grants.

Materialization persists Materializing before external operations. It acquires
complete source bytes under an explicit per-read cap, rejects missing, wrong-size
or changed-during-read acquisitions, and computes SHA-256 from the returned
bytes. It checks every source root against the destination before the one
CreateNew JSON write and persists Registered only after that write succeeds.
An interrupted operation can leave a Materializing record or a JSON file and
requires observation, not automatic replay. No original-byte write is planned.

The native Entity is the authoritative baseline on reconstruction. The JSON
destination is a private metadata projection, not a separate registration or
grant. Later reads resolve the retained binding again and compare the obtained
bytes against the retained digest. The content comparison, path metadata,
source incarnation and read stability are independent. CNCF's opaque source
incarnation is not serializable; after reconstruction identity remains unknown.
The portable provider also reports stability Unknown unless it detects Changed.
Neither matching content nor the metadata projection proves approval or makes
prior evidence ready. Remote adapters and their qualification are outside this
local contract.
