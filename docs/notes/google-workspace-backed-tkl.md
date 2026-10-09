# Google Workspace-backed TKL and Knowledge Processing Contexts

- Date: 2026-10-02
- Status: Current design decision; implementation pending
- Scope: Initial Google Workspace backend, persistence, processing contexts and TKW boundary

## 基本構成

最初のターゲットは Google Workspace をバックエンドにした Textus Knowledge Lake (TKL) とする。

TKL の永続的な実体は Drive のフォルダまたはフォルダ群で表現する。TKL アプリケーションは、その上に Resource、Evidence、Preparation、PreparedMaterial、provenance と知識処理の文脈を管理する。

Google Workspace は最初の物理バックエンドであり、TKL の意味モデルは provider-neutral とする。既存の Drive 資料は参照を基本とし、TKL フォルダへ一律に移動・複製しない。

```text
TKL Drive フォルダ群
  資料 / メタデータ / 既存知識の投影 / 準備成果
        |
        +--> Drive Project ------+
        |                        |
        +--> Gemini Notebook ----+--> Gemini / 人による知識処理
                                      |
                                  準備成果
                                      |
                                 TKL へ取り込み
                                      |
                               PreparedMaterial
                                      |
                                Raw Candidate
                                      |
                                     TKW
                                      |
                          形成 / 編集 / レビュー / 承認
                                      |
                                KnowledgeHub
                                      |
                         TKL へ既存知識の投影を更新
```

ここで Gemini Notebook は、会話で合意した notebook 型の知識処理環境を指す設計上の呼称である。具体的な製品・API・連携方法は adapter 設計時に確認する。

## 永続化基盤と処理文脈の分離

| 概念 | 責務 |
| --- | --- |
| TKL フォルダ群 | 資料、TKL 管理情報、準備成果とその来歴を保存する |
| Knowledge Processing Context | 目的、参照資料、既存知識、処理履歴、成果の対応を表す |
| Drive Project | 知識処理の文脈を Google 側で実現する選択肢 |
| Gemini Notebook | 準備・分析・統合・成果物生成に用いる別の文脈の選択肢 |
| TKL アプリケーション | 保存基盤、処理文脈、成果の取り込みと TKW への引き渡しを管理する |

Drive Project と Notebook は TKL の永続的な管理情報の正本にしない。Notebook を presentation 専用に限定せず、知識処理の文脈として扱う。ただし両環境の機能や自動化 API が同一であるとは仮定しない。

TKL と処理文脈は一対一に固定しない。同じ資料を、合意事項の分析、技術調査、知識候補の抽出など、異なる文脈から参照できる。一つの文脈が複数の TKL フォルダや外部の既存資料を参照することも許容する。

## Drive 上の保存構成

以下は論理的な保存役割の例であり、フォルダ名・階層を必須仕様にはしない。

```text
TKL/
├── Metadata/          # Resource、Evidence、文脈、出典、版、処理履歴
├── Evidence/          # 管理対象の原資料、または既存資料への参照
├── PreparedMaterial/ # 正本 JSON と人・LLM 向けの投影
└── KnowledgeContext/ # KnowledgeHub の既存知識の投影
```

既存の NICT の `00_Inbox / 10_Evidence / 20_Artifacts / 90_Archive` 構成も、同じ論理役割へ対応付けて利用できる。フォルダ名・パスを TKL Resource の識別子にはしない。

初期実装では Drive 上の JSON を TKL 管理情報の正本とし、別の TKL DB を必須にしない。PreparedMaterial は引き続き stable ID と版・ライフサイクルを持つ TKL Entity であり、その正本を JSON として永続化する。

将来 DB を導入する場合は、検索用索引・キャッシュか、正本の移行先かを明確にする。Drive JSON と DB の双方を独立した正本として更新しない。原資料の正本は原 provider、成立した知識の正本は KnowledgeHub にある。

ファイルの `appProperties` は TKL ID など短い対応情報に利用できる。複雑な構造や長い処理履歴は JSON に置く。必要に応じて Gemini が参照しやすい文書へ投影するが、その投影を管理情報の正本にしない。


## Journal store and current index projection

For Knowledge Lakes that are used as a Body of Knowledge (BoK), the Drive layout should be self-contained: the logical meaning of the Lake must be recoverable without consulting a Git repository or another external index.

The recommended storage model is:

```text
Knowledge Lake/
└── components/
    └── <component>/
        ├── index/                  # mutable current projection
        │   ├── spec.json
        │   ├── design.json
        │   ├── notes.json
        │   └── journal.json
        └── journal/                # append-oriented source of truth
            └── YYYY/
                └── MM/
                    └── YYYY-MM-DD-<material-name>/
                        ├── material.json
                        ├── ...
                        └── derived assets
```

### Journal as source of truth

The `journal/` tree is the semantic history of the Knowledge Lake. New Material packages are added by date and should normally not be overwritten in place.

A directory containing `material.json` is a Material package. The package may contain images, PDFs, SVGs, audio/video, generated artifacts, source snapshots, references, and other provider-suitable assets.

The Material metadata must be sufficient to reconstruct its logical role without GitHub. Candidate metadata includes:

