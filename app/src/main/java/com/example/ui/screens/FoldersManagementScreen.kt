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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.*
import com.example.ui.components.SelectionAction
import com.example.ui.components.SelectionIcons
import com.example.data.WorkspaceTransferResult
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
    selectedCourse:String?=null,
    onCourseSelected:((String?)->Unit)?=null,
    addRequested:Boolean=false,
    onAddHandled:()->Unit={},
    onSearch:()->Unit={},
    onSettings:()->Unit={},
    onDeleteCourse:suspend(String)->Result<Unit> = {Result.failure(IllegalStateException("刪除尚未連接"))},
    onTransfer:suspend(List<Long>,String,Boolean,Boolean)->Result<WorkspaceTransferResult> ={_,_,_,_->Result.failure(IllegalStateException("搬移尚未連接"))},
    showTopBar: Boolean = true,
    showFloatingAdd:Boolean=true,
    sharePublisher:suspend(String)->com.example.data.importer.PublishedShare=com.example.data.importer.ShareLinks::publish,
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

    var localCourse by rememberSaveable{mutableStateOf<String?>(null)}
    val selectedCourseFilter = if(onCourseSelected!=null)selectedCourse else localCourse
    fun chooseCourse(course:String?){if(onCourseSelected!=null)onCourseSelected(course)else localCourse=course}

    var showAllFolders by rememberSaveable { mutableStateOf(false) }
    val coverContext = LocalContext.current
    val coverStore = remember(coverContext) { CourseCoverStore(coverContext) }
    var coverCourse by remember { mutableStateOf<String?>(null) }
    var coverRevision by remember { mutableStateOf(0) }
    val courses = remember(decks,coverRevision) { (decks.map{it.category}+coverStore.courses()).distinct() }
    LaunchedEffect(decks){decks.map{it.category}.distinct().forEach{coverStore.register(it)}}
    var createCourse by remember{mutableStateOf(false)}
    var deletingCourse by remember{mutableStateOf<String?>(null)}
    var selectedIds by remember{mutableStateOf(setOf<Long>())}
    var selecting by remember{mutableStateOf(false)}
    var transferMode by remember{mutableStateOf<String?>(null)}
    var transferTarget by remember{mutableStateOf<String?>(null)}
    var busy by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf("")}
    var deleteSelected by remember{mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    fun transfer(target:String,overwrite:Boolean){scope.launch{busy=true;onTransfer(selectedIds.toList(),target,transferMode=="copy",overwrite).onSuccess{selectedIds=emptySet();selecting=false;transferMode=null;transferTarget=null}.onFailure{error=it.message?:"操作失敗"};busy=false}}
    LaunchedEffect(addRequested){if(addRequested){showAddMenu=true;onAddHandled()}}
    BackHandler(enabled = selecting || selectedCourseFilter != null || showAllFolders) { if(selecting){selecting=false;selectedIds=emptySet()}else{chooseCourse(null);showAllFolders=false} }
    if(createCourse||coverCourse!=null){val original=coverCourse
        CourseCoverDialog(original.orEmpty(),coverStore,{createCourse=false;coverCourse=null},{},create=createCourse,onDelete=original?.let{{deletingCourse=original}},onConfirm={name,cover->
            require(name==original||name !in courses){"已有同名課程"}
            fun finish(){coverStore.set(name,cover);coverRevision++;createCourse=false;coverCourse=null;chooseCourse(name)}
            if(original!=null&&name!=original){if(decks.any{it.category==original})onRenameCourse(original,name){ok->if(ok){coverStore.rename(original,name);finish()}else error="重新命名失敗"}else{coverStore.rename(original,name);finish()}}else finish()
        })
    }
    deletingCourse?.let{course->AlertDialog(onDismissRequest={if(!busy)deletingCourse=null},title={Text("刪除課程「$course」？")},text={Text("會一併刪除 ${decks.count{it.category==course}} 個資料夾與其中所有單字、學習紀錄，無法復原。")},confirmButton={TextButton(enabled=!busy,onClick={scope.launch{busy=true;onDeleteCourse(course).onSuccess{coverStore.remove(course);coverRevision++;deletingCourse=null;coverCourse=null;if(selectedCourseFilter==course)chooseCourse(null)}.onFailure{error=it.message?:"刪除失敗"};busy=false}}){Text(if(busy)"刪除中…" else "刪除課程",color=MaterialTheme.colorScheme.error)}},dismissButton={TextButton(enabled=!busy,onClick={deletingCourse=null}){Text("取消")}})}
    if(transferMode!=null&&transferTarget==null)AlertDialog(onDismissRequest={if(!busy)transferMode=null},title={Text(if(transferMode=="copy")"複製到課程" else "移動到課程")},text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState())){if(courses.none{it!=selectedCourse})Text("請先新增另一個課程，再搬移或複製。");courses.filter{it!=selectedCourse}.forEach{course->TextButton(enabled=!busy,onClick={val names=decks.filter{it.id in selectedIds}.map{it.name};if(decks.any{it.category==course&&it.name in names})transferTarget=course else transfer(course,false)}){Text(course)}}}},confirmButton={})
    transferTarget?.let{target->AlertDialog(onDismissRequest={if(!busy)transferTarget=null},title={Text("發現同名資料夾")},text={Text("略過會保留原資料；覆蓋會以來源內容取代目標資料夾，目標相同單字的學習紀錄保留。")},confirmButton={TextButton(enabled=!busy,onClick={transfer(target,true)}){Text("覆蓋")}},dismissButton={TextButton(enabled=!busy,onClick={transfer(target,false)}){Text("略過")}})}
    if(deleteSelected)AlertDialog(onDismissRequest={deleteSelected=false},title={Text("刪除 ${selectedIds.size} 個資料夾？")},text={Text("其中的單字與學習紀錄也會刪除，無法復原。")},confirmButton={TextButton(onClick={decks.filter{it.id in selectedIds}.forEach(onDeleteFolder);selectedIds=emptySet();selecting=false;deleteSelected=false}){Text("刪除")}},dismissButton={})
    if(error.isNotEmpty())AlertDialog(onDismissRequest={error=""},title={Text("操作未完成")},text={Text(error)},confirmButton={})
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

    if(showAddMenu){AlertDialog(onDismissRequest={showAddMenu=false},title={Text("新增教材")},text={Column{TextButton(onClick={showAddMenu=false;if(selectedCourseFilter==null)createCourse=true else showAddDialog=true}){Text(if(selectedCourseFilter==null)"建立課程" else "建立資料夾")};TextButton(onClick={showAddMenu=false;importRequested=true}){Text("匯入分享檔案")};TextButton(onClick={showAddMenu=false;importLinkRequested=true}){Text("匯入分享網址")}}},confirmButton={})}
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
                            if (success) { coverStore.rename(originalName, newName); coverRevision++; chooseCourse(newName) }
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
            if(selecting)Surface(color=Color(0xFF903A43)){Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
                SelectionAction(Icons.Default.Close,"結束多選"){selecting=false;selectedIds=emptySet()}
                Text("已選 ${selectedIds.size}",Modifier.weight(1f),color=Color.White,maxLines=1)
                SelectionAction(SelectionIcons.All,"全選"){val ids=decks.filter{selectedCourseFilter==null||it.category==selectedCourseFilter}.map{it.id}.toSet();selectedIds=if(selectedIds.containsAll(ids))emptySet()else ids}
                SelectionAction(Icons.Default.ContentCopy,"複製",selectedIds.isNotEmpty()){transferMode="copy"}
                SelectionAction(Icons.Default.DriveFileMove,"移動",selectedIds.isNotEmpty()){transferMode="move"}
                SelectionAction(Icons.Default.Delete,"刪除",selectedIds.isNotEmpty()){deleteSelected=true}
            }}else if (showTopBar) TopAppBar(
                expandedHeight = 48.dp,
                windowInsets = TopAppBarDefaults.windowInsets,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedCourseFilter?:"我的課程", maxLines=1,overflow=TextOverflow.Ellipsis,style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                },
                navigationIcon={if(selectedCourseFilter!=null)IconButton(onClick={chooseCourse(null)}){Icon(Icons.Default.ArrowBack,"返回所有課程")}},
                actions = {
                    IconButton(onClick=onSearch){Icon(Icons.Default.Search,"搜尋")}
                    if(selectedCourseFilter==null)IconButton(onClick=onSettings){Icon(Icons.Default.Settings,"設定")}else{
                        IconButton(onClick={shareCourse=selectedCourseFilter}){Icon(Icons.Default.Share,"分享課程")}
                        IconButton(onClick={selecting=true}){Icon(SelectionIcons.Multiple,"多選資料夾")}
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = { if(showFloatingAdd&&!selecting){
            FloatingActionButton(
                onClick = { showAddMenu = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 98.dp).testTag("add_folder_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "新增資料夾")
            }
        } },
        modifier = modifier
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
        FolderSharingPanel(decks,allCards,selectedCourseFilter,shareDeckId,{shareDeckId=null},importRequested,{importRequested=false},onImportShare,onUndoShare,shareUndoIds,requestCourse=shareCourse,onCourseRequestHandled={shareCourse=null},showActions=false,importLinkRequested=importLinkRequested,onLinkRequestHandled={importLinkRequested=false},publishShare=sharePublisher)
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

            }

            if (courses.isEmpty() || (selectedCourseFilter!=null&&displayedDecks.none{it.category==selectedCourseFilter})) {
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
                                onOpen = { chooseCourse(course) }, onCover = { coverCourse = course }, modifier = Modifier.weight(1f))
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                courses.filter { showAllFolders || it == selectedCourseFilter }.forEach { course ->
                    item(key = "course:$course") {
                        if (showAllFolders) Text(course, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                    ) { dragging ->
                        Surface(shape=RoundedCornerShape(20.dp),shadowElevation=if(dragging)12.dp else 0.dp,color=if(dragging||deck.id in selectedIds)MaterialTheme.colorScheme.surfaceVariant else Color.Transparent){Row(verticalAlignment=Alignment.CenterVertically){
                            if(selecting)IconButton(onClick={selectedIds=if(deck.id in selectedIds)selectedIds-deck.id else selectedIds+deck.id},modifier=Modifier.padding(start=12.dp).size(44.dp)){Icon(if(deck.id in selectedIds)Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,"選取資料夾",tint=if(deck.id in selectedIds)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)}
                            ModernCollectionRow(title = deck.name, subtitle = "", count = totalCardCount, due = dueCount.takeIf { it > 0 }, progress = if (totalCardCount > 0) masteredCount.toFloat() / totalCardCount else null, onManage = { if(selecting)selectedIds=if(deck.id in selectedIds)selectedIds-deck.id else selectedIds+deck.id else onOpenCardList(deck.id) }, onLearn = { onJumpToStudy(deck.id) }, onQuiz = { onJumpToQuiz(deck.id) }, onEdit = { editingDeck = deck }, onDelete = { deckToDelete = deck }, onShare = { shareDeckId = deck.id }, dragModifier = if(selecting)null else Modifier.draggableHandle(),onLongClick={selecting=true;selectedIds=selectedIds+deck.id},selectionMode=selecting,modifier=Modifier.weight(1f))
                        }}
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
