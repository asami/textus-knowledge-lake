# Phase 1 Project-local Boundary Decision

- Date: 2026-10-02
- Status: User-directed scope correction; Phase implementation pending
- Owner / allowed project: `textus-knowledge-lake`

## User direction

> textus-knowledge-lakeのphase 1はプロジェクトに閉じた開発。他のプロジェクトには依存しない。
> 他のプロジェクトとの連携は管理して。クローズ時に必要な提案をするところまでが責務。

この指示により、Phase 1 は TKL 自身のモデル、永続化、準備処理、提案生成と外部境界の扱いを、TKL の repository-owned fixtures / executable specifications で成立させる開発とする。他プロジェクトの実装、稼働、リリース、契約合意を完了条件にしない。

## Superseded closure

旧 Phase 文書にあった TEAI 経由の実接続、KnowledgeHub への登録、BoK 公開判断を TKL Phase 1 の完了条件から外す。TKW が Candidate の形成・編集・レビュー・承認・Admission を所有する現在の設計は維持する。

TKL は RawCandidateProposal と lineage を生成し、TKL 側の結果未確認、同じ提案 ID による照会・再送、受領対応の記録をローカルの境界 fixture で検証する。fixture の TKW Candidate / receipt はテストデータであり、実 TKW の永続化・同時受領の冪等性を保証したとは扱わない。TKW の代替 store や承認機能は作らない。

Google Workspace-backed TKL は最初の本番ターゲットとして維持する。Phase 1 は Drive 用 JSON / adapter 契約と手動成果取り込みを扱い、live Drive、Drive Project 自動化、Google の資格情報や未検証の競合制御に依存せず閉じられる。ローカル adapter の保証と live provider の保証は分けて記録する。

## Integration responsibility

[Integration register](../notes/phase-1-integration-register.md) で owner、目的、入力・出力、契約案と参照版、TKL 側の検証、外部未確認保証、未確定事項、次の作業を管理する。TKW、TEAI/OpenClaw、KnowledgeHub、Google adapter、CML/Cozy/CNCF と必要なら BoK が対象となる。

クローズ時には、この repository に版を固定した schema / fixture / 受入例を含む提案 package を残し、必要な外部開発を具体的に提案する。外部 owner の受入・実装や別 repository への書き込み、メッセージ送信はクローズの前提にしない。

## Changed records and assurance

- [Phase 1](../phase/phase-1.md) と [checklist](../phase/phase-1-checklist.md) を TKL-local completion に改定する。
- README、application model、backend/context note、architecture、CML scaffold の責務・初期経路を整合させる。
- 過去の設計・レビュー journal は履歴として保持し、本決定を現在の Phase scope authority とする。

本記録は実行範囲の修正であり、実装、live integration、検証、独立 review や Phase 完了を主張しない。改定後の authority に対して元の Phase invocation の開始ゲートを再評価する。