- stable Material ID and title;
- component / subsystem;
- creation and update timestamps;
- classification such as spec, design, notes, journal, architecture, reference, infographic;
- status;
- provenance and source references;
- asset roles;
- `derivedFrom`, `supersedes`, and related lineage when applicable.

Git repositories, Slack, Gmail, external websites, and other systems are provenance/source references, not prerequisites for interpreting the Lake.

### Index as current projection

The `index/` tree is a mutable projection of the current logical view. It may expose familiar BoK categories such as `spec`, `design`, `notes`, and `journal` even though the physical Material packages remain in the dated journal tree.

Index files may be overwritten. They are not the historical source of truth and must be rebuildable by scanning `journal/` and interpreting `material.json`.

This gives the Lake two complementary views:

- physical/history view: dated Material packages under `journal/`;
- logical/current view: rebuildable projections under `index/`.

### Google Drive version history

Google Drive version history is an operational recovery aid for mutable index files. TKL does not use Drive's version history as the semantic version/history model of the BoK.

Initially, no independent TKL index-backup mechanism is required. If operational experience requires stronger recovery, TKL may later add periodic index snapshots. Such snapshots remain operational recovery data rather than Knowledge semantics.

### GitHub relationship

GitHub remains appropriate for version-controlled text/model artifacts such as CML, Markdown specifications, source code, and exact reference diagrams. A Knowledge Lake Material may cite those artifacts as provenance or source.

However, the Knowledge Lake must remain semantically self-contained. A consumer must be able to reconstruct the component's BoK structure and Material lineage from the Lake's journal and metadata even when GitHub is unavailable.

This pattern is intentionally provider-neutral at the TKL semantic level. Google Drive is the first implementation backend.

## 知識処理の文脈モデル

`Knowledge Processing Context` は概念名であり、DTO/CML の確定済み仕様ではない。少なくとも次の対応を記録できるモデルを設計する。

- stable context ID、目的、テーマ、処理範囲;
- 参照する Resource / Evidence の ID と、各処理で実際に使用した版・snapshot;
- 既存知識の ID・版と KnowledgeContext の基準時点;
- 外部文脈の種別、識別子または参照先と利用した processor;
- 処理要求、実行履歴、人による操作、生成・処理 agent の情報;
- 成果物の参照、取り込んだ PreparedMaterial の ID・版と provenance;
- 文脈の状態、および obsolete / superseded な資料の扱い。

処理文脈の現在の参照集合と、個々の処理に実際に使われた資料集合を区別する。利用環境から確認できない処理詳細は未確認として記録し、完全な再現性を推測で主張しない。

Gemini に渡すのは、その処理に必要な Evidence、既存知識、説明文書とする。TKL の機械向けメタデータ全体を自動的に推論コンテキストへ含めない。旧版や無関係な資料を active context へ蓄積しない。

## TKW との関係

TKL は資料と準備処理を管理し、TKW は候補のライフサイクル、形成、編集、レビュー、承認、Admission を管理する。

二つの経路を持つ。

1. Discovery preparation: 外部 Evidence → TKL → PreparedMaterial → Raw Candidate Proposal → TKW。
2. Candidate preparation: TKW の既存候補に紐づく写真・音声等 → TKL で準備処理 → PreparedMaterial を元の TKW 候補へ返す。

TKW の CandidateSource / RawDataIndex は候補中心の索引であり、TKL Resource ID や外部資料を参照する。同じ資料を TKL と TKW がそれぞれの責務で参照でき、ファイル本体の二重保存は要求しない。

TKW を経由して KnowledgeHub に成立した知識は TKL の KnowledgeProjection に戻し、次の処理文脈で Existing Knowledge + New Evidence として利用する。

## 最初の実装経路

最初は小さな Drive 資料セットについて、Resource 登録 → Evidence → Drive Project を利用した準備処理 → 成果の取り込み → PreparedMaterial → 根拠付き Raw Candidate の TKW 引き渡しを成立させる。

Drive Project を最初の adapter として優先するが、TKL の文脈モデルは Notebook も表現できるようにする。Notebook 連携、TKW からの candidate preparation、Gmail / Slack は後続実装とし、最初の経路の成立条件にはしない。

次に確定するのは、フォルダ内メタデータの契約、ID・版、更新競合と部分失敗の扱い、文脈と処理履歴、成果の取り込み、および TKW への受け渡し契約である。具体的な API 自動化とメタデータ更新方式は未実装・未確定とする。

## 既存文書との関係

この note は、TKL の独立 DB を必須とする保存先の想定、および Notebook を presentation 専用に限定する想定を更新する。provider-neutral な意味モデル、外部資料の参照優先、PreparedMaterial Entity、TKW 境界、KnowledgeHub feedback は維持する。

README、Phase 1、CML scaffold に残る旧記述の整合は後続作業とする。この記録は設計判断であり、連携機能や Phase 1 の実装完了を意味しない。

## References

- [Architecture](architecture.md)
- [Application model](application-model.md)
- [Dual raw sources and Workbench index](../journal/2026-09-25-dual-raw-source-and-workbench-index.md)
- [Decision journal](../journal/2026-10-02-google-workspace-backed-tkl-contexts.md)
- [Google Drive projects](https://support.google.com/drive/answer/16684520?hl=en)
- [Drive custom file properties](https://developers.google.com/workspace/drive/api/guides/properties)
