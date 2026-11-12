# 淺藍與 Liquid Glass 更新

Android 與 PWA 共用三種配色方向：晴空藍、冰川藍、矢車菊藍。
PWA 在「設定 → 外觀」選擇；Android 在相同位置切換。

教材入口改為課程封面，再進入課程內的資料夾。「全部資料夾」、
分享課程／資料夾、檔案／網址匯入、排序、重新命名及刪除仍保留。
封面可以選藍色預設或個人圖片；圖片置中裁切成 600 × 760 JPEG。
課程重新命名會保留封面。封面屬於個人外觀，不會放入分享教材。
PWA 封面隨裝置資料及網頁 JSON 備份保存；Android 封面儲存於 App
私有空間，目前不包含在 Android 可攜 JSON 備份內。

導覽採四分頁膠囊與獨立搜尋圓鈕。玻璃材質只用於導覽，選取鏡片以
彈簧移動並在切換時抬起、放大、落下。PWA 支援減少動態及減少透明度偏好；
Android 12 以上使用頁面擷取模糊，舊版使用實色背景確保辨識度。
練習中的翻卡及測驗入口都保留。翻卡仍為點擊 3D 翻面、左滑沒記住、
右滑記住，並保留撤回、接續進度與原有排程。

參考 BitChord 的實際元件結構與動態說明，未直接移植其原始碼：
- https://github.com/kushagrasinghx/BitChord/blob/main/app/src/main/java/com/music/bitchord/ui/components/GlassNavBar.kt
- https://github.com/kushagrasinghx/BitChord/blob/main/app/src/main/java/com/music/bitchord/ui/components/floatingtabbar/FloatingTabBar.kt

驗證入口：
- `web`: `npm run typecheck`, `npm run test:core`, `npm run build:pages`
- `web`: `VOCAB_TEST_ORIGIN=http://127.0.0.1:3000 npx playwright test tests/browser/liquid-ui.spec.ts`
- Android: `gradlew :app:compileDebugKotlin :app:testDebugUnitTest`

這批變更實作上述需求，並非將先前 196 個設計狀態全部移植完成。
