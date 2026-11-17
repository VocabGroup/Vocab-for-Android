package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.entity.*
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ModernScreensTest {
    @get:Rule val compose = createComposeRule()
    @Test fun incomingSharedFolderIsReadOnlyUntilGetIsPressed(){
        var acquired=0
        val folder=com.example.data.importer.SharedFolder("朋友單字","日常","","#DBEEFA",listOf(Flashcard(deckId=0,word="journey",definition="旅程")))
        compose.setContent{MyApplicationTheme{SharedMaterialPreview("fixture",{}, {acquired++},load={com.example.data.importer.ReceivedShare(listOf(folder),Long.MAX_VALUE,"folder","朋友單字")})}}
        compose.onNodeWithText("journey").assertIsDisplayed()
        compose.onNodeWithContentDescription("單字操作").assertDoesNotExist()
        compose.onNodeWithText("journey").assertHasNoClickAction()
        compose.onNodeWithText("取得").performClick()
        compose.runOnIdle{assertEquals(1,acquired)}
        compose.onRoot().captureRoboImage("build/outputs/shared-folder-360.png")
    }
    @Test fun customCoverColorCanBePickedWithoutSavingUntilConfirmed(){
        val context=androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val store=com.example.data.settings.CourseCoverStore(context);store.clear();store.set("日常","#DBEEFA")
        var saved=0
        compose.setContent{MyApplicationTheme{CourseCoverDialog("日常",store,{}, {saved++})}}
        compose.onNodeWithContentDescription("自訂封面顏色").assertIsDisplayed().performClick()
        compose.onNodeWithTag("cover_color_hex").performTextClearance()
        compose.onNodeWithTag("cover_color_hex").performTextInput("#123ABC")
        compose.onNodeWithText("使用此顏色").performClick()
        compose.runOnIdle{assertEquals("#DBEEFA",store.value("日常"))}
        compose.onNodeWithText("晴").assertDoesNotExist()
        compose.onNodeWithText("儲存").performClick()
        compose.runOnIdle{assertEquals("#123ABC",store.value("日常"));assertEquals(1,saved)}
    }
    private val note = GrammarNote(id = 1, title = "too … to", course = "高中第一冊", folder = "L1", summary = "太…而無法…", structure = "too + adjective + to + V")

    @Test fun grammarHomeLight() {
        compose.setContent {
            MyApplicationTheme(darkTheme = false) {
                Surface(color = MaterialTheme.colorScheme.background) { GrammarDashboardScreen(listOf(note), {}, {}, {}, {}, {}) }
            }
        }
        compose.onNodeWithText("今天的文法").assertIsDisplayed()
        compose.onRoot().captureRoboImage("src/test/screenshots/redesign-grammar-home.png")
    }

    @Test fun grammarLibrariesDarkAndOverflowActions() {
        var edited = 0
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface {
                    GrammarLibrariesScreen(
                        libraries = listOf(GrammarLibrary(id = 1, name = "L1", course = "高中第一冊")), notes = listOf(note), highlightedAction = "manage",
                        onAdd = { _, _, _, _ -> }, onUpdate = { edited++ }, onDelete = {}, onReorder = {}, onManage = {}, onLearn = {}, onQuiz = {})
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/redesign-grammar-library-dark.png")
        compose.onNodeWithContentDescription("更多操作").performClick()
        compose.onNodeWithText("編輯", useUnmergedTree = true).performClick()
        compose.onNodeWithText("編輯文法庫").assertIsDisplayed()
        compose.onNodeWithText("儲存").performClick()
        compose.runOnIdle { assertEquals(1, edited) }
    }

    @Test fun emptyGrammarEditorSupportsWholePageSwipe() {
        compose.setContent {
            MyApplicationTheme(darkTheme = false) {
                GrammarEditorScreen(note = null, questions = emptyList(), patterns = emptyList(), examples = emptyList(), courses = listOf("高中第一冊"), folders = listOf("L1"), initialCourse = "高中第一冊", initialFolder = "L1", onGenerateAi = { GrammarDraft() }, onScanImages = { GrammarDraft() }, onSave = { _, _, _ -> }, onBack = {})
            }
        }
        compose.onNodeWithText("句型", useUnmergedTree = true).performClick()
        compose.onNodeWithText("新增第一組句型").assertIsDisplayed()
        compose.onRoot().captureRoboImage("src/test/screenshots/redesign-editor.png")
        compose.onRoot().performTouchInput { swipe(androidx.compose.ui.geometry.Offset(right - 10f, centerY + 150f), androidx.compose.ui.geometry.Offset(left + 10f, centerY + 150f)) }
        compose.onNodeWithText("新增第一題").assertIsDisplayed()
    }

    @Test fun folderPickerHandlesLongLazyLists() {
        var picked = ""
        compose.setContent {
            MyApplicationTheme {
                Surface { Text("背景畫面") }
                ModernAlertDialog(onDismissRequest = {}, title = { Text("選擇資料夾") }, text = {
                    LazyColumn { items((1..30).toList()) { id -> ModernListRow("L$id", "高中第一冊", onClick = { picked = "L$id" }) } }
                }, confirmButton = {}, dismissButton = { TextButton(onClick = {}) { Text("取消") } })
            }
        }
        compose.onNodeWithText("L1").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("L1", picked) }
        compose.onRoot().captureRoboImage("src/test/screenshots/redesign-folder-picker.png")
    }
}
