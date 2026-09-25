# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 語言

思考與回應一律使用**繁體中文**（程式碼、識別字、commit 訊息維持英文慣例即可）。

## 專案概要

IntelliJ IDEA 外掛「Values」（plugin id `com.h8000572003.intellijplugin`），以 ALT+ENTER intention 產生常用 Java 程式碼：setter 填值、物件間 mapper / assertion、依 getter 產生欄位、`if` null 檢查、SQL injection 修正等。功能清單見 `README.md`，版本變更記錄在 `plugin.xml` 的 `<change-notes>`。

- 開發主線是 `main`；`master` 是舊分支，不要以它為基礎。
- 版本號在 `build.gradle.kts` 的 `version`，同時要在 `plugin.xml` 的 `<change-notes>` 加上一筆。

## 常用指令

Gradle 9 wrapper、JDK 21 執行（編譯目標 Java 17）。

```bash
./gradlew test                       # 全部測試（根專案 + sql-core）
./gradlew :sql-core:test             # 只跑純 Java 的 SQL 核心測試（不需下載 IDE）
./gradlew :sql-core:test --tests 'com.h8000572003.values.sql.SqlParameterizerTest$QuotedValues'
./gradlew test --tests com.h8000572003.values.SqlInjectionFixIntentionTest
./gradlew buildPlugin                # 產出外掛 zip 至 build/distributions
./gradlew runIde                     # 開一個載入外掛的 IDE 沙盒
```

- 根專案的編譯與測試需要從 JetBrains 儲存庫下載 IntelliJ（版本由 `gradle.properties` 的 `platformVersion` 決定）。無法連線 JetBrains 時只有 `:sql-core` 能在本機建置，其餘交給 GitHub Actions 的 `Build` workflow（`./gradlew test buildPlugin`）驗證。
- 沒有 lint 設定；`code_quality.yml` 在 CI 跑 Qodana。

## 架構

### 兩個 Gradle 專案
- **根專案**：IntelliJ 外掛本體（IntelliJ Platform Gradle Plugin 2.x）。所有 intention 在 `src/main/resources/META-INF/plugin.xml` 註冊，新增 intention 必須在此登記。
- **`sql-core`**：純 Java、不依賴 IntelliJ API 的 SQL 改寫邏輯（JUnit 5）。刻意獨立出來，讓核心邏輯能快速以 TDD 開發；外掛以 `implementation(project(":sql-core"))` 打包。

### SQL injection 修正的資料流
1. `SqlInjectionFixIntention`（外掛層，`GenerateMapFromStringWith{,out}DeclareAction` 兩個子類只差在是否宣告參數集合）找到游標所在的字串串接。
2. `SqlConcatenation` 把 PSI 運算元轉成 `SqlPart`：字串字面值 → `Literal`；編譯期常數 → `Constant`（保留程式碼、不綁定）；`sql = sql + ...` 中累加的變數 → `Verbatim`；其他 → `Expression`（要綁定的值）。
3. `SqlParameterizer.rewrite` 逐字元追蹤 SQL 單引號狀態：引號內含值的整段變成一個參數（例如 `'%" + x + "%'` → `?`，值為 `"%" + x + "%"`），引號外的值直接綁定；識別字位置、整串 `IN (...)`、黏在 SQL 字詞上的值則丟出 `UnsupportedSqlException`，外掛層轉為錯誤提示且不改動程式。
4. `ParameterCodeGenerator` 產生放入參數的敘述（型別用完整名稱，外掛層再 `shortenClassReferences`），插在原敘述之前。

綁定樣式（`:name` + `Map` 或 `?` + `List`）與參數集合變數名稱來自設定頁：`configurable/MyPluginSettings`（application service）與 `ValueConfigurable`（Tools > Values Settings）。

### 其他 intention
- `BaseGenerateAllSetterFieldNameAction` + `commons/Assignment`：依參數型別產生 setter 值；數字值策略由 `KeppNameNumberValueStrategy`（同名重複同值）或 `NotKeepValueStrategy`（流水號）決定。
- 多數舊 intention 把 PSI 或 `SmartPsiElementPointer` 存在欄位、於 `isAvailable` 設定、`invoke` 使用。這種有狀態寫法會讓 IntelliJ 的 intention 預覽失效；新程式請比照 `SqlInjectionFixIntention` 保持無狀態，並給 `getFamilyName()` 非空值。

## 測試

- 外掛層測試繼承 `BasePlatformTestCase`（JUnit 3 風格，方法名以 `test` 開頭），測試環境**沒有 JDK**：`String` 等 JDK 類別無法解析，所以 `java.util.Map` 之類的完整名稱不會被縮短，常數求值要能在型別無法解析時運作。測試資料盡量避免依賴 JDK 類別。
- 修改設定值的測試要在 `tearDown` 還原 `MyPluginSettings`（application 層級，跨測試共用）。
