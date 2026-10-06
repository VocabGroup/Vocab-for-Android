package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.assistant.*
import com.example.data.entity.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AssistantGrammarWriterTest {
    private lateinit var db: AppDatabase
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).allowMainThreadQueries().build()
    }
    @After fun cleanup() { db.close() }
    private val draft = GrammarDraft(title = "現在完成式", summary = "過去事件與現在的關聯", structure = "have/has + p.p.",
        patterns = listOf(GrammarPatternDraft(formula = "has + p.p.", examples = listOf(GrammarExampleDraft(sentence = "He has studied.", translation = "他已經讀過了。")))),
        questions = listOf(GrammarQuestionDraft(grammarPatternIndex = 0, prompt = "He {{answer}} studied.", answer = "has", options = "has,have,had")))

    @Test fun createsNotePatternsExamplesAndQuestionsInTheChosenLibraryAndCanUndo() = runTest {
        val libraryId = db.grammarLibraryDao().insert(GrammarLibrary(course = "高中英文", name = "時態"))
        val id = createAssistantGrammar(db, libraryId, draft)
        val note = db.grammarNoteDao().getById(id)!!
        assertEquals("高中英文", note.course); assertEquals("時態", note.folder); assertEquals("AI", note.sourceType)
        val pattern = db.grammarContentDao().getPatternsForNote(id).single()
        assertEquals(pattern.id, db.grammarContentDao().getExamplesForNote(id).single().grammarPatternId)
        assertEquals(pattern.id, db.grammarQuestionDao().getForNote(id).single().grammarPatternId)
        undoAssistantGrammarCreation(db, id, assistantGrammarSnapshot(db, id))
        assertNull(db.grammarNoteDao().getById(id))
        assertTrue(db.grammarContentDao().getPatternsForNote(id).isEmpty())
        assertTrue(db.grammarContentDao().getExamplesForNote(id).isEmpty())
        assertTrue(db.grammarQuestionDao().getForNote(id).isEmpty())
        assertEquals(1, db.grammarLibraryDao().getAllOnce().size)
    }

    @Test fun rejectsUnknownLibraryDuplicatesAndInvalidQuestionsWithoutPartialWrites() = runTest {
        assertTrue(runCatching { createAssistantGrammar(db, 999, draft) }.isFailure)
        val libraryId = db.grammarLibraryDao().insert(GrammarLibrary(name = "時態"))
        val invalid = draft.copy(questions = listOf(GrammarQuestionDraft(prompt = "No answer marker", answer = "has")))
        assertTrue(runCatching { createAssistantGrammar(db, libraryId, invalid) }.isFailure)
        assertTrue(db.grammarNoteDao().getAllOnce().isEmpty())
        createAssistantGrammar(db, libraryId, draft)
        assertTrue(runCatching { createAssistantGrammar(db, libraryId, draft) }.isFailure)
        assertEquals(1, db.grammarNoteDao().getAllOnce().size)
    }

    @Test fun undoNeverDeletesSubsequentUserEdits() = runTest {
        val libraryId = db.grammarLibraryDao().insert(GrammarLibrary(name = "時態"))
        val id = createAssistantGrammar(db, libraryId, draft)
        val snapshot = assistantGrammarSnapshot(db, id)
        val note = db.grammarNoteDao().getById(id)!!
        db.grammarNoteDao().update(note.copy(summary = "使用者修改過的內容"))
        assertTrue(runCatching { undoAssistantGrammarCreation(db, id, snapshot) }.isFailure)
        assertEquals("使用者修改過的內容", db.grammarNoteDao().getById(id)!!.summary)
    }
}
