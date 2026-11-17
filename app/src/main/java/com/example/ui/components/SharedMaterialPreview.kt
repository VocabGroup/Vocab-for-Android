package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.importer.ReceivedShare
import com.example.data.importer.ShareLinks

/** Shared materials are intentionally detached from the user's database and review state. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedMaterialPreview(url:String,onClose:()->Unit,onGet:()->Unit,load:suspend(String)->ReceivedShare=ShareLinks::loadDetails){
    var loaded by remember(url){mutableStateOf<ReceivedShare?>(null)}
    var error by remember(url){mutableStateOf<String?>(null)}
    var folderIndex by remember(url){mutableStateOf<Int?>(null)}
    var search by remember(url){mutableStateOf("")}
    var attempt by remember{mutableIntStateOf(0)}
    LaunchedEffect(url,attempt){error=null;try{loaded=load(url)}catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){error=e.message?:"無法讀取分享"}}
    val data=loaded
    val course=data?.kind=="course" || (data!=null&&data.folders.size>1)
    val folder=data?.folders?.getOrNull(folderIndex?:if(course)-1 else 0)
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)){
        Scaffold(modifier=Modifier.fillMaxSize().imePadding(),topBar={EditorialTopAppBar(
            title={Text(folder?.name?:data?.title?.ifBlank{data.folders.firstOrNull()?.course.orEmpty()}?:"分享教材",maxLines=1)},
            navigationIcon={IconButton(onClick={if(course&&folderIndex!=null){folderIndex=null;search=""}else onClose()}){Icon(if(course&&folderIndex!=null)Icons.Default.ArrowBack else Icons.Default.Close,"返回／關閉")}},
            actions={Button(onClick=onGet,enabled=data!=null){Text("取得")}}
        )}){padding->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                if(data==null&&error==null)LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let{Text(it,color=MaterialTheme.colorScheme.error);TextButton(onClick={attempt++}){Text("重試")}}
                if(data!=null){
                    ModernTextField(search,{search=it},placeholder={Text(if(folder==null)"搜尋資料夾" else "搜尋單字或解釋")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true,modifier=Modifier.fillMaxWidth())
                    LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(bottom=24.dp)){
                        if(folder==null){items(data.folders.indices.filter{data.folders[it].name.contains(search,true)},key={it}){index->val item=data.folders[index]
                            ModernListRow(item.name,"${item.cards.size} 個單字",Icons.Default.Folder,onClick={folderIndex=index;search=""})
                        }}else{
                            items(folder.cards.filter{it.word.contains(search,true)||it.definition.contains(search,true)}){card->
                                Column(Modifier.fillMaxWidth().padding(vertical=16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                                    Text(card.word,style=MaterialTheme.typography.headlineSmall,fontFamily=FontFamily.Serif)
                                    if(card.phonetic.isNotBlank())Text(card.phonetic,style=MaterialTheme.typography.bodySmall)
                                    Text(listOf(card.partOfSpeech,card.definition).filter{it.isNotBlank()}.joinToString(" · "))
                                    if(card.exampleSentence.isNotBlank())Text(card.exampleSentence)
                                    if(card.exampleTranslation.isNotBlank())Text(card.exampleTranslation,style=MaterialTheme.typography.bodySmall)
                                };HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
