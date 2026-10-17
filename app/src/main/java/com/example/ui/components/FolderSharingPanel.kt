package com.example.ui.components

import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.entity.Deck
import com.example.data.entity.Flashcard
import com.example.data.importer.FolderShare
import com.example.data.importer.SharedFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun FolderSharingPanel(decks:List<Deck>,cards:List<Flashcard>,course:String?,requestDeckId:Long?=null,onRequestHandled:()->Unit={},importRequested:Boolean=false,onImportRequestHandled:()->Unit={},onImport:suspend(List<SharedFolder>,String,Boolean)->Result<List<Long>>,onUndo:suspend(List<Long>)->Result<Unit>,externalUndoIds:List<Long> = emptyList()){
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var mode by remember{mutableStateOf("")};var selected by remember{mutableStateOf(setOf<Long>())};var includeNotes by remember{mutableStateOf(false)}
    var incoming by remember{mutableStateOf<List<SharedFolder>>(emptyList())};var checked by remember{mutableStateOf(setOf<Int>())};var target by remember{mutableStateOf("")};var skip by remember{mutableStateOf(false)};var busy by remember{mutableStateOf(false)};var message by remember{mutableStateOf("")};var undo by remember{mutableStateOf<List<Long>>(emptyList())};var confirmUndo by remember{mutableStateOf(false)}
    LaunchedEffect(externalUndoIds){undo=externalUndoIds}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)scope.launch{busy=true;runCatching{withContext(Dispatchers.IO){context.contentResolver.openInputStream(uri)!!.use{stream->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);var total=0;while(true){val n=stream.read(buffer);if(n<0)break;total+=n;require(total<=20*1024*1024){"分享檔超過 20 MB"};out.write(buffer,0,n)};FolderShare.parse(out.toString("UTF-8"))}}}.onSuccess{incoming=it;checked=it.indices.toSet();target=course.orEmpty();mode="import"}.onFailure{message=it.message.orEmpty()};busy=false}}
    LaunchedEffect(requestDeckId){if(requestDeckId!=null){selected=setOf(requestDeckId);mode="export";onRequestHandled()}}
    LaunchedEffect(importRequested){if(importRequested){picker.launch(arrayOf("application/json","text/plain","application/octet-stream"));onImportRequestHandled()}}
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(enabled=!busy,onClick={selected=emptySet();mode="export"}){Text("選取／分享")};OutlinedButton(enabled=!busy,onClick={picker.launch(arrayOf("application/json","text/plain","application/octet-stream"))}){Text("匯入分享檔案")}}
        if(undo.isNotEmpty())TextButton(onClick={confirmUndo=true}){Text("撤回這次匯入")}
        if(message.isNotBlank())Text(message,style=MaterialTheme.typography.bodySmall)
    }
    if(confirmUndo)AlertDialog(onDismissRequest={if(!busy)confirmUndo=false},title={Text("撤回這次匯入？")},text={Text("僅移除這批新資料夾及之後的修改；原本資料不受影響。")},confirmButton={TextButton(enabled=!busy,onClick={scope.launch{busy=true;onUndo(undo).onSuccess{undo=emptyList();confirmUndo=false;message="已撤回匯入"}.onFailure{message=it.message.orEmpty()};busy=false}}){Text("撤回")}},dismissButton={TextButton(onClick={confirmUndo=false}){Text("取消")}})
    if(mode.isNotEmpty())AlertDialog(onDismissRequest={if(!busy)mode=""},title={Text(if(mode=="export")"分享資料夾" else "匯入分享資料夾")},text={Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
        if(mode=="export"){
            Text("只分享內容，不包含學習狀態、API Key 或聊天紀錄。")
            decks.forEach{deck->Row{Checkbox(checked=deck.id in selected,onCheckedChange={selected=if(it)selected+deck.id else selected-deck.id});Text("${deck.category} · ${deck.name}（${cards.count{it.deckId==deck.id}} 字）")}}
            Row{Checkbox(checked=includeNotes,onCheckedChange={includeNotes=it});Text("包含個人筆記（可能含私人資訊）")}
            Text("分享內容預覽：")
            cards.filter{it.deckId in selected}.take(80).forEach{Text("${it.word} — ${it.definition}\n${it.exampleSentence}\n${it.exampleTranslation}",style=MaterialTheme.typography.bodySmall)}
        }else{
            Text("所有單字視為全新學習。同名資料夾另建，不覆蓋。")
            OutlinedTextField(value=target,onValueChange={target=it},label={Text("目的課程（空白保留原分類）")},modifier=Modifier.fillMaxWidth())
            decks.map{it.category}.distinct().forEach{c->TextButton(onClick={target=c}){Text(c)}}
            Row{Checkbox(checked=skip,onCheckedChange={skip=it});Text("略過詞庫已有單字（否則加入新資料夾）")}
            incoming.forEachIndexed{index,deck->Row{Checkbox(checked=index in checked,onCheckedChange={checked=if(it)checked+index else checked-index});Text("${deck.course} · ${deck.name}：${deck.cards.size} 字，已有 ${deck.cards.count{c->cards.any{it.normalizedWord==c.normalizedWord}}} 字")};deck.cards.take(40).forEach{Text("${it.word} — ${it.definition}\n${it.exampleSentence}",style=MaterialTheme.typography.bodySmall)}}
        }
    }},confirmButton={TextButton(enabled=!busy&&(if(mode=="export")selected.isNotEmpty() else checked.isNotEmpty()),onClick={scope.launch{busy=true;try{if(mode=="export"){
        val json=withContext(Dispatchers.Default){FolderShare.export(decks.filter{it.id in selected},cards,includeNotes)}
        val file=withContext(Dispatchers.IO){File(context.cacheDir,"Vocab-${System.currentTimeMillis()}.vocab-share.json").apply{writeText(json)}}
        val uri=FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="application/json";putExtra(Intent.EXTRA_STREAM,uri);clipData=ClipData.newRawUri("Vocab",uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"分享 Vocab 資料夾"));mode=""
    }else{onImport(incoming.filterIndexed{i,_->i in checked},target,skip).onSuccess{undo=it;message="已匯入 ${it.size} 個資料夾，精熟度已重設";mode=""}.onFailure{message=it.message.orEmpty()}}}catch(e:Exception){message=e.message.orEmpty()}finally{busy=false}}}){Text(if(busy)"處理中…" else if(mode=="export")"分享 JSON" else "確認匯入")}},dismissButton={TextButton(enabled=!busy,onClick={mode=""}){Text("取消")}})
}
