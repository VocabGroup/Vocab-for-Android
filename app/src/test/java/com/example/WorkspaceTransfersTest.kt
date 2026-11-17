package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.WorkspaceTransfers
import com.example.data.entity.Deck
import com.example.data.entity.Flashcard
import com.example.data.entity.StudyLog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkspaceTransfersTest{
    private lateinit var db:AppDatabase
    private lateinit var transfers:WorkspaceTransfers
    @Before fun setup(){db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),AppDatabase::class.java).allowMainThreadQueries().build();transfers=WorkspaceTransfers(db)}
    @After fun close(){db.close()}
    @Test fun skipRetainsSourceAndOverwritePreservesTargetLearning()=runTest{
        val a=db.deckDao().insertDeck(Deck(name="A"));val b=db.deckDao().insertDeck(Deck(name="B"))
        val source=db.flashcardDao().insertCard(Flashcard(deckId=a,word="apple",definition="新版解釋",notes="備註"))
        val target=db.flashcardDao().insertCard(Flashcard(deckId=b,word="apple",definition="舊版",isMastered=true,repetitionCount=7))
        db.studyLogDao().insertLog(StudyLog(cardId=source,rating=3))
        assertEquals(1,transfers.words(listOf(source),b,false,false).skipped)
        assertNotNull(db.flashcardDao().getCardById(source))
        assertEquals("舊版",db.flashcardDao().getCardById(target)!!.definition)
        assertEquals(1,transfers.words(listOf(source),b,false,true).overwritten)
        val saved=db.flashcardDao().getCardById(target)!!
        assertEquals("新版解釋",saved.definition);assertEquals("備註",saved.notes);assertTrue(saved.isMastered);assertEquals(7,saved.repetitionCount)
        assertNull(db.flashcardDao().getCardById(source));assertEquals(target,db.studyLogDao().getAllLogs().first().single().cardId)
    }
    @Test fun copiesResetProgressAndPreserveSource()=runTest{
        val a=db.deckDao().insertDeck(Deck(name="A",category="甲"));val b=db.deckDao().insertDeck(Deck(name="B",category="乙"))
        val id=db.flashcardDao().insertCard(Flashcard(deckId=a,word="book",definition="書",isMastered=true,repetitionCount=8,mistakeCount=3,intervalDays=30))
        transfers.words(listOf(id),b,true,false)
        val copy=db.flashcardDao().getCardsByDeckOnce(b).single()
        assertFalse(copy.isMastered);assertEquals(0,copy.repetitionCount);assertEquals(0,copy.mistakeCount);assertEquals(0,copy.intervalDays)
        assertTrue(db.flashcardDao().getCardById(id)!!.isMastered)
        transfers.folders(listOf(a),"丙",true,false)
        val folder=db.deckDao().getAllDecksOnce().single{it.category=="丙"}
        assertFalse(db.flashcardDao().getCardsByDeckOnce(folder.id).single().isMastered)
        assertNotNull(db.deckDao().getDeckById(a))
    }
    @Test fun folderOverwriteIsExplicitAndMatchesSourceContent()=runTest{
        val a=db.deckDao().insertDeck(Deck(name="L1",category="甲"));val b=db.deckDao().insertDeck(Deck(name="L1",category="乙"))
        db.flashcardDao().insertCard(Flashcard(deckId=a,word="cat",definition="貓"))
        db.flashcardDao().insertCard(Flashcard(deckId=b,word="dog",definition="狗"))
        assertEquals(1,transfers.folders(listOf(a),"乙",false,false).skipped)
        assertNotNull(db.deckDao().getDeckById(a))
        transfers.folders(listOf(a),"乙",false,true)
        assertNull(db.deckDao().getDeckById(a));assertEquals(listOf("cat"),db.flashcardDao().getCardsByDeckOnce(b).map{it.word})
    }
    @Test fun deleteCourseRemovesOnlyItsOwnContentsAndLogs()=runTest{
        val a=db.deckDao().insertDeck(Deck(name="A",category="甲"));val b=db.deckDao().insertDeck(Deck(name="B",category="乙"))
        val id=db.flashcardDao().insertCard(Flashcard(deckId=a,word="safe",definition="安全"))
        db.studyLogDao().insertLog(StudyLog(cardId=id,rating=3))
        transfers.deleteCourse("甲")
        assertNull(db.deckDao().getDeckById(a));assertNull(db.flashcardDao().getCardById(id));assertTrue(db.studyLogDao().getAllLogs().first().isEmpty())
        assertNotNull(db.deckDao().getDeckById(b))
    }
    @Test fun missingTargetDoesNotDeleteSource()=runTest{
        val a=db.deckDao().insertDeck(Deck(name="A"));val id=db.flashcardDao().insertCard(Flashcard(deckId=a,word="safe",definition="安全"))
        assertTrue(runCatching{transfers.words(listOf(id),999,false,true)}.isFailure)
        assertNotNull(db.flashcardDao().getCardById(id))
    }
}
