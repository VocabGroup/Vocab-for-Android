package com.example
import com.example.data.importer.FolderShare
import com.example.data.entity.Deck
import com.example.data.entity.Flashcard
import com.example.util.ArticleCoverage
import com.example.util.ScanInstructions
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
class FolderShareTest {
 @Test fun exportAndImportOnlyContent(){
  val raw=FolderShare.export(listOf(Deck(id=1,name="第一課",category="英文")),listOf(Flashcard(id=8,deckId=1,word="apple",definition="蘋果",notes="PRIVATE",isMastered=true,mistakeCount=8,intervalDays=99)))
  assertFalse(raw.contains("PRIVATE"));assertFalse(raw.contains("isMastered"));assertFalse(raw.contains("intervalDays"));assertFalse(raw.contains("\"id\""))
  val parsed=FolderShare.parse(raw).single().cards.single();assertEquals("apple",parsed.word);assertFalse(parsed.isMastered);assertEquals(0,parsed.mistakeCount);assertEquals(0,parsed.intervalDays);assertEquals(0L,parsed.id)
 }
 @Test fun webShareIsAcceptedAndInjectedProgressDiscarded(){
  val raw="""{"format":"vocab-folder-share","version":1,"decks":[{"name":"Unit 1","course":"English","color":"#426B63","cards":[{"word":"study","definition":"學習","mastered":true,"id":"injected","intervalDays":100}]}]}"""
  val card=FolderShare.parse(raw).single().cards.single();assertFalse(card.isMastered);assertEquals(0,card.intervalDays);assertEquals(0L,card.id)
  assertTrue(runCatching{FolderShare.parse(JSONObject(raw).put("version",99).toString())}.isFailure)
 }
 @Test fun articleCoverageUsesFullWordsAndInflections(){
  assertTrue(ArticleCoverage.missing("She studied and took off. Children were running.",listOf("study","take off","child","run")).isEmpty())
  assertEquals(listOf("apple","take off"),ArticleCoverage.missing("Pineapple. Take the office.",listOf("apple","take off")))
 }
 @Test fun scanningNeverMinesSentences(){assertTrue(ScanInstructions.build().contains("禁止從例句"));assertTrue(ScanInstructions.build("自訂範圍","只要左欄").contains("只要左欄"))}
}
