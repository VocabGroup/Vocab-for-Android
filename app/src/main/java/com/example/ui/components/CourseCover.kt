package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.settings.CourseCoverStore
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

private val covers = listOf("sky" to "晴空藍", "ice" to "冰川藍", "cornflower" to "矢車菊藍", "sea" to "海灣藍")
private fun coverColor(value: String) = when (value) { "preset:ice" -> Color(0xFFE6F3F7); "preset:cornflower" -> Color(0xFFDCE5FA); "preset:sea" -> Color(0xFFCFE5ED); else -> Color(0xFFDBEEFA) }

@Composable
fun CourseBook(course: String, subtitle: String, cover: String, store: CourseCoverStore, onOpen: () -> Unit, onCover: () -> Unit, modifier: Modifier = Modifier, showCoverButton: Boolean = true) {
    Box(modifier) {
        Surface(onClick = onOpen, shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp, topEnd = 22.dp, bottomEnd = 22.dp), color = coverColor(cover), shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth().aspectRatio(.8f).testTag("course_$course")) {
            Box(Modifier.fillMaxSize()) {
                val file = store.file(cover)
                if (file != null) {
                    AsyncImage(model = file, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .45f), Color.Transparent, Color.Black.copy(alpha = .6f)))))
                }
                Box(Modifier.padding(start = 8.dp).width(2.dp).fillMaxHeight().background(Color.Black.copy(alpha = .06f)))
                Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Text(course, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = if (file != null) Color.White else Color(0xFF193D5C))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = if (file != null) Color.White else Color(0xFF193D5C))
                }
            }
        }
        if (showCoverButton) FilledTonalIconButton(onClick = onCover, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 42.dp).size(40.dp), shape = CircleShape) { Icon(Icons.Default.Edit, "自訂 $course 封面", Modifier.size(18.dp)) }
    }
}

@Composable
fun CourseCoverDialog(course: String, store: CourseCoverStore, onDismiss: () -> Unit, onSaved: () -> Unit) {
    var draft by remember(course) { mutableStateOf(store.value(course)) }
    val original = remember(course) { store.value(course) }
    var busy by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            busy = true; error = null
            try { val next = store.preparePhoto(course, uri); if (draft != original) store.discard(draft); draft = next }
            catch (e: Exception) { error = e.message ?: "圖片處理失敗" }
            finally { busy = false }
        }
    }
    fun dismiss() { if (!busy) { if (draft != original) store.discard(draft); onDismiss() } }
    ModernAlertDialog(onDismissRequest = ::dismiss, title = { Text("自訂課程封面") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CourseBook(course, "", draft, store, {}, {}, Modifier.width(160.dp).align(Alignment.CenterHorizontally), showCoverButton = false)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                covers.forEach { (id, name) ->
                    IconButton(onClick = { if (draft != original) store.discard(draft); draft = "preset:$id" }, enabled = !busy, modifier = Modifier.background(coverColor("preset:$id"), CircleShape)) { if (draft == "preset:$id") Icon(Icons.Default.Check, name, tint = Color(0xFF193D5C)) else Text(name.take(1), color = Color(0xFF193D5C)) }
                }
            }
            OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "處理圖片中…" else "選擇自己的圖片") }
            Text("圖片置中裁切，僅儲存在此裝置，不會自動分享到雲端。", style = MaterialTheme.typography.bodySmall)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(enabled = !busy, onClick = { runCatching { store.set(course, draft) }.onSuccess { onSaved() }.onFailure { error = it.message } }) { Text("儲存封面") } }, dismissButton = { TextButton(enabled = !busy, onClick = ::dismiss) { Text("取消") } })
}
