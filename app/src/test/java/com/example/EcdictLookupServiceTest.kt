package com.example

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.example.data.dictionary.EcdictLookupService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EcdictLookupServiceTest {
    @Test fun bundledLookupAndMissingFields() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        for (word in listOf("apple", "abandon", "serendipity", "take off")) {
            val result = EcdictLookupService.lookup(context, " ${word.uppercase()} ")
            assertEquals(word, result.word.lowercase())
            assertTrue(result.definition.isNotBlank())
            assertEquals("", result.exampleSentence)
            assertEquals("", result.exampleTranslation)
        }
        assertTrue(runCatching { EcdictLookupService.lookup(context, "zzzznonexistentword") }.isFailure)
        assertTrue(runCatching { EcdictLookupService.key("../secret") }.isFailure)
        assertTrue(runCatching { EcdictLookupService.key("蘋果") }.isFailure)
    }
}
