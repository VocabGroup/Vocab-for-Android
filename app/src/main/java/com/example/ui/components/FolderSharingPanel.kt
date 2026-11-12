package com.example.ui.components

import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.example.ui.components.ModernAlertDialog as AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.entity.Deck
import com.example.data.entity.Flashcard
import com.example.data.importer.FolderShare
import com.example.data.importer.SharedFolder
import com.example.data.importer.ShareLinks
import com.example.data.importer.PublishedShare
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun FolderSharingPanel(decks:List<Deck>,cards:List<Flashcard>,course:String?,requestDeckId:Long?=null,onRequestHandled:()->Unit={},importRequested:Boolean=false,onImportRequestHandled:()->Unit={},onImport:suspend(List<SharedFolder>,String,Boolean)->Result<List<Long>>,onUndo:suspend(List<Long>)->Result<Unit>,externalUndoIds:List<Long> = emptyList(),requestCourse:String?=null,onCourseRequestHandled:()->Unit={},showActions:Boolean=true,importLinkRequested:Boolean=false,onLinkRequestHandled:()->Unit={}){
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var published by remember{mutableStateOf<PublishedShare?>(null)}
    var confirmUpload by remember{mutableStateOf(false)}
    var showLinkImport by remember{mutableStateOf(false)}
    var inputUrl by remember{mutableStateOf("")}
    LaunchedEffect(importLinkRequested){if(importLinkRequested){showLinkImport=true;onLinkRequestHandled()}}
    var mode by remember{mutableStateOf("")};var selected by remember{mutableStateOf(setOf<Long>())};var includeNotes by remember{mutableStateOf(false)}
    var incoming by remember{mutableStateOf<List<SharedFolder>>(emptyList())};var checked by remember{mutableStateOf(setOf<Int>())};var target by remember{mutableStateOf("")};var skip by remember{mutableStateOf(false)};var busy by remember{mutableStateOf(false)};var message by remember{mutableStateOf("")};var undo by remember{mutableStateOf<List<Long>>(emptyList())};var confirmUndo by remember{mutableStateOf(false)}
    LaunchedEffect(externalUndoIds){undo=externalUndoIds}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)scope.launch{busy=true;runCatching{withContext(Dispatchers.IO){context.contentResolver.openInputStream(uri)!!.use{stream->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);var total=0;while(true){val n=stream.read(buffer);if(n<0)break;total+=n;require(total<=20*1024*1024){"分享檔超過 20 MB"};out.write(buffer,0,n)};FolderShare.parse(out.toString("UTF-8"))}}}.onSuccess{message="";incoming=it;checked=it.indices.toSet();target=course.orEmpty();mode="import"}.onFailure{message=it.message.orEmpty()};busy=false}}
    LaunchedEffect(requestDeckId){if(requestDeckId!=null){selected=setOf(requestDeckId);includeNotes=false;message="";mode="export";onRequestHandled()}}
    LaunchedEffect(requestCourse){if(requestCourse!=null){selected=decks.filter{it.category==requestCourse}.map{it.id}.toSet();includeNotes=false;message="";mode="export";onCourseRequestHandled()}}
    LaunchedEffect(importRequested){if(importRequested){picker.launch(arrayOf("application/json","text/plain","application/octet-stream"));onImportRequestHandled()}}
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
        if(showActions)Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(enabled=!busy,onClick={selected=emptySet();mode="export"}){Text("選取／分享")};OutlinedButton(enabled=!busy,onClick={picker.launch(arrayOf("application/json","text/plain","application/octet-stream"))}){Text("匯入分享檔案")}}
        if(undo.isNotEmpty())TextButton(onClick={confirmUndo=true}){Text("撤回這次匯入")}
        if(busy)Text("處理分享資料中…",style=MaterialTheme.typography.bodySmall)
        if(message.isNotBlank())Text(message,style=MaterialTheme.typography.bodySmall)
    }
    if(confirmUndo)AlertDialog(onDismissRequest={if(!busy)confirmUndo=false},title={Text("撤回這次匯入？")},text={Text("僅移除這批新資料夾及之後的修改；原本資料不受影響。")},confirmButton={TextButton(enabled=!busy,onClick={scope.launch{busy=true;onUndo(undo).onSuccess{undo=emptyList();confirmUndo=false;message="已撤回匯入"}.onFailure{message=it.message.orEmpty()};busy=false}}){Text("撤回")}},dismissButton={TextButton(onClick={confirmUndo=false}){Text("取消")}})
    if(mode.isNotEmpty())AlertDialog(onDismissRequest={if(!busy)mode=""},title={Text(if(mode=="export")"分享資料夾" else "匯入分享資料夾")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){if(message.isNotBlank())Text(message,color=MaterialTheme.colorScheme.error);Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
        if(mode=="export"){
            Text("只分享內容，不包含學習狀態、API Key 或聊天紀錄。")
            Text("已選 ${selected.size} 個資料夾 · ${cards.count{it.deckId in selected}} 個單字")
            OutlinedButton(enabled=!busy&&selected.isNotEmpty(),onClick={confirmUpload=true}){Text("產生分享網址／QR Code")}
            Row{TextButton(onClick={selected=decks.filter{course==null||it.category==course}.map{it.id}.toSet()}){Text("全選目前課程")};TextButton(onClick={selected=emptySet()}){Text("清除選取")}}
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
    }}},confirmButton={TextButton(enabled=!busy&&(if(mode=="export")selected.isNotEmpty() else checked.isNotEmpty()),onClick={scope.launch{busy=true;try{if(mode=="export"){
        val json=withContext(Dispatchers.Default){FolderShare.export(decks.filter{it.id in selected},cards,includeNotes)}
        val file=withContext(Dispatchers.IO){File(context.cacheDir,"Vocab-${System.currentTimeMillis()}.vocab-share.json").apply{writeText(json)}}
        val uri=FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="application/json";putExtra(Intent.EXTRA_STREAM,uri);clipData=ClipData.newRawUri("Vocab",uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"分享 Vocab 資料夾"));mode=""
    }else{onImport(incoming.filterIndexed{i,_->i in checked},target,skip).onSuccess{undo=it;message="已匯入 ${it.size} 個資料夾，精熟度已重設";mode=""}.onFailure{message=it.message.orEmpty()}}}catch(e:Exception){message=e.message.orEmpty()}finally{busy=false}}}){Text(if(busy)"處理中…" else if(mode=="export")"分享 JSON" else "確認匯入")}},dismissButton={TextButton(enabled=!busy,onClick={mode=""}){Text("取消")}})
    if(confirmUpload)AlertDialog(onDismissRequest={if(!busy)confirmUpload=false},title={Text("上傳分享教材？")},text={Text("所選教材會上傳至 Vocab 雲端，任何持有連結的人都能讀取。7 天後自動刪除；不包含學習進度與 API Key。${if(includeNotes)"注意：你已選擇包含個人筆記。" else "個人筆記不會上傳。"}")},confirmButton={TextButton(enabled=!busy,onClick={scope.launch{busy=true;message="";try{val raw=withContext(Dispatchers.Default){FolderShare.export(decks.filter{it.id in selected},cards,includeNotes)};published=ShareLinks.publish(raw);mode="";confirmUpload=false}catch(e:Exception){message=e.message?:"分享失敗";confirmUpload=false}finally{busy=false}}}){Text(if(busy)"上傳中…" else "同意並產生連結")}},dismissButton={TextButton(enabled=!busy,onClick={confirmUpload=false}){Text("取消")}})
    published?.let{share->val qr=remember(share.url){ShareLinks.qr(share.url)}
        AlertDialog(onDismissRequest={if(!busy)published=null},title={Text("分享網址與 QR Code")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Image(qr.asImageBitmap(),"分享 QR Code",Modifier.fillMaxWidth().height(220.dp))
            Text(share.url,style=MaterialTheme.typography.bodySmall)
            Text("到期：${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(share.expiresAt))}")
            Text("連結不會因讀取延長期限；關閉前可撤銷本次分享。",style=MaterialTheme.typography.bodySmall)
            TextButton(enabled=!busy,onClick={val clipboard=context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager;clipboard.setPrimaryClip(ClipData.newPlainText("Vocab",share.url));message="已複製分享網址"}){Text("複製網址")}
            TextButton(enabled=!busy,onClick={scope.launch{busy=true;try{ShareLinks.revoke(share);published=null;message="已撤銷分享"}catch(e:Exception){message=e.message?:"撤銷失敗"}finally{busy=false}}}){Text("撤銷這個連結")}
            if(message.isNotBlank())Text(message)
        }},confirmButton={TextButton(enabled=!busy,onClick={context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,share.url)},"分享 Vocab 教材"))}){Text("分享網址")}},dismissButton={TextButton(enabled=!busy,onClick={published=null}){Text("關閉")}})
    }
    if(showLinkImport)AlertDialog(onDismissRequest={if(!busy)showLinkImport=false},title={Text("從分享網址匯入")},text={Column{OutlinedTextField(value=inputUrl,onValueChange={inputUrl=it},label={Text("貼上 Vocab 分享網址")},enabled=!busy,modifier=Modifier.fillMaxWidth());if(message.isNotBlank())Text(message)}},confirmButton={TextButton(enabled=!busy&&inputUrl.isNotBlank(),onClick={scope.launch{busy=true;message="";try{incoming=ShareLinks.load(inputUrl);checked=incoming.indices.toSet();target=course.orEmpty();mode="import";showLinkImport=false}catch(e:Exception){message=e.message?:"讀取失敗"}finally{busy=false}}}){Text(if(busy)"讀取中…" else "預覽教材")}},dismissButton={TextButton(enabled=!busy,onClick={showLinkImport=false}){Text("取消")}})
}
