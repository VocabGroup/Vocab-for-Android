package com.example.data.assistant

import com.example.data.entity.Flashcard
import org.json.JSONObject

/** Explicit chat edits may replace populated fields; omitted fields stay intact. */
fun applyAssistantCardUpdate(card: Flashcard, fields: JSONObject): Flashcard {
    fun value(key: String, current: String, limit: Int): String =
        if (fields.has(key) && !fields.isNull(key)) fields.getString(key).trim().take(limit) else current
    val word = value("word", card.word, 100)
    require(word.isNotBlank()) { "單字不能為空白" }
    return card.copy(word = word, normalizedWord = word.lowercase(),
        phonetic = value("phonetic", card.phonetic, 100),
        partOfSpeech = value("partOfSpeech", card.partOfSpeech, 80),
        definition = value("definition", card.definition, 500),
        exampleSentence = value("exampleSentence", card.exampleSentence, 600),
        exampleTranslation = value("exampleTranslation", card.exampleTranslation, 600),
        notes = value("notes", card.notes, 2000)
    )
}

fun Flashcard.assistantEditableFields(): JSONObject = JSONObject()
    .put("cardId", id).put("word", word).put("phonetic", phonetic)
    .put("partOfSpeech", partOfSpeech).put("definition", definition)
    .put("exampleSentence", exampleSentence).put("exampleTranslation", exampleTranslation).put("notes", notes)
