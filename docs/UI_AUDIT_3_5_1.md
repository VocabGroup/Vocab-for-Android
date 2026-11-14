# 3.5.1 UI audit

This pass preserves data models, navigation callbacks, import/export formats and learning rules. Fixtures contain synthetic material only.

## Android

`PageAuditTest` renders all 38 public screen composables (including legacy entry points) at 360 × 640 with 1.3 font scale and 844 × 390 in dark mode. The generated captures are local build outputs, not user data or release assets.

Covered families: today/history, course/folder library, card list/quality/editor, lookup, study/flip, quizzes, scan/external import, grammar libraries/detail/editor/import/learning/quizzes/results/search, reading/writing, AI/loading, onboarding and settings. Screens with provider/network state use fixtures; this does not certify external providers or physical camera hardware.

Specific interaction checks verify long grammar content can scroll without losing the next action, grammar answer submission remains above injected IME insets, and the shared compact form retains its entered name and reachable confirmation. All eight settings subpages are opened and returned from. Existing tests continue covering sharing, navigation, scanning, quiz progress and editing.

Fixes: missing IME insets on form/search/quiz scaffolds; non-scrollable grammar learning body; cramped dialog headings/actions; legacy grammar reading panels/list surfaces; duplicate reading explanation; disappearing AI-fill label on narrow editor headers. Shared dialogs retain caller secure-window and system-inset policies.

No physical Android device is connected. Keyboard validation uses Android/Compose inset dispatch under Robolectric; actual vendor keyboards, cameras and system bars still need device verification.

## PWA

WebKit and Chromium checks cover 320 px width, portrait/landscape, desktop workspace, dark mode, keyboard-like visible viewport offsets and short dialogs. The keyboard tests validate layout geometry, not a physical iPhone keyboard.

Covered: today/course/folder views, overflow menus, edit/rename, card editor/notes/batch/delete/move/add, search, AI composer, scan camera/manual corners/queue/retry, import help, all eight settings subpages, onboarding/install animation, share preview/confirmation/JSON/revocation states and quiz custom count/resume. Native-only grammar/writing data remains preserved in AndroidArchive; this release does not introduce new web editors for it.

Fixes: folder FAB obstructing the last row (create/import moved into document flow); menu clipping (portal + visible viewport bounds); nested dialogs not following keyboard viewport; missing dialog focus/Escape behavior; long labels, small-height sheets and safe-area header padding.

Core tests include menu-boundary cases with anchors outside the visible viewport. Provider requests and share operations in browser regression tests are mocked to avoid uploading personal material or consuming storage quotas.

## Publication hygiene

Only explicit source/test/docs/version files are staged. Existing unrelated README/dictionary edits, local captures, caches, environment files and signing credentials are excluded. Builds use existing GitHub signing secrets; no credentials are added to source.
