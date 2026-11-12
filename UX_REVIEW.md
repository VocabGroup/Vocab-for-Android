# 3.3.0 UI / UX 檢查範圍

本版依概念稿方向實作，不宣稱每頁已像素級還原。Android 與 PWA 功能仍不完全相同；沒有完成的網頁文法／作文功能不能以靜態畫面冒充。

## 共用規則

- 閱讀內容維持純色，減少背景彩色漸層；玻璃限導覽與操作層，不套滿單字／文法正文。
- Android 共用按鈕與卡片使用更圓潤的外形；維持至少 48dp 的主要操作高度。
- 導覽列 64dp，高透明、細亮邊與柔和陰影；Android 12 以下不採 RenderEffect，改為較不透明的降級顯示。
- 教材庫的分享狀態與選檔 launcher 固定掛載，不能放在可能卸載的 LazyColumn item。
- PWA 狀態提示不得攔截清單選單；模態內容與底部鍵盤區保持獨立。
- 原始資料與測驗草稿不放入 GitHub；分享內容由白名單輸出，不含進度與 API Key。

## 分頁盤點

| 模組／來源 | 本次修改與驗證 | 限制 |
|---|---|---|
| MainScreen | 四入口、學習／測驗切換、文章／作文保留、玻璃標示；Robolectric 導覽測試 | 玻璃非 Apple 原生材質，效能待真機 |
| Dashboard／StudyHub | 保留今日進度與原有學習入口，共用操作形狀更新 | 首頁完整概念稿排版與最近使用仍非完整重做 |
| FoldersManagement | 課程收合、同課程排序、課程分享、捲動後匯入；互動與截圖測試 | 置頂／全域教材搜尋尚未新增 |
| CardList／AddEditCard | 共用卡片／按鈕更新，既有編輯安全區測試保留 | 未承諾每個舊子畫面重排為概念稿 |
| QuizGames | 題數與接續；配對盤面／發音題位置存檔；輸入中斷接續測試 | 錄音辨識與 Android 程序被系統終止待實機 |
| PhotoOcr／ExternalImport | 保留多圖、進度、錯誤與匯入預覽；既有回歸 | 相機及 Play Services 待實機 |
| GrammarDashboard／Libraries／Editor／Detail／Quiz | 共用元件更新；保留既有文法互動／截圖測試與底部安全區 | 文法測驗不是本版單字測驗草稿功能；更細分排版待後續 |
| AiAssistant／InteractiveReading | 保留 AI 確認、可取消請求、閱讀與回覆流程 | 不使用真實付費 Key 測試 |
| ReadingWritingLibrary／Calendar／Quality／Settings／Onboarding | 入口與共用元件檢查，功能保留 | 未逐一驗證所有狀態與各型實體手機 |
| PWA share／FolderSharing | 雲端分享、QR、跨頁匯入確認、到期／錯誤；API 與瀏覽器測試 | KV 最終一致性與免費配額限制見 SHARING_SETUP.md |
| PWA AdvancedQuiz | 題數、序列與答案草稿、配對盤面、完成清除；解析與瀏覽器測試 | 實際儲存能力依瀏覽器；失敗須顯示提示 |
| PWA 其餘入口 | 保留既有手機瀏覽器回歸案例，純色背景與透明導覽 | 不等於 Android 新分組／四入口的完整移植 |

截圖與瀏覽器 traces 只存 build／test-results，不公開個人教材。測試結果以實際執行輸出為準；此清單不替代真機驗收或無障礙稽核。
