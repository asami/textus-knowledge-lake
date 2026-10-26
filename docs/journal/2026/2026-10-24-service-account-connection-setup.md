# TKL service-account connection preparation

Date: 2026-10-24

This is an operational procedure for the dedicated development checkout.
The normative connection contract is CNCF's
`docs/spec/google-drive-service-account.md`; TKL's consumer contract is
[Material reference v1](../../spec/material-reference-v1.md).
No real Google connection success is recorded here. The initial report placed
the key on Mac mini; the user subsequently selected MacBook Air as the
verification host and reported that TKL/Studio have not been started. Select
the existing key explicitly on that runtime host; no key is copied by this work.

## Existing operator preparation

The operator reports Google Cloud project `textus-editing-studio-dev`
(`702480093991`), enabled Drive API, shared Drive `Textus Knowledge Lake Dev`,
and service-account membership for
`tkl-drive-deve@textus-editing-studio-dev.iam.gserviceaccount.com`.
Drive ACL access still needs verification. The suggested key path is
`/Users/asami/.config/textus/credentials/knowledge-lake-dev.json` on Mac mini.
This document contains no key or token. The path above is the initial proposal,
not an observed working MacBook Air configuration. Metadata-only discovery on
MacBook Air found a different JSON filename; its selection requires the user's
identification before real provider authentication.

## Private configuration on the runtime host

1. Keep the downloaded service-account JSON outside Git and synchronized
   folders. Its direct parent directory must be owned by the runtime OS user
   with mode `0700`; the JSON file must have mode `0600`, the same owner, no
   hardlinks, and no symlink anywhere in its path. Apply the same conditions to
   the separate connection configuration file below. The program refuses
   unsafe permissions without repairing them.
2. Prepare a separate UTF-8 JSON configuration using the following shape.
   Replace `EXACT_NATIVE_SUBJECT_ID` with the provider-authenticated caller ID
   used by the Studio → Workbench → TKL route, not the service-account email.
   Choose the same connection ID as the existing TKL catalog or registration
   and registered-read binding. Here `knowledge-lake-dev` is an example only.

```json
{
  "schema": "cncf.google-drive-service-account/1",
  "connectionId": "knowledge-lake-dev",
  "allowedSubjects": ["EXACT_NATIVE_SUBJECT_ID"],
  "scopes": ["https://www.googleapis.com/auth/drive.readonly"],
  "credentialFile": "/Users/asami/.config/textus/credentials/knowledge-lake-dev.json"
}
```

3. Supply this file's canonical `file:///...` URI as TKL's optional component
   initialization parameter `google-drive.service-account.configuration` in
   the owning component-instance assembly. Do not supply
   `oauth.connection.configuration` at the same time. The parameter references
   the connection file, not the downloaded Google key. Keep this assembly
   configuration private as for existing OAuth configuration.
4. The runtime HTTP admission must permit token `POST` to
   `https://oauth2.googleapis.com` and the required Drive `GET` operations to
   `https://www.googleapis.com`, attributed to the actual TKL component.
   Native operation, caller and Material permissions continue to apply.
5. Restart/reconstruct the component after configuration/key changes. No token
   custody file is created; a token is acquired lazily and cached in that
   initialized connection. No browser OAuth screen is needed for this strategy.

Instead of `credentialFile`, the connection JSON may contain exactly
`"credentialEnvironment": "GOOGLE_APPLICATION_CREDENTIALS"`. Only that explicit
selection reads the environment variable; its value must be the same kind of
canonical absolute private key path. Merely setting the environment variable
does not select this strategy. No implicit ADC, metadata-server fallback or
domain-wide delegation is enabled.

The example read-only scope supports the first original-reference test.
Material registration/upload also needs an explicitly configured write-capable
Drive scope, corresponding native HTTP methods, and a shared-Drive role
permitting those operations. Scope does not grant Drive membership or Material
permission. Do not widen permissions simply because a read fails.

## First actual connection check

After the runtime host has the updated CNCF artifact and TKL checkout, use the
existing TKL `Material.getMaterial` and `Material.readOriginal` operations for
one explicitly admitted harmless Material/resource. Metadata retrieval alone
does not call Google and cannot verify the connection. Original retrieval
performs token exchange and confirms the known provider resource's kind,
parent, media and bytes. Record the actual outcome without token/body output.

Shared-drive discovery/listing and deletion are not part of the current
FileStore SPI. They are not prerequisites for reading a known Material and
must not be substituted with an Editing Studio Drive-specific logical API.
Keep physical resource mapping in TKL. Actual Drive ID/resource mapping and
Mac mini launch configuration remain to be supplied/verified before this
operational check; controlled native-driver specs are separate evidence.
