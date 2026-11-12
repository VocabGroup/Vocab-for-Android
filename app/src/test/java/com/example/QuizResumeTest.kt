package com.example

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.data.entity.Deck
import com.example.data.entity.Flashcard
import com.example.ui.screens.QuizGamesScreen
import com.example.ui.theme.MyApplicationTheme
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers="w412dp-h892dp-420dpi",sdk=[36])
class QuizResumeTest {
 @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
 @Test fun partialTypedAnswerAndCustomQuestionCountResumeWithoutSubmittingTwice(){
  val prefs=compose.activity.getSharedPreferences("quiz_checkpoint_v1",0);prefs.edit().clear().commit()
  val cards=listOf(Flashcard(id=1,deckId=1,word="apple",definition="蘋果"),Flashcard(id=2,deckId=1,word="banana",definition="香蕉"))
  var reports=0
  compose.setContent{MyApplicationTheme{QuizGamesScreen(cards,listOf(Deck(id=1,name="測試",category="英文")),launchDeckId=1,onWrongAnswer={reports++})}}
  compose.onNodeWithText("自訂題數（1–1000）").performTextReplacement("1")
  compose.onNodeWithText("中文填英文 · 2").performClick()
  compose.onNodeWithText("1 / 1").assertIsDisplayed()
  compose.onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("partial")
  compose.onNodeWithTag("exit_game_midway_button").performClick()
  compose.onNodeWithText("接續上次").assertIsDisplayed()
  val saved=JSONObject(requireNotNull(prefs.getString("session",null)))
  assertEquals(1,saved.getInt("count"));assertEquals("partial",saved.getJSONObject("progress").getString("typed"));assertEquals(1,saved.getJSONObject("progress").getJSONArray("round").length())
  compose.onNodeWithText("接續上次").performClick()
  compose.onNodeWithText("partial").assertIsDisplayed()
  compose.runOnIdle{assertEquals(0,reports)}
 }
}
