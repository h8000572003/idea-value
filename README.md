# IDEA 外掛 Values

## 目的

使用code generator方式，增快常用程式開發方式(使用ALT+ENTER)
![set name value.gif](set%20name%20value.gif)


## Generate
- Generate fields by get/is method name：於物件中選空白，根據物件method get/is名稱，產生相對應【成員變數】
- Generate all setters cache name values： 物件根據名稱塞特定根據名稱，若數字類時，根據成員變數名稱產生重複數字
- Generate all setters no cache values：物件根據名稱塞特定根據名稱，若數字類時，將隨機產生流水號且不重複資料
- Generated set/get based on parameter 1 as parameter 2: 物件對應set value根據參數1對應參數2
- Generated assert based on parameter 1 as parameter 2：產生物件之間assertion根據參數1對應參數2