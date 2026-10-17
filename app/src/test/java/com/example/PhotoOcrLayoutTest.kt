package com.example

import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.data.entity.Deck
import com.example.ui.screens.PhotoOcrScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.OcrCardCandidate
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-420dpi", sdk = [36])
class PhotoOcrLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val decks = listOf(Deck(id = 1, name = "搜尋單字", category = "日常"), Deck(id = 2, name = "第二課", category = "英文"))
    @Composable private fun Content(cards: List<OcrCardCandidate> = emptyList(), onImport: (List<OcrCardCandidate>, Long, (Boolean) -> Unit) -> Unit = { _, _, _ -> }) {
        MyApplicationTheme(darkTheme = true) { Surface {
                PhotoOcrScreen(decks = decks, candidates = cards, onImportCandidates = onImport, onBack = {}, aiAvailable = true)
        } }
    }
    @Test fun compactOptionsStayInSameRowAndMenusWork() {
        RuntimeEnvironment.setFontScale(1.3f)
        compose.setContent { Content() }
        val destination = compose.onNodeWithTag("scan_destination").fetchSemanticsNode().boundsInRoot
        val scope = compose.onNodeWithTag("scan_scope").fetchSemanticsNode().boundsInRoot
        assertEquals(destination.top, scope.top, .1f)
        assertTrue(destination.right <= scope.left)
        compose.onNodeWithTag("scan_scope").performClick()
        compose.onNodeWithText("自訂範圍").performClick()
        compose.onNodeWithText("例如：只辨識左欄畫線的主單字").assertIsDisplayed()
        compose.onNodeWithTag("scan_destination").performClick()
        compose.onNodeWithText("英文 › 第二課").performClick()
        compose.onNodeWithText("英文 › 第二課").assertIsDisplayed()
        compose.onNodeWithText("重複單字：略過").assertDoesNotExist()
        compose.onRoot().captureRoboImage("build/outputs/scan-layout-empty.png")
    }
    @Test fun resultsHaveSpaceScrollEditSelectAndImport() {
        RuntimeEnvironment.setFontScale(1.3f)
        var imported = 0
        val cards = (1..18).map { OcrCardCandidate("word$it", "n.", "解釋 $it", "Example $it", exampleTranslation = "例句 $it") }
        compose.setContent { Content(cards) { selected, _, done -> imported = selected.size; done(true) } }
        compose.onNodeWithTag("scan_results").assertIsDisplayed()
        val list = compose.onNodeWithTag("scan_results").fetchSemanticsNode().boundsInRoot
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("Results should occupy most available screen height", list.height > root.height * .45f)
        compose.onNodeWithText("例: Example 1").assertDoesNotExist()
        compose.onNodeWithContentDescription("word1 的操作").performClick()
        compose.onNodeWithText("顯示例句").performClick()
        compose.onNodeWithText("例: Example 1").assertIsDisplayed()
        compose.onNodeWithContentDescription("word1 的操作").performClick()
        compose.onNodeWithText("收起例句").performClick()
        compose.onNodeWithContentDescription("word1 的操作").performClick()
        compose.onNodeWithText("編輯").performClick()
        compose.onNodeWithText("儲存").performClick()
        compose.onNodeWithTag("scan_results").performScrollToIndex(17)
        compose.onNodeWithText("word18").assertIsDisplayed()
        compose.onNodeWithTag("batch_import_button").assertIsDisplayed()
        compose.onNodeWithText("取消全選").performClick()
        compose.onNodeWithTag("batch_import_button").assertIsNotEnabled()
        compose.onNodeWithText("全選").performClick()
        compose.onNodeWithTag("batch_import_button").performClick()
        compose.runOnIdle { assertEquals(18, imported) }
        compose.onRoot().captureRoboImage("build/outputs/scan-layout-results.png")
    }
}
