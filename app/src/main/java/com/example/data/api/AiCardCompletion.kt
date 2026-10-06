package com.example.data.api

import com.example.util.OcrCardCandidate

/** Do not attach a translation for a different sentence to a preserved original. */
fun mergeAiCompletion(original: OcrCardCandidate, generated: OcrCardCandidate): OcrCardCandidate {
    if (!original.word.trim().equals(generated.word.trim(), ignoreCase = true)) return original
    val translationMatches = original.exampleSentence.isBlank() ||
        original.exampleSentence.trim() == generated.exampleSentence.trim()
    return original.copy(
        phonetic = original.phonetic.ifBlank { generated.phonetic },
        partOfSpeech = original.partOfSpeech.ifBlank { generated.partOfSpeech },
        definition = original.definition.ifBlank { generated.definition },
        exampleSentence = original.exampleSentence.ifBlank { generated.exampleSentence },
        exampleTranslation = original.exampleTranslation.ifBlank {
            if (translationMatches) generated.exampleTranslation else ""
        }
    )
}
