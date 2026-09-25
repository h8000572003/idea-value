# IDEA 外掛 Values

## 目的

使用code generator方式，增快常用程式開發方式(使用ALT+ENTER)
![set name value.gif](set%20name%20value.gif)


## Generate（游標位置按 ALT+ENTER）
- Generate fields by get/is method name：游標在類別內空白處，依類別的 public get/is 方法產生對應的成員變數（已存在的欄位略過）
- Generate all setters cache name values：游標在區域變數名稱上，呼叫該類別（含父類別）所有 setter 並填入範例值；數字類依屬性名稱重複使用同一個數字
- Generate all setters no cache values：同上，但數字類每次產生新的流水號
- Generated set/get based on parameter 1 as parameter 2：游標在 `void map(Target t, Source s)` 方法內，產生 `t.setX(s.getX())`（只列出兩邊都有的屬性）
- Generated set/get based on parameter 1 return value：游標在 `Target toTarget(Source s)` 方法內，建立回傳物件、複製屬性並回傳
- Generated assert based on parameter 1 as parameter 2：游標在 `void check(T expected, T actual)` 方法內，為每個 getter 產生 `assertEquals`（陣列用 `assertArrayEquals`）
- Generate fix sql injection related declare parameters：將 SQL 字串串接的值改為綁定參數（:name 或 ?），並於該行前產生宣告與放入參數的程式
- Generate fix sql injection related without declare parameters：同上，但不產生參數集合的宣告
  - 引號內的值（含 LIKE '%...%'）整段成為一個參數，常數與累加中的 SQL 變數（sql = sql + ...）保留不綁定
  - 無法安全綁定的位置（ORDER BY / 表格、欄位名稱、IN (...) 整串、黏在 SQL 字詞上的值）會顯示錯誤提示而不修改程式
  - 綁定樣式與參數集合名稱在 Settings → Tools → Values Settings 設定
- Add 'value != null' check：游標在 if 條件中 `value.method()` 的 `value` 上，於條件前加上 `value != null &&`
- String / StringBuilder 快速文件（Ctrl+Q）：顯示變數累加的內容，保留 if / else / switch / 迴圈結構

## 開發與發佈

- 開發指令與架構：見 `CLAUDE.md`
- 發佈流程：見 [docs/RELEASE.md](docs/RELEASE.md)
