package com.example.ui.screens
import com.example.ui.components.ModernCollectionRow
import com.example.ui.components.FolderSharingPanel
import com.example.ui.components.CourseBook
import com.example.ui.components.CourseCoverDialog
import com.example.data.settings.CourseCoverStore
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import com.example.data.importer.SharedFolder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import com.example.ui.components.ModernAlertDialog as AlertDialog
import com.example.ui.components.ModernButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.ModernOutlinedButton as OutlinedButton
import com.example.ui.components.ModernTextField as OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.ui.components.EditorialTopAppBar as TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Deck
import com.example.data.entity.Flashcard
import com.example.ui.components.CalmEmptyState
import com.example.ui.components.AddFolderDialog
import com.example.ui.components.EditFolderDialog
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersManagementScreen(
    decks: List<Deck>,
    allCards: List<Flashcard>,
    onAddFolder: (courseName: String, folderName: String, description: String, colorHex: String) -> Unit,
    onUpdateFolder: (Deck) -> Unit,
    onDeleteFolder: (Deck) -> Unit,
    onMoveFolder: (Deck, Int) -> Unit = { _, _ -> },
    onMoveFolderGlobally: (Deck, Int) -> Unit = { _, _ -> },
    onReorderFolders: (List<Long>) -> Unit = {},
    onRenameCourse: (String, String, (Boolean) -> Unit) -> Unit = { _, _, done -> done(false) },
    onOpenCardList: (deckId: Long) -> Unit,
    onJumpToStudy: (deckId: Long) -> Unit,
    onJumpToQuiz: (deckId: Long) -> Unit,
    onOpenAssistant: () -> Unit = {},
    onImportShare:suspend(List<SharedFolder>,String,Boolean)->Result<List<Long>> = {_,_,_->Result.failure(IllegalStateException("匯入尚未連接"))},
    onUndoShare:suspend(List<Long>)->Result<Unit> = {Result.failure(IllegalStateException("匯入尚未連接"))},
    shareUndoIds:List<Long> = emptyList(),
    scanUndoCount:Int=0,
    onUndoScan:()->Unit={},
    showTopBar: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddMenu by remember {mutableStateOf(false)}
    var shareDeckId by remember {mutableStateOf<Long?>(null)}
    var importRequested by remember {mutableStateOf(false)}
    var importLinkRequested by remember {mutableStateOf(false)}
    var confirmUndoScan by remember {mutableStateOf(false)}
    var editingDeck by remember { mutableStateOf<Deck?>(null) }
    var deckToDelete by remember { mutableStateOf<Deck?>(null) }
    var courseToRename by remember { mutableStateOf<String?>(null) }
    var renamedCourseValue by remember { mutableStateOf("") }

    val courses = remember(decks) {
        decks.map { it.category }.distinct().ifEmpty { listOf("通用課程") }
    }
    var selectedCourseFilter by remember { mutableStateOf<String?>(null) }

    var showAllFolders by rememberSaveable { mutableStateOf(false) }
    val coverContext = LocalContext.current
    val coverStore = remember(coverContext) { CourseCoverStore(coverContext) }
    var coverCourse by remember { mutableStateOf<String?>(null) }
    var coverRevision by remember { mutableStateOf(0) }
    BackHandler(enabled = selectedCourseFilter != null || showAllFolders) { selectedCourseFilter = null; showAllFolders = false }
    coverCourse?.let { course -> CourseCoverDialog(course, coverStore, { coverCourse = null }, { coverRevision++; coverCourse = null }) }
    var shareCourse by remember { mutableStateOf<String?>(null) }
    val filteredDecks = decks
    val sourceDeckIds = filteredDecks.map { it.id }
    var displayedDeckIds by remember(sourceDeckIds) { mutableStateOf(sourceDeckIds) }
    val displayedDecks = displayedDeckIds.mapNotNull { id -> filteredDecks.firstOrNull { it.id == id } }
    val folderListState = rememberLazyListState()
    var folderOrderChanged by remember { mutableStateOf(false) }
    val folderReorderState = rememberReorderableLazyListState(
        lazyListState = folderListState
    ) { from, to ->
        val fromId = from.key as? Long
        val toId = to.key as? Long
        if (fromId != null && toId != null && decks.firstOrNull { it.id == fromId }?.category == decks.firstOrNull { it.id == toId }?.category) {
            val fromIndex = displayedDeckIds.indexOf(fromId)
            val toIndex = displayedDeckIds.indexOf(toId)
            if (fromIndex in displayedDeckIds.indices && toIndex in displayedDeckIds.indices && fromIndex != toIndex) {
                val course=decks.first{it.id==fromId}.category
                val scoped=displayedDeckIds.filter{id->decks.any{it.id==id&&it.category==course}}.toMutableList()
                val target=scoped.indexOf(toId);scoped.add(target,scoped.removeAt(scoped.indexOf(fromId)))
                val scopeIds=scoped.toSet();var cursor=0
                displayedDeckIds=displayedDeckIds.map{if(it in scopeIds)scoped[cursor++] else it}
                folderOrderChanged = true
            }
        }
    }

    LaunchedEffect(folderReorderState.isAnyItemDragging) {
        if (!folderReorderState.isAnyItemDragging && folderOrderChanged) {
            onReorderFolders(displayedDeckIds)
            folderOrderChanged = false
        }
    }

    if(showAddMenu){AlertDialog(onDismissRequest={showAddMenu=false},title={Text("新增資料夾")},text={Column{TextButton(onClick={showAddMenu=false;showAddDialog=true}){Text("建立資料夾")};TextButton(onClick={showAddMenu=false;importRequested=true}){Text("匯入分享檔案")}}},confirmButton={TextButton(onClick={showAddMenu=false}){Text("取消")}})}
    if(confirmUndoScan){AlertDialog(onDismissRequest={confirmUndoScan=false},title={Text("撤回上次單字匯入？")},text={Text("移除該批新字卡與後續修改，原有字卡保留。")},confirmButton={TextButton(onClick={confirmUndoScan=false;onUndoScan()}){Text("撤回")}},dismissButton={TextButton(onClick={confirmUndoScan=false}){Text("取消")}})}
    if (showAddDialog) {
        AddFolderDialog(
            existingCourses = courses,
            defaultCourse = selectedCourseFilter,
            onDismiss = { showAddDialog = false },
            onConfirm = { courseName, folderName, description, colorHex ->
                showAddDialog = false
                onAddFolder(courseName, folderName, description, colorHex)
            }
        )
    }

    editingDeck?.let { deck ->
        EditFolderDialog(
            deck = deck,
            existingCourses = courses,
            onDismiss = { editingDeck = null },
            onConfirm = { updated ->
                editingDeck = null
                onUpdateFolder(updated)
            }
        )
    }

    deckToDelete?.let { deck ->
        AlertDialog(
            onDismissRequest = { deckToDelete = null },
            title = { Text("確定刪除資料夾「${deck.name}」？", fontWeight = FontWeight.Bold) },
            text = { Text("刪除此資料夾將一併移除其中的所有單字卡，此操作無法復原。") },
            confirmButton = {
                Button(
                    onClick = {
                        deckToDelete = null
                        onDeleteFolder(deck)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("確定刪除", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deckToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    courseToRename?.let { originalName ->
        AlertDialog(
            onDismissRequest = { courseToRename = null },
            title = { Text("重新命名課程", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renamedCourseValue,
                    onValueChange = { renamedCourseValue = it },
                    label = { Text("課程名稱") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newName = renamedCourseValue.trim()
                        onRenameCourse(originalName, newName) { success ->
                            if (success) { coverStore.rename(originalName, newName); coverRevision++; selectedCourseFilter = newName }
                            courseToRename = null
                        }
                    },
                    enabled = renamedCourseValue.isNotBlank() &&
                        renamedCourseValue.trim() != originalName
                ) {
                    Text("儲存")
                }
            },
            dismissButton = {
                TextButton(onClick = { courseToRename = null }) { Text("取消") }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (showTopBar) TopAppBar(
                expandedHeight = 48.dp,
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("教材庫", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                },
                actions = {
                    IconButton(onClick = onOpenAssistant) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Vocab AI")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddMenu = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 98.dp).testTag("add_folder_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "新增資料夾")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
        FolderSharingPanel(decks,allCards,selectedCourseFilter,shareDeckId,{shareDeckId=null},importRequested,{importRequested=false},onImportShare,onUndoShare,shareUndoIds,requestCourse=shareCourse,onCourseRequestHandled={shareCourse=null},showActions=false,importLinkRequested=importLinkRequested,onLinkRequestHandled={importLinkRequested=false})
        LazyColumn(
            state = folderListState,
            modifier = Modifier
                .testTag("folder_list")
                .weight(1f)
                .fillMaxWidth()
                .padding(top = if (showTopBar) 6.dp else 0.dp)
                .padding(horizontal = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                if(scanUndoCount>0)TextButton(onClick={confirmUndoScan=true}){Text("撤回上次單字匯入（$scanUndoCount 字）")}
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedCourseFilter ?: if (showAllFolders) "全部資料夾" else "我的課程", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).padding(top = 12.dp))
                    if (selectedCourseFilter != null || showAllFolders) TextButton(onClick = { selectedCourseFilter = null; showAllFolders = false }) { Text("返回課程") }
                    else TextButton(onClick = { showAllFolders = true }) { Text("全部資料夾") }
                }
                TextButton(onClick={importLinkRequested=true}) { Text("匯入分享網址") }

            }

            if (displayedDecks.isEmpty()) {
                item {
                    CalmEmptyState(
                        icon = Icons.Default.Folder,
                        title = "還沒有資料夾",
                        message = "點右下角新增第一個資料夾。"
                    )
                }
            } else {
                if (selectedCourseFilter == null && !showAllFolders) items(courses.chunked(2), key = { it.joinToString("|") }) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { course ->
                            val courseDecks = decks.filter { it.category == course }
                            val count = allCards.count { card -> courseDecks.any { it.id == card.deckId } }
                            val cover = remember(course, coverRevision) { coverStore.value(course) }
                            CourseBook(course, "${courseDecks.size} 個資料夾 · $count 字", cover, coverStore,
                                onOpen = { selectedCourseFilter = course }, onCover = { coverCourse = course }, modifier = Modifier.weight(1f))
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                courses.filter { showAllFolders || it == selectedCourseFilter }.forEach { course ->
                    item(key = "course:$course") {
                        if (showAllFolders) Text(course, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row {
                            IconButton(onClick = { shareCourse = course }) { Icon(Icons.Default.Share, "分享課程") }
                            IconButton(onClick = { selectedCourseFilter = course; showAddMenu = true }) { Icon(Icons.Default.Add,"新增教材") }
                            IconButton(onClick = { coverCourse = course }) { Icon(Icons.Default.PhotoLibrary, "自訂課程封面") }
                            IconButton(onClick = { renamedCourseValue = course; courseToRename = course }) { Icon(Icons.Default.Edit, "重新命名課程") }
                        }
                    }
                    itemsIndexed(displayedDecks.filter { it.category == course }, key = { _, deck -> deck.id }) { _, deck ->
                    val deckCards = allCards.filter { it.deckId == deck.id }
                    val totalCardCount = deckCards.size
                    val masteredCount = deckCards.count { it.isMastered }
                    val dueCount = deckCards.count { it.nextReviewTimestamp <= System.currentTimeMillis() && !it.isMastered && !it.isSuspended }

                    val deckColor = try {
                        Color(android.graphics.Color.parseColor(deck.colorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    ReorderableItem(
                        state = folderReorderState,
                        key = deck.id
                    ) {
ModernCollectionRow(title = deck.name, subtitle = "", count = totalCardCount, due = dueCount.takeIf { it > 0 }, progress = if (totalCardCount > 0) masteredCount.toFloat() / totalCardCount else null, onManage = { onOpenCardList(deck.id) }, onLearn = { onJumpToStudy(deck.id) }, onQuiz = { onJumpToQuiz(deck.id) }, onEdit = { editingDeck = deck }, onDelete = { deckToDelete = deck }, onShare = { shareDeckId = deck.id }, dragModifier = Modifier.draggableHandle())
                    }
                }
            }

            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        }
    }
}
