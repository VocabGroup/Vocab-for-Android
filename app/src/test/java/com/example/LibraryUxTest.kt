package com.example

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.data.entity.Deck
import com.example.ui.screens.FoldersManagementScreen
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers="w412dp-h892dp-420dpi",sdk=[36])
class LibraryUxTest {
 @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
 @Test fun courseGroupsOpenInlineAndCourseShareSelectsOnlyItsFolders(){
  val decks=listOf(Deck(id=1,name="搜尋單字",category="日常"),Deck(id=2,name="生活會話",category="日常"),Deck(id=3,name="第一課",category="英文"))
  compose.setContent{MyApplicationTheme{FoldersManagementScreen(decks,emptyList(),{_,_,_,_->},{},{},onOpenCardList={},onJumpToStudy={},onJumpToQuiz={})}}
  compose.onNodeWithText("搜尋單字").assertDoesNotExist()
  compose.onNodeWithTag("course_日常").performClick()
  compose.onNodeWithText("搜尋單字").assertIsDisplayed()
  compose.onNodeWithText("第一課").assertDoesNotExist()
  compose.onRoot().captureRoboImage("build/outputs/library-ux.png")
  compose.onNodeWithText("分享課程").performClick()
  compose.onNodeWithText("已選 2 個資料夾 · 0 個單字").assertIsDisplayed()
 }
 @Test fun fileImportOpensPreviewEvenAfterHeaderIsScrolledAway(){
  val decks=(1..30).map{Deck(id=it.toLong(),name="資料夾$it",category="日常")};var imports=0
  compose.setContent{MyApplicationTheme{FoldersManagementScreen(decks,emptyList(),{_,_,_,_->},{},{},onOpenCardList={},onJumpToStudy={},onJumpToQuiz={},onImportShare={folders,_,_->imports=folders.size;Result.success(listOf(99L))})}}
  compose.onNodeWithTag("course_日常").performClick()
  compose.onNodeWithTag("folder_list").performScrollToIndex(30)
  compose.onNodeWithTag("add_folder_fab").performClick()
  compose.onNodeWithText("匯入分享檔案").performClick()
  val fixture=File(compose.activity.cacheDir,"test-share.json").apply{writeText("""{"format":"vocab-folder-share","version":1,"decks":[{"name":"匯入測試","course":"英文","cards":[{"word":"apple","definition":"蘋果"}]}]}""")}
  compose.runOnIdle{
    val saved=android.os.Bundle();compose.activity.activityResultRegistry.onSaveInstanceState(saved)
    val keys=requireNotNull(saved.getStringArrayList("KEY_COMPONENT_ACTIVITY_REGISTERED_KEYS"))
    val codes=requireNotNull(saved.getIntegerArrayList("KEY_COMPONENT_ACTIVITY_REGISTERED_RCS"))
    val launched=requireNotNull(saved.getStringArrayList("KEY_COMPONENT_ACTIVITY_LAUNCHED_KEYS")).last()
    compose.activity.activityResultRegistry.dispatchResult(codes[keys.indexOf(launched)],Activity.RESULT_OK,Intent().setData(Uri.fromFile(fixture)))
  }
  compose.waitUntil(10000){compose.onAllNodesWithText("確認匯入").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("確認匯入").assertIsDisplayed().performClick()
  compose.waitUntil(10000){imports==1}
 }
 @Test fun fourTabNavigationRetainsQuizArticlesAndAi(){
  var page by mutableIntStateOf(0);var ai=0
  compose.setContent{MyApplicationTheme{Surface{MainScreen(page,{page=it},{Text("今日內容")},{Text("教材內容")},{Text("翻卡內容")},{Text("測驗內容")},libraryToolContent={Text("文章內容")},onOpenAssistant={ai++})}}}
  compose.onNodeWithTag("nav_tab_study").performClick()
  compose.onNodeWithTag("nav_tab_quiz").performClick()
  compose.onNodeWithText("測驗內容").assertIsDisplayed()
  compose.onNodeWithTag("nav_tab_folders").performClick()
  compose.onNodeWithTag("nav_tab_library_tool").performClick()
  compose.onNodeWithText("文章內容").assertIsDisplayed()
  compose.onNodeWithTag("nav_tab_ai").performClick()
  compose.runOnIdle{assertEquals(1,ai)}
  compose.onRoot().captureRoboImage("build/outputs/glass-ux.png")
 }
 @Test fun generatedQrDecodesToTheExactShareUrl(){
  val url="https://vocab-adam.pages.dev/share#"+"a".repeat(48)
  val bitmap=com.example.data.importer.ShareLinks.qr(url)
  val pixels=IntArray(bitmap.width*bitmap.height);bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
  val source=com.google.zxing.RGBLuminanceSource(bitmap.width,bitmap.height,pixels)
  val decoded=com.google.zxing.qrcode.QRCodeReader().decode(com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source)))
  assertEquals(url,decoded.text)
  org.junit.Assert.assertTrue(runCatching{com.example.data.importer.ShareLinks.qr("https://evil.invalid/share#"+"a".repeat(48))}.isFailure)
 }
}
