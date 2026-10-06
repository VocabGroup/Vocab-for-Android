package com.example

import com.example.data.api.mergeAiCompletion
import com.example.data.assistant.applyAssistantCardUpdate
import com.example.data.assistant.assistantEditableFields
import com.example.data.entity.Flashcard
import com.example.util.OcrCardCandidate
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class Ai311RegressionTest {
    private val original = OcrCardCandidate("bank", "n.", "銀行", "She waited by the river bank.", phonetic = "/bæŋk/")

    @Test fun completionTranslatesTheExistingSentenceWithoutReplacingIt() {
        val generated = original.copy(exampleTranslation = "她在河岸邊等候。")
        val result = mergeAiCompletion(original, generated)
        assertEquals(original.exampleSentence, result.exampleSentence)
        assertEquals("她在河岸邊等候。", result.exampleTranslation)
        assertEquals(original.definition, result.definition)
    }

    @Test fun translationForANewSentenceIsNotAttachedToTheOriginal() {
        val generated = original.copy(exampleSentence = "I work at a bank.", exampleTranslation = "我在銀行工作。")
        val result = mergeAiCompletion(original, generated)
        assertEquals(original.exampleSentence, result.exampleSentence)
        assertEquals("", result.exampleTranslation)
    }

    @Test fun completionNeverOverwritesExistingTranslationOrOtherPopulatedFields() {
        val complete = original.copy(exampleTranslation = "原有翻譯")
        assertEquals(complete, mergeAiCompletion(complete, original.copy(definition = "河岸", exampleTranslation = "新翻譯")))
    }

    @Test fun emptySentenceCanReceiveAMatchingNewSentenceAndTranslation() {
        val empty = original.copy(exampleSentence = "")
        val result = mergeAiCompletion(empty, original.copy(exampleTranslation = "她在河岸邊等候。"))
        assertEquals(original.exampleSentence, result.exampleSentence)
        assertEquals("她在河岸邊等候。", result.exampleTranslation)
    }

    @Test fun explicitChatUpdateReplacesNonemptyFieldsButKeepsOmittedFieldsAndSrs() {
        val card = Flashcard(id = 8, deckId = 3, word = "bank", definition = "銀行",
            exampleSentence = original.exampleSentence, exampleTranslation = "既有翻譯", notes = "原註記", intervalDays = 21, repetitionCount = 5)
        val updated = applyAssistantCardUpdate(card, JSONObject().put("definition", "河岸").put("notes", "依使用者指令修改"))
        assertEquals("河岸", updated.definition)
        assertEquals("依使用者指令修改", updated.notes)
        assertEquals(card.exampleSentence, updated.exampleSentence)
        assertEquals(card.exampleTranslation, updated.exampleTranslation)
        assertEquals(21, updated.intervalDays)
        assertEquals(5, updated.repetitionCount)
        assertEquals(card.id, updated.id)
        assertEquals(card.deckId, updated.deckId)
        assertEquals(card, applyAssistantCardUpdate(updated, card.assistantEditableFields()))
    }

    @Test fun explicitClearAndWordRenameAreSupportedButNullDoesNotClear() {
        val card = Flashcard(deckId = 3, word = "bank", definition = "銀行", notes = "原註記")
        val updated = applyAssistantCardUpdate(card, JSONObject().put("word", "Shore").put("notes", "").put("definition", JSONObject.NULL))
        assertEquals("Shore", updated.word)
        assertEquals("shore", updated.normalizedWord)
        assertEquals("", updated.notes)
        assertEquals("銀行", updated.definition)
        assertTrue(runCatching { applyAssistantCardUpdate(card, JSONObject().put("word", " ")) }.isFailure)
    }
}
