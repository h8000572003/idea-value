# 發佈 SOP

外掛 ID：`com.h8000572003.intellijplugin`。發佈由 GitHub Actions 的 `Release` workflow（`.github/workflows/release.yml`）自動完成，分兩種：

| | EAP（搶先版） | 正式版 |
|---|---|---|
| 觸發 | 每次合併（push）到 `main`；只改 `docs/`、`*.md` 時不觸發 | 在 Actions 手動執行 `Release` |
| 版本號 | `1.10.1-eap.4`（下個正式版號 + 距上次正式版的 commit 數） | `1.10.1` |
| Marketplace 頻道 | `eap`，只有加入 EAP 頻道的使用者會收到 | 預設頻道，所有使用者 |
| git tag／GitHub Release | 無 | 自動建立 `v1.10.1` tag 與 Release（附 zip） |

兩種都會先跑 `test`、`buildPlugin`、`verifyPlugin`，任一失敗就不發佈。

## 一次性設定

### 1. Marketplace 權杖（必要）
1. 登入 JetBrains Marketplace，到 **My Tokens**（https://plugins.jetbrains.com/author/me/tokens）產生權杖。
2. GitHub repo → **Settings → Secrets and variables → Actions → New repository secret**，新增 `PUBLISH_TOKEN`。

> Marketplace 規定外掛的**第一個版本**必須從網站手動上傳。若外掛尚未在 Marketplace 上架，先依「只產出檔案」取得 zip，於 Marketplace 網站 **Upload plugin** 上傳一次，之後才能用 workflow 發佈。

### 2. 簽章憑證（建議）
沒有設定時 workflow 會略過簽章，仍可發佈；設定後 Marketplace 會顯示外掛已簽章。

```bash
openssl genpkey -aes-256-cbc -algorithm RSA -out private_encrypted.pem -pkeyopt rsa_keygen_bits:4096
openssl rsa -in private_encrypted.pem -out private.pem
openssl req -key private.pem -new -x509 -days 365 -out chain.crt
```

新增 secrets：

| Secret | 內容 |
|---|---|
| `CERTIFICATE_CHAIN` | `chain.crt` 的完整內容 |
| `PRIVATE_KEY` | `private.pem` 的完整內容 |
| `PRIVATE_KEY_PASSWORD` | 產生 `private_encrypted.pem` 時輸入的密碼 |

憑證有效期為 365 天，到期前重新產生並更新 secrets。私鑰檔案不要提交到 repo。

## 版本號規則

版本號由 `.github/scripts/version.sh` 依 git 歷史計算，**不需要手動改 `gradle.properties`**：

- 以最新的正式版 tag（`vX.Y.Z`）為基準，預設升 patch：`v1.10.0` → `1.10.1`。
- 自上次正式版以來，任一 commit 訊息含 `[minor]` 升 minor（`1.11.0`），含 `[major]` 升 major（`2.0.0`）。手動發佈正式版時也可以在 `bump` 選項直接指定。
- `gradle.properties` 的 `pluginVersion` 是下限：還沒有任何 `v` tag 時，第一個正式版就是這個值；想跳到特定版本（例如 `3.0.0`）時提高它即可。
- EAP 版號小於同號正式版（`1.10.1-eap.4` < `1.10.1`），使用者會正常升級到正式版。

## Change notes

每個版本的 change notes 自動取自上次正式版以來的 **commit 標題**（不含 merge commit），寫進外掛的 `<change-notes>`，也作為 GitHub Release 說明。因此：

- commit 標題請寫成使用者看得懂的變更說明，例如 `Fix setter values for BigDecimal`。
- `plugin.xml` 裡手寫的 `<change-notes>` 只用於本機建置，不必再每次維護。

## 日常流程

1. 開分支開發，發 PR；`Build` workflow 通過後合併到 `main`。
2. 合併後 `Release` workflow 自動發佈 EAP 版。可在 Actions 執行頁的 Summary 看到版本號與 change notes。
3. 要發佈正式版時：GitHub **Actions → Release → Run workflow**，Branch 選 `main`，`bump` 通常選 `auto`，按 **Run workflow**。
4. 正式版完成後會有 `vX.Y.Z` tag 與 GitHub Release；Marketplace 審核（通常 1～2 個工作天）通過後，使用者即可更新。

### 使用者安裝 EAP 版

IDE **Settings → Plugins → ⚙ → Manage Plugin Repositories** 加入 `https://plugins.jetbrains.com/plugins/eap/list`，之後即可收到 EAP 更新。

## 只產出檔案（不發佈）

- **每次 push／PR**：`Build` workflow 執行頁下方 **Artifacts** 的 `plugin-zip`。
- **EAP 或正式版的 run**：Artifacts 的 `plugin-<版本>`（有設定憑證時含 `-signed.zip`）。
- **本機**：`./gradlew buildPlugin`，檔案在 `build/distributions/`。

安裝 zip：IDE **Settings → Plugins → ⚙ → Install Plugin from Disk…** 選擇 zip（下載的 artifact 需先解壓出裡面的外掛 zip）。

## 失敗處理

| 狀況 | 處理 |
|---|---|
| 測試或 `verifyPlugin` 失敗 | 修正後發 PR 合併；新的 push 會重新發佈 EAP |
| `Secret PUBLISH_TOKEN is not set` | 設定 secret。EAP 在未設定時只會警告並略過上傳；正式版會失敗 |
| `Tag vX.Y.Z already exists` | 該版本已發佈過；有新 commit 後再執行，或改選較大的 `bump` |
| Marketplace 回應版本已存在 | 同一版本號不能重複上傳（例如重跑同一個 commit 的 EAP），有新 commit 後會自動產生新版本號 |
| Marketplace 審核退回 | 依審核意見修正，合併後再發佈新版本 |
| 執行 Release 時選了 `main` 以外的分支 | workflow 會拒絕；請選 `main` |

## 使用手冊網頁（GitHub Pages）

`docs/index.html` 是外掛的展示與操作說明頁。一次性設定：repo **Settings → Pages → Build and deployment**，Source 選 **Deploy from a branch**，Branch 選 `main`、資料夾 `/docs`。之後每次合併到 `main` 會自動更新 https://h8000572003.github.io/idea-value/ 。

功能有變動時，請一併更新手冊中的操作說明與前後對照。
