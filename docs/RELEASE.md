# 發佈 SOP

外掛 ID：`com.h8000572003.intellijplugin`。發佈由 GitHub Actions 的 `Release` workflow（`.github/workflows/release.yml`）完成：推送 `v<版本>` tag 即自動測試、驗證、簽章、上傳 JetBrains Marketplace，並建立附帶外掛 zip 的 GitHub Release。

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

## 每次發佈

1. 從 `main` 開分支，修改：
   - `gradle.properties` 的 `pluginVersion`（例如 `1.11.0`）
   - `src/main/resources/META-INF/plugin.xml` 的 `<change-notes>`，加入此版本的變更
   - 功能有變動時一併更新 `README.md`
2. 發 PR，等 `Build` workflow 通過後合併到 `main`。
3. 在 `main` 最新 commit 上打 tag 並推送（tag 必須等於 `v` + `pluginVersion`）：
   ```bash
   git checkout main && git pull
   git tag v1.11.0
   git push origin v1.11.0
   ```
4. 到 GitHub **Actions → Release** 確認執行成功。流程依序為：
   1. 檢查 tag 與 `pluginVersion` 一致
   2. `test`、`buildPlugin`、`verifyPlugin`（以 Marketplace 建議的多個 IDE 版本檢查相容性）
   3. `signPlugin`（有設定憑證時）
   4. 上傳 zip 為 workflow artifact
   5. `publishPlugin` 上傳 Marketplace
   6. 建立 GitHub Release，附上 zip 並自動產生 release notes
5. Marketplace 會進行審核（通常 1～2 個工作天），可在外掛頁面的 **Versions** 查看狀態，通過後使用者即可更新。

### 預發佈（beta）
版本加上後綴即發佈到對應頻道，例如 `pluginVersion=1.11.0-beta.1`、tag `v1.11.0-beta.1` 會發佈到 `beta` 頻道，GitHub Release 標示為 pre-release。

使用者要安裝 beta 版：**Settings → Plugins → ⚙ → Manage Plugin Repositories** 加入 `https://plugins.jetbrains.com/plugins/beta/list`。

## 只產出檔案（不發佈）

任一方式取得 `values-<版本>.zip`：

- **每次 push / PR**：`Build` workflow 執行頁面下方 **Artifacts** 的 `plugin-zip`。
- **手動執行 Release**：Actions → Release → **Run workflow**，`publish` 不勾選；會完整測試與驗證，並產出 artifact `plugin-<版本>`（有設定憑證時含 `-signed.zip`），不上傳 Marketplace。
- **本機**：`./gradlew buildPlugin`，檔案在 `build/distributions/`。

安裝 zip：IDE **Settings → Plugins → ⚙ → Install Plugin from Disk…** 選擇 zip（下載的 artifact 需先解壓出裡面的外掛 zip）。

## 失敗處理

| 狀況 | 處理 |
|---|---|
| `Tag vX does not match pluginVersion` | 刪除 tag（`git push --delete origin vX && git tag -d vX`），修正版本或改打正確的 tag |
| 測試或 `verifyPlugin` 失敗 | 修正後發新 PR；刪除舊 tag，合併後重新打 tag |
| `Secret PUBLISH_TOKEN is not set` 或權杖失效 | 重新產生權杖並更新 secret，於失敗的 run 點 **Re-run jobs** |
| Marketplace 回應版本已存在 | 同一版本號不能重複上傳，提高 `pluginVersion` 重新發佈 |
| Marketplace 審核退回 | 依審核意見修正，提高版本號重新發佈 |
