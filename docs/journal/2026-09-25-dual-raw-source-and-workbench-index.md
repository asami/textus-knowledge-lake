# Dual Raw Sources and Workbench Raw-Data Index

Date: 2026-09-25
Status: active

Google Workspace is the physical Knowledge Lake and contains two distinct classes of raw material.

1. Native Raw Evidence: material that exists independently of a Workbench candidate, such as Drive documents, Gmail, Slack-derived collections, Web material and other federated evidence.
2. Managed Candidate Source: raw material intentionally submitted for an existing Information Candidate, such as mobile book photographs, audio, transcripts and capture-derived metadata.

TKL manages Knowledge Lake resources and preparation for both classes. Native Raw Evidence normally follows discovery preparation: evidence -> PreparedMaterial -> candidate proposal. Managed Candidate Source follows candidate preparation: an existing TKW candidate/source index references Google Workspace material -> TKL prepares it -> PreparedMaterial returns to that candidate.

TKL does not own the Information Candidate lifecycle. For Managed Candidate Source, TKW DB owns the candidate-centric raw-data index and references TKL Resource IDs / Google Workspace resources.

Google Workspace stores raw/derived file bodies. TKL DB manages Knowledge Lake Resource/Evidence/Preparation/PreparedMaterial metadata. TKW DB manages InformationCandidate/CandidateSource/RawDataIndex/Review/Admission metadata.

The same physical Google Workspace resource may therefore be referenced from both a TKL Resource record and a TKW RawDataIndex record without duplicating the resource itself.
