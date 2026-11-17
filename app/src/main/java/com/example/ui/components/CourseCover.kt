package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.settings.CourseCoverStore
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

private val covers=listOf(
 Triple("sky","晴空藍","#DBEEFA"),Triple("ice","冰川藍","#E6F3F7"),Triple("cornflower","矢車菊藍","#DCE5FA"),Triple("sea","海灣藍","#CFE5ED"),
 Triple("lavender","薰衣草","#E7E0F5"),Triple("rose","玫瑰粉","#F4DFE5"),Triple("mint","薄荷綠","#DCEFE5"),Triple("sage","鼠尾草","#DDE7D5"),
 Triple("sand","沙岸米","#F1E7D2"),Triple("peach","蜜桃","#F5DFD1"),Triple("slate","霧灰","#DCE3EA"),Triple("lemon","奶油黃","#F5EFCE"))
private fun coverColor(value:String):Color=runCatching { Color(android.graphics.Color.parseColor(if(value.startsWith("#"))value else covers.firstOrNull{value=="preset:${it.first}"}?.third?:"#DBEEFA")) }.getOrDefault(Color(0xFFDBEEFA))

@Composable
fun CourseBook(course:String,subtitle:String,cover:String,store:CourseCoverStore,onOpen:()->Unit,onCover:()->Unit,modifier:Modifier=Modifier,showCoverButton:Boolean=true) {
 val background=coverColor(cover);val file=store.file(cover);val ink=if(file!=null||background.luminance()<.35f)Color.White else Color(0xFF193D5C)
 Box(modifier) {
  Surface(onClick=onOpen,shape=RoundedCornerShape(topStart=10.dp,bottomStart=10.dp,topEnd=28.dp,bottomEnd=28.dp),color=background,shadowElevation=4.dp,modifier=Modifier.fillMaxWidth().aspectRatio(.8f).testTag("course_$course")) {
   Box(Modifier.fillMaxSize()){
    if(file!=null){AsyncImage(file,null,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize());Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.35f),Color.Transparent,Color.Black.copy(alpha=.65f)))))}
    Box(Modifier.padding(start=8.dp).width(2.dp).fillMaxHeight().background(Color.Black.copy(alpha=.06f)))
    Column(Modifier.fillMaxSize().padding(start=20.dp,end=16.dp,top=54.dp,bottom=18.dp),verticalArrangement=Arrangement.SpaceBetween){
     Text(course,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold,color=ink,maxLines=3,overflow=TextOverflow.Ellipsis)
     Text(subtitle,style=MaterialTheme.typography.bodySmall,color=ink)
    }
   }
  }
  if(showCoverButton)FilledTonalIconButton(onCover,modifier=Modifier.align(Alignment.TopEnd).padding(8.dp).size(36.dp),shape=CircleShape){Icon(Icons.Default.Edit,"編輯課程 $course",Modifier.size(20.dp))}
 }
}

@Composable
fun CourseCoverDialog(course:String,store:CourseCoverStore,onDismiss:()->Unit,onSaved:()->Unit,create:Boolean=false,onConfirm:((String,String)->Unit)?=null,onDelete:(()->Unit)?=null){
 var name by remember(course){mutableStateOf(course)}
 var draft by remember(course){mutableStateOf(store.value(course))}
 val original=remember(course){store.value(course)}
 var showColorPicker by remember{mutableStateOf(false)}
 var pickerStart by remember{mutableStateOf("#DBEEFA")}
 var pickerColor by remember{mutableStateOf<String?>("#DBEEFA")}
 var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)}
 val scope=rememberCoroutineScope()
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->if(uri!=null)scope.launch{busy=true;error=null;try{val next=store.preparePhoto(name.ifBlank{"new"},uri);if(draft!=original)store.discard(draft);draft=next}catch(e:Exception){error=e.message?:"圖片處理失敗"}finally{busy=false}}}
 fun dismiss(){if(!busy){if(showColorPicker)showColorPicker=false else {if(draft!=original)store.discard(draft);onDismiss()}}}
 ModernAlertDialog(onDismissRequest=::dismiss,title={Text(if(showColorPicker)"自訂顏色" else if(create)"新增課程" else if(onConfirm!=null)"編輯課程" else "自訂課程封面")},text={
  if(showColorPicker)CoverColorPicker(pickerStart){pickerColor=it}else Column(Modifier.fillMaxWidth().heightIn(max=440.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
   if(onConfirm!=null)ModernTextField(name,{name=it.take(200);error=null},label={Text("課程名稱")},singleLine=true,modifier=Modifier.fillMaxWidth())
   CourseBook(name.ifBlank{"我的新課程"},"",draft,store,{},{},Modifier.width(112.dp).align(Alignment.CenterHorizontally),false)
   covers.chunked(6).forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){row.forEach{(id,label,color)->IconButton(onClick={if(draft!=original)store.discard(draft);draft="preset:$id"},enabled=!busy,modifier=Modifier.size(40.dp).background(coverColor(color),CircleShape).semantics{contentDescription=label}){if(draft=="preset:$id")Icon(Icons.Default.Check,null,tint=Color(0xFF193D5C))}}}}
   OutlinedButton(onClick={pickerStart=String.format(java.util.Locale.ROOT,"#%06X",coverColor(draft).toArgb() and 0xFFFFFF);pickerColor=pickerStart;showColorPicker=true},enabled=!busy,modifier=Modifier.fillMaxWidth().semantics{contentDescription="自訂封面顏色"}){
       Box(Modifier.size(24.dp).background(coverColor(draft),CircleShape));Spacer(Modifier.width(10.dp));Text("自訂顏色",Modifier.weight(1f));Icon(Icons.Default.ChevronRight,null)
   }
   OutlinedButton(onClick={picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},enabled=!busy,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.PhotoLibrary,null,Modifier.size(22.dp));Spacer(Modifier.width(8.dp));Text(if(busy)"處理圖片中…" else "選擇自己的圖片")}
   error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
  }
 },confirmButton={TextButton(enabled=!busy&&(if(showColorPicker)pickerColor!=null else name.isNotBlank()),onClick={if(showColorPicker){pickerColor?.let{if(draft!=original)store.discard(draft);draft=it};showColorPicker=false}else runCatching{if(onConfirm!=null)onConfirm(name.trim(),draft) else {store.set(course,draft);onSaved()}}.onFailure{error=it.message}}){Text(if(showColorPicker)"使用此顏色" else if(create)"建立課程" else "儲存")}},dismissButton={if(!showColorPicker&&onDelete!=null)TextButton(enabled=!busy,onClick={dismiss();onDelete()}){Text("刪除課程",color=MaterialTheme.colorScheme.error)};TextButton(enabled=!busy,onClick=::dismiss){Text("取消")}})
}
