# プロジェクト宣言による Cozy ジェネレーター選択

2026-10-22、CB-ESS3-GENERATOR-SELECTION-001 の修正として、既存の launcher 設定契約を用い、`conf/cozy/launcher.yaml` の `development.runtime.enabled` を `false` にする。Hub / Studio と同じ設定で、プロジェクトが宣言する Cozy `0.3.3-SNAPSHOT` を生成時に選択する意図を記録する。

バージョンは維持し、グローバル開発設定と他のチェックアウトは変更しない。製品コード、公開 API / ABI、セキュリティ、SQL、依存バージョンの変更、および main への反映、push、外部公開は対象外とする。

生成・仕様による検証と受入判断は親ワークフローが別途担当する。本記録は修正の意図であり、検証結果や Phase 完了を表すものではない。
