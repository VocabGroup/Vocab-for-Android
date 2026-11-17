package com.example.data

import androidx.room.withTransaction
import com.example.data.entity.Flashcard

data class WorkspaceTransferResult(val changed:Int,val skipped:Int,val overwritten:Int)

/** Content replacement is explicit; copying never inherits review history. */
class WorkspaceTransfers(private val db:AppDatabase) {
    suspend fun deleteCourse(course:String)=db.withTransaction{
        require(course.isNotBlank())
        val decks=db.deckDao().getAllDecksOnce().filter{it.category==course}
        for(deck in decks){db.studyLogDao().deleteForCards(db.flashcardDao().getCardsByDeckOnce(deck.id).map{it.id});db.deckDao().deleteDeck(deck)}
    }
    private fun fresh(card:Flashcard,deckId:Long,order:Long)=card.copy(id=0,deckId=deckId,isMastered=false,isFavorite=false,isSuspended=false,mistakeCount=0,intervalDays=0,easeFactor=2.5f,repetitionCount=0,nextReviewTimestamp=System.currentTimeMillis(),lastReviewedTimestamp=null,createdAt=System.currentTimeMillis(),sortOrder=order)
    private fun content(target:Flashcard,source:Flashcard)=target.copy(word=source.word,normalizedWord=source.normalizedWord,phonetic=source.phonetic,partOfSpeech=source.partOfSpeech,definition=source.definition,exampleSentence=source.exampleSentence,exampleTranslation=source.exampleTranslation,imageUrl=source.imageUrl,notes=source.notes)
    suspend fun words(ids:List<Long>,target:Long,copy:Boolean,overwrite:Boolean):WorkspaceTransferResult=db.withTransaction {
        require(db.deckDao().getDeckById(target)!=null){"目標資料夾已不存在"}
        var changed=0;var skipped=0;var replaced=0;var order=db.flashcardDao().getMaxSortOrder(target)+ids.size+1
        for(id in ids.distinct()){
            val source=db.flashcardDao().getCardById(id)?:continue
            if(source.deckId==target){skipped++;continue}
            val existing=db.flashcardDao().getCardByWord(target,source.normalizedWord)
            if(existing!=null){
                if(!overwrite){skipped++;continue}
                db.flashcardDao().updateCard(content(existing,source))
                if(!copy){db.studyLogDao().reassignCardLogs(source.id,existing.id);db.flashcardDao().deleteCard(source)}
                replaced++
            }else if(copy)db.flashcardDao().insertCard(fresh(source,target,order--))
            else db.flashcardDao().updateCard(source.copy(deckId=target,sortOrder=order--))
            changed++
        }
        WorkspaceTransferResult(changed,skipped,replaced)
    }
    suspend fun folders(ids:List<Long>,course:String,copy:Boolean,overwrite:Boolean):WorkspaceTransferResult=db.withTransaction {
        require(course.isNotBlank()&&course.length<=200){"請選擇目的課程"}
        var changed=0;var skipped=0;var replaced=0
        for(id in ids.distinct()){
            val source=db.deckDao().getDeckById(id)?:continue
            if(source.category==course){skipped++;continue}
            val target=db.deckDao().getAllDecksOnce().firstOrNull{it.category==course&&it.name==source.name}
            if(target!=null&&!overwrite){skipped++;continue}
            val cards=db.flashcardDao().getCardsByDeckOnce(source.id)
            if(target!=null){
                val sourceWords=cards.map{it.normalizedWord}.toSet()
                db.flashcardDao().deleteCards(db.flashcardDao().getCardsByDeckOnce(target.id).filter{it.normalizedWord !in sourceWords})
                var order=System.currentTimeMillis()+cards.size
                for(card in cards){val match=db.flashcardDao().getCardByWord(target.id,card.normalizedWord)
                    if(match!=null){db.flashcardDao().updateCard(content(match,card).copy(sortOrder=order--));if(!copy)db.studyLogDao().reassignCardLogs(card.id,match.id)}
                    else if(copy)db.flashcardDao().insertCard(fresh(card,target.id,order--))
                    else db.flashcardDao().updateCard(card.copy(deckId=target.id,sortOrder=order--))
                }
                db.deckDao().updateDeck(target.copy(description=source.description,colorHex=source.colorHex))
                if(!copy)db.deckDao().deleteDeck(source)
                replaced++
            }else if(copy){val newId=db.deckDao().insertDeck(source.copy(id=0,category=course,createdAt=System.currentTimeMillis(),sortOrder=System.currentTimeMillis()));cards.forEachIndexed{index,card->db.flashcardDao().insertCard(fresh(card,newId,System.currentTimeMillis()+cards.size-index))}}
            else db.deckDao().updateDeck(source.copy(category=course))
            changed++
        }
        WorkspaceTransferResult(changed,skipped,replaced)
    }
}
