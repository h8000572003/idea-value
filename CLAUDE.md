# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 語言

思考與回應一律使用**繁體中文**（程式碼、識別字、commit 訊息維持英文慣例即可）。

## 專案概要

IntelliJ IDEA 外掛「Values」（plugin id `com.h8000572003.intellijplugin`），以 ALT+ENTER intention 產生常用 Java 程式碼：setter 填值、物件間 mapper / assertion、依 getter 產生欄位、`if` null 檢查、SQL injection 修正，另有 String/StringBuilder 累加內容的快速文件。功能說明見 `README.md`。

- 開發主線是 `main`；`master` 是舊分支，不要以它為基礎。
- 版本號在 `gradle.properties` 的 `pluginVersion`；每次升版同時在 `plugin.xml` 的 `<change-notes>` 加一筆。發佈流程見 `docs/RELEASE.md`（推送 `v<pluginVersion>` tag 觸發 `Release` workflow）。

## 常用指令

Gradle 9 wrapper、JDK 21 執行（編譯目標 Java 17）。

```bash
./gradlew test                       # 全部測試（根專案 + core）
./gradlew :core:test                 # 只跑純 Java 的核心測試（不需下載 IDE，數秒完成）
./gradlew :core:test --tests 'com.h8000572003.values.sql.SqlParameterizerTest$QuotedValues'
./gradlew test --tests com.h8000572003.values.GenerateSourceMapperActionTest
./gradlew buildPlugin                # 外掛 zip 產生於 build/distributions
./gradlew verifyPlugin               # 以 Marketplace 建議的多個 IDE 版本檢查相容性（會下載多個 IDE）
./gradlew runIde                     # 開一個載入外掛的 IDE 沙盒
```

- 根專案的編譯與測試需要從 JetBrains 儲存庫下載 IntelliJ（版本由 `gradle.properties` 的 `platformVersion` 決定）。無法連線 JetBrains 時只有 `:core` 能在本機建置，其餘交給 GitHub Actions 的 `Build` workflow（`./gradlew test buildPlugin`，測試逐項列在 log 中）驗證。
- 沒有 lint 設定；`code_quality.yml` 在 CI 跑 Qodana。

## 開發方式

採 TDD：先寫測試、確認失敗，再實作。邏輯盡量放進 `core` 以便在本機快速跑紅綠循環；外掛層只負責 PSI 與插入程式碼，測試在 CI 執行。

## 架構

### 兩個 Gradle 專案
- **`core`**：純 Java、不依賴 IntelliJ API（JUnit 5）。外掛以 `implementation(project(":core"))` 打包。
  - `sql`：`SqlParameterizer` 把字串串接的 SQL 改寫成綁定參數。
  - `codegen`：`PropertyNames`（JavaBeans 命名）、`SampleValues` + `NumberSequences`（setter 範例值）、`CodeGenerators`（setter 呼叫、屬性複製、assertion、欄位宣告）。
  - `doc`：`AccumulatedContent` 把累加片段（`Fragment` + 所在的 if/switch/迴圈 `Level` 路徑）渲染成虛擬碼與 HTML。
- **根專案**：IntelliJ 外掛本體。所有 intention 與 documentation provider 在 `src/main/resources/META-INF/plugin.xml` 註冊；類別名稱是既有使用者設定的識別，不要改名。

### 外掛層的共用元件
- `PsiAccessors`：從 `PsiClass` 找 public instance getter/setter（含繼承、排除 `Object`），轉成 core 的 `Accessor`。
- `CodeInserter`：把 core 產生的程式碼字串以 PSI 插入（敘述或欄位），再 `shortenClassReferences` 與 reformat。所以 core 產生的型別可用完整名稱（例如 `java.math.BigDecimal`）。
- `CaretStatementsIntention`：游標在方法內空白處、依方法簽章產生敘述的 intention 基底（mapper、assertion）。

### SQL injection 修正的資料流
`SqlInjectionFixIntention`（`GenerateMapFromStringWith{,out}DeclareAction` 只差在是否宣告參數集合）→ `SqlConcatenation` 把運算元轉成 `SqlPart`（字面值 `Literal`、編譯期常數 `Constant`、`sql = sql + ...` 累加的變數 `Verbatim`、其餘 `Expression`）→ `SqlParameterizer` 追蹤單引號狀態產生綁定參數，無法安全綁定時丟 `UnsupportedSqlException`（外掛層轉為錯誤提示、不改程式）→ `ParameterCodeGenerator` 產生放參數的敘述，插在原敘述之前。綁定樣式與集合變數名稱來自 `configurable/MyPluginSettings`（設定頁 `ValueConfigurable`）。

### 慣例
- Intention 必須無狀態：不要把 PSI 存在欄位（會破壞 intention 預覽）；`getFamilyName()` 不可為空字串。
- 跨呼叫的狀態（例如 setter 的 `NumberSequence`）標註 `@FileModifier.SafeFieldForPreview`，並在預覽時（`IntentionPreviewUtils.isIntentionPreviewActive()`）改用 `copy()`。

## 測試

- 外掛層測試繼承 `JavaTestCase`（`LightJavaCodeInsightFixtureTestCase`，以執行測試的 JDK 作為專案 JDK，所以 `String` 等 JDK 類別可解析、import 會被縮短）。JUnit 3 風格，方法名以 `test` 開頭。
- `JavaTestCase.assertCode` 比對結果時忽略空行與行尾空白（格式化器留下的空行不穩定）。
- 修改設定值的測試要在 `tearDown` 還原 `MyPluginSettings`（application 層級，跨測試共用）。
