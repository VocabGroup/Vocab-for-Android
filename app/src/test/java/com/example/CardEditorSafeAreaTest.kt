package com.example
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.SideEffect
import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.example.data.entity.Deck
import com.example.data.entity.Flashcard
import com.example.ui.screens.AddEditCardScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers="w412dp-h892dp-420dpi",sdk=[36])
class CardEditorSafeAreaTest {
 @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
 @Test fun lastCardAndAddButtonRemainReachable(){
  val cards=(1..12).map{Flashcard(id=it.toLong(),deckId=1,word="word$it",definition="解釋 $it",exampleSentence="Sentence $it",sortOrder=it.toLong())}
  compose.setContent{MyApplicationTheme{Surface{AddEditCardScreen(editingCard=cards.first(),allCards=cards,decks=listOf(Deck(id=1,name="測試")),onGetDictionaryMatch={null},onAddFolder={_,_,_,_->},onSaveBatchCards={rows,_->Result.success(rows.mapIndexed{i,c->if(c.id==0L)(i+100).toLong() else c.id})},onBack={})}}}
  compose.onNodeWithTag("card_editor_list").performScrollToIndex(13)
  compose.onNodeWithText("word12",useUnmergedTree=true).assertIsDisplayed()
  compose.onNodeWithTag("add_card_fab").assertIsDisplayed()
  val root=compose.onRoot().fetchSemanticsNode().boundsInRoot
  val button=compose.onNodeWithTag("add_card_fab").fetchSemanticsNode().boundsInRoot
  assertTrue(button.bottom<=root.bottom)
 }
}
