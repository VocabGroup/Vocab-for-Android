# 全頁面設計套用核對

參考：`vocab-rebuilt-experience.html` 的頁面結構，套用使用者確認的淺藍配色，
並保留點擊翻卡、左滑沒記住、右滑記住。每列核對的是一個功能族群，
包含其表單、選單、預覽、處理中、失敗重試、完成與空白狀態。

## 必須保留

- 原有資料實體、ID、關聯、學習／測驗進度與備份格式。
- 原有 callback 與權限檢查，AI 修改前確認、失敗不離開預覽、復原。
- 既有 Android 文法／寫作內容。PWA 的 AndroidArchive 不可丟棄。
- 相機、相簿多選、檔案、掃描裁剪、連結、文字與分享檔匯入。
- 全部測驗模式、題數選擇、暫停、接續、紀錄、朗讀與背景控制。
- 課程與資料夾分享、七天期限、預覽、重複檢查與接收流程。

## 畫面對照

| 功能族群 | Android 來源 | PWA 來源 | 版面要點 |
|---|---|---|---|
| 初次開啟與安裝 | OnboardingGoalScreen | OnboardingScreen、BrowserExperience、SafariInstallAnimation | 清楚主動作、目標／提醒選擇、平台安裝教學 |
| 今天與紀錄 | DashboardScreen、StudyCalendarScreen | HomeView、學習統計 | 當下任務、教材單字、最近教材、接續進度、可展開統計 |
| 課程與資料夾 | FoldersManagementScreen | FoldersView、CourseCoverDialog | 課程封面、全部清單、物件層級操作 |
| 單字與多選 | CardListScreen、VocabularyQualityScreen | ManageCardsSheet、CardActionMenu | 可讀清單、固定操作區、篩選與批次工具 |
| 新增與編輯 | AddEditCardScreen | CardSheet | 整份編輯器、定位目標、欄位層次、草稿與安全區 |
| 查字 | WordLookupScreen | WordLookupScreen | 頂端搜尋、來源、音標、例句、歷史、加入位置 |
| 翻卡 | StudyHubScreen、FlashcardReviewScreen、FlipCard | HubView、StudySheet、FlipCard | 選範圍、接續、左右滑、撤回、朗讀 |
| 測驗 | QuizGamesScreen | AdvancedQuizSheet | 分類題型、題數、進度、答題、暫停與結果 |
| 掃描與匯入 | PhotoOcrScreen、ExternalImportScreen | ScanSheet、DocumentScanner、LiveDocumentCamera、ImportSheet、ImportDeviceHelp | 分階段、圖片主工作區、多頁佇列與校對 |
| 文法 | GrammarLibraryScreens、GrammarScreens、GrammarFlowScreens、GrammarSearchScreen | AndroidArchive 既有文法資料 | 筆記清單、編輯、句型／例句、練習／測驗與弱點 |
| 閱讀與寫作 | ReadingWritingLibraryScreens、InteractiveReadingScreen、GrammarWritingCheckScreen | AiSheet 文章、AndroidArchive 寫作資料 | 教材列表、閱讀正文、查字、覆蓋與修改確認 |
| AI | AiAssistantScreen、AiGenerationLoadingScreen | AiSheet | 輕量對話、範圍、附件、串流、可取消、修改預覽 |
| 分享 | FolderSharingPanel | FolderSharing、ShareLinkDialog、share/page | 選擇、預覽、檔案／網址、QR、接收與撤回 |
| 設定 | SettingsScreen | SettingsScreen、WebDisplaySettings | 分組列、子頁、供應商／Key、資料、更新與顯示 |
| 共用 chrome | MainScreen、ModernUi、EditorialUi、LiquidDock | ParityUi、SheetFrame、LiquidNavigation | 統一標頭、清單、表單、底部工具與可拖曳泡泡 |

## 驗證方式

先確認資料與動作入口，再以手機窄版、一般手機、桌面、深色、鍵盤開啟
檢查主要路徑。截圖及互動測試使用合成教材，不能將使用者資料或金鑰放入測試。
此表是工作範圍；未經實作與驗證的功能不能標示為已完成。
