package com.example.ui.screens
import androidx.compose.material3.FilledTonalIconButton

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.result.IntentSenderRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PhotoLibrary
import com.example.ui.components.ModernButton as Button
import com.example.ui.components.ModernCard as Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.ModernOutlinedButton as OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.activity.compose.BackHandler
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ui.components.ModernAlertDialog
import coil.compose.AsyncImage
import com.example.util.ScanInstructions
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.Deck
import com.example.data.api.GeminiService
import com.example.util.AiImagePreprocessor
import com.example.util.DocumentParser
import com.example.util.OcrCardCandidate
import com.example.util.OcrWordParser
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PhotoOcrScreen(
    decks: List<Deck>,
    candidates: List<OcrCardCandidate>,
    initialDeckId: Long? = null,
    onImportCandidates: (List<OcrCardCandidate>, Long,(Boolean)->Unit) -> Unit,
    onBack: () -> Unit,
    existingCards:List<com.example.data.entity.Flashcard> = emptyList(),
    onImportToNewFolder: (List<OcrCardCandidate>,Long,(Boolean)->Unit)->Unit = {_,_,done->done(false)},
    aiAvailable: Boolean = false,
    pdfPageLimit: Int = 20,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedDeckId by remember(decks, initialDeckId) {
        mutableStateOf(
            initialDeckId?.takeIf { requestedId -> decks.any { it.id == requestedId } }
                ?: decks.firstOrNull()?.id
        )
    }
    var isOcrScanning by remember { mutableStateOf(false) }
    var isScannerLaunching by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var isAiEnriching by remember { mutableStateOf(false) }
    var recognitionProgress by remember { mutableStateOf("") }
    var scanMode by remember { mutableStateOf("主單字／標題") }
    var customScan by remember { mutableStateOf("") }
    val pendingImages=remember{mutableStateListOf<Uri>()}
    val completedImages=remember{mutableStateListOf<Uri>()}
    val imageErrors=remember{androidx.compose.runtime.mutableStateMapOf<Uri,String>()}
    var imageJob by remember{mutableStateOf<Job?>(null)}
    var confirmLeave by remember{mutableStateOf(false)}
    var destinationExpanded by remember { mutableStateOf(false) }
    var modeExpanded by remember { mutableStateOf(false) }
    var previewImage by remember { mutableStateOf<Uri?>(null) }
    var showImageQueue by remember { mutableStateOf(false) }
    var importing by remember{mutableStateOf(false)}
    var editingCandidate by remember{mutableStateOf<Int?>(null)}
    var editWord by remember{mutableStateOf("")}
    var editDefinition by remember{mutableStateOf("")}
    DisposableEffect(Unit){onDispose{imageJob?.cancel()}}
    BackHandler(enabled=isOcrScanning){confirmLeave=true}

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val localCandidates = remember(candidates) {
        mutableStateListOf<OcrCardCandidate>().apply { addAll(candidates) }
    }

    LaunchedEffect(decks) {
        if (decks.none { it.id == selectedDeckId }) {
            selectedDeckId = decks.firstOrNull()?.id
        }
    }

    val processScannedUri: (Uri, String) -> Unit = { uri, source ->
        isOcrScanning = true
        recognitionProgress = "正在辨識 1 / 1 張"
        coroutineScope.launch {
            runCatching { DocumentParser.parseDocumentToCards(context, uri, pdfPageLimit) }
                .onSuccess { (text, parsed) ->
                    isOcrScanning = false
                    recognitionProgress = ""
                    localCandidates.clear()
                    localCandidates.addAll(parsed)
                    snackbarHostState.showSnackbar(
                        when {
                            text.isBlank() -> "${source}未讀取到文字，請換一張更清晰的影像"
                            parsed.isEmpty() -> "${source}已讀到文字，但沒有找到可匯入的英文單字"
                            else -> "${source}完成，找到 ${parsed.size} 張單字卡"
                        }
                    )
                }
                .onFailure {
                    isOcrScanning = false
                    recognitionProgress = ""
                    snackbarHostState.showSnackbar("${source}失敗：${it.message ?: "無法讀取內容"}")
                }
        }
    }

    val processImageUris: (List<Uri>, String) -> Unit = processImages@ { uris, source ->
        if (!aiAvailable) {coroutineScope.launch {snackbarHostState.showSnackbar("請先啟用個人 AI API 並選擇支援圖片的模型")};return@processImages}
        val selectedUris=uris.distinct().filter{it !in completedImages}
        if(selectedUris.isEmpty()||isOcrScanning)return@processImages
        isOcrScanning=true
        imageJob=coroutineScope.launch {
            try {
                selectedUris.forEachIndexed{index,uri->
                    recognitionProgress="正在辨識 ${index+1}／${selectedUris.size} 張"
                    try{
                        val payload=AiImagePreprocessor.prepare(context,uri)
                        val parsed=GeminiService.analyzeImages(listOf(payload),ScanInstructions.build(scanMode,customScan))
                        parsed.forEach{candidate->if(localCandidates.none{it.word.trim().equals(candidate.word.trim(),true)})localCandidates.add(candidate)}
                        completedImages.add(uri);imageErrors.remove(uri)
                    }catch(error:Exception){if(error is CancellationException)throw error;imageErrors[uri]=error.localizedMessage?:"辨識失敗"}
                }
                snackbarHostState.showSnackbar("$source：已完成 ${completedImages.size} 張，成功結果保留；失敗可單獨重試")
            }finally{isOcrScanning=false;recognitionProgress="";imageJob=null}
        }
    }
    fun enqueueImages(uris:List<Uri>){if(pendingImages.size+uris.size>30){coroutineScope.launch{snackbarHostState.showSnackbar("單次最多 30 張，請分批匯入")};return};uris.forEach{if(it !in pendingImages)pendingImages.add(it)}}


    val documentScannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        isScannerLaunching = false
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val pageUris = runCatching {
                GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)?.pages.orEmpty().map { it.imageUri }
            }.getOrElse {
                coroutineScope.launch { snackbarHostState.showSnackbar("無法讀取掃描結果，請重新掃描或改用相片") }
                emptyList()
            }
            if (pageUris.isNotEmpty()) {
                enqueueImages(pageUris)
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("文件掃描沒有回傳可傳送給 AI 的頁面圖片")
                }
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("已取消文件掃描")
            }
        }
    }

    val scannerOptions = remember(pdfPageLimit) {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(pdfPageLimit.coerceIn(1, 50))
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }

    // Full-resolution camera capture. TakePicturePreview only returns a thumbnail
    // and is not reliable enough for a full vocabulary page.
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { saved ->
        val capturedUri = pendingCameraUri
        pendingCameraUri = null
        if (saved && capturedUri != null) {
            enqueueImages(listOf(capturedUri))
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("已取消拍照")
            }
        }
    }

    // Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        enqueueImages(uris)
    }
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val mimeType = context.contentResolver.getType(it).orEmpty()
            val uriText = it.toString().lowercase()
            val isImage = mimeType.startsWith("image/") ||
                listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".heic", ".heif")
                    .any(uriText::endsWith)
            if (isImage) {
                enqueueImages(listOf(it))
            } else {
                processScannedUri(it, "檔案分析")
            }
        }
    }

    val candidateListState = rememberLazyListState()
    fun enrichCandidates() {
        if (isAiEnriching || !aiAvailable) return
        isAiEnriching = true
        coroutineScope.launch {
            snackbarHostState.showSnackbar("AI 正在分析並補齊單字資訊")
            val currentList = localCandidates.toList()
            runCatching {
                val aiEnriched = GeminiService.batchEnrichCandidates(currentList)
                OcrWordParser.enrichCandidatesWithDictionary(aiEnriched)
            }.onSuccess { finalEnriched ->
                localCandidates.clear()
                localCandidates.addAll(finalEnriched)
                val serviceError = GeminiService.lastError.value
                snackbarHostState.showSnackbar(
                    when {
                        serviceError != null -> "AI 補齊失敗：$serviceError"
                        finalEnriched != currentList -> "AI 補齊完成，請確認內容"
                        else -> "AI 沒有回傳可套用的新資料"
                    }
                )
            }.onFailure { error ->
                snackbarHostState.showSnackbar("AI 補齊失敗：${error.localizedMessage ?: "請檢查 API 設定"}")
            }
            isAiEnriching = false
        }
    }
    editingCandidate?.let { index ->
        ModernAlertDialog(
            onDismissRequest = { editingCandidate = null },
            title = { Text("編輯辨識內容") },
            text = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editWord, onValueChange = { editWord = it }, label = { Text("單字") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editDefinition, onValueChange = { editDefinition = it }, label = { Text("解釋") }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(enabled = editWord.isNotBlank(), onClick = {
                if (index in localCandidates.indices) localCandidates[index] = localCandidates[index].copy(word = editWord.trim(), definition = editDefinition.trim())
                editingCandidate = null
            }) { Text("儲存") } },
            dismissButton = { TextButton(onClick = { editingCandidate = null }) { Text("取消") } }
        )
    }
    fun importCandidates() {
        val deckId = selectedDeckId ?: return
        if(importing)return;importing=true
        val done:(Boolean)->Unit={success->importing=false;if(!success)coroutineScope.launch{snackbarHostState.showSnackbar("匯入未完成，辨識結果保留，請檢查儲存空間後重試")}}
        onImportCandidates(localCandidates.filter { it.isSelected }, deckId,done)
    }

    if(confirmLeave){androidx.compose.material3.AlertDialog(onDismissRequest={confirmLeave=false},title={Text("作業仍在進行")},text={Text("離開會取消尚未完成的圖片辨識，確定離開嗎？")},confirmButton={TextButton(onClick={imageJob?.cancel();confirmLeave=false;onBack()}){Text("取消作業並離開")}},dismissButton={TextButton(onClick={confirmLeave=false}){Text("繼續等待")}})}
    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 48.dp,
                title = { Text("掃描匯入", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = {if(isOcrScanning||isAiEnriching)confirmLeave=true else onBack()}) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (localCandidates.isNotEmpty()) {
                Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = ::enrichCandidates,
                            enabled = aiAvailable && !isAiEnriching && !isOcrScanning && !importing,
                            modifier = Modifier.weight(1f).testTag("ai_batch_enrich_button")
                        ) {
                            if (isAiEnriching) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text(if (isAiEnriching) "補齊中…" else "AI 補齊")
                        }
                        Button(
                            onClick = ::importCandidates,
                            enabled = selectedDeckId != null && localCandidates.any { it.isSelected } && !isOcrScanning && !isAiEnriching && !importing,
                            modifier = Modifier.weight(1f).testTag("batch_import_button")
                        ) { Text("匯入（${localCandidates.count { it.isSelected }}）", maxLines = 1) }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 6.dp)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (decks.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "尚未建立資料夾；可以先辨識，但無法匯入。",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Row(Modifier.fillMaxWidth().testTag("scan_options_row"), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.weight(1f)) {
                    TextButton(enabled = decks.isNotEmpty() && !isOcrScanning && !importing, onClick = { destinationExpanded = true }, modifier = Modifier.fillMaxWidth().testTag("scan_destination")) {
                        val deck = decks.firstOrNull { it.id == selectedDeckId }
                        Text(deck?.let { "${it.category} › ${it.name}" } ?: "選擇資料夾", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = destinationExpanded, onDismissRequest = { destinationExpanded = false }) {
                        decks.forEach { deck ->
                            DropdownMenuItem(text = { Text("${deck.category} › ${deck.name}") }, onClick = { selectedDeckId = deck.id; destinationExpanded = false })
                        }
                    }
                }
                Box(Modifier.weight(1f)) {
                    TextButton(enabled = !isOcrScanning && !isAiEnriching && !importing, onClick = { modeExpanded = true }, modifier = Modifier.fillMaxWidth().testTag("scan_scope")) {
                        Text(scanMode, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) {
                        listOf("主單字／標題", "全部明列詞條", "自訂範圍").forEach { mode ->
                            DropdownMenuItem(text = { Text(mode) }, onClick = { scanMode = mode; modeExpanded = false })
                        }
                    }
                }
            }
            if (scanMode == "自訂範圍") OutlinedTextField(value = customScan, onValueChange = { customScan = it }, enabled = !isOcrScanning, label = { Text("例如：只辨識左欄畫線的主單字") }, maxLines = 2, modifier = Modifier.fillMaxWidth())

            if (!aiAvailable) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "拍照與相片辨識需要先在設定中啟用個人 AI API。",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            val sourceButtonsEnabled = !isOcrScanning && !isScannerLaunching && !isAiEnriching && !importing

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalIconButton(
                    onClick = {
                        runCatching {
                            val photoFile = File.createTempFile(
                                "vocab_capture_",
                                ".jpg",
                                context.cacheDir
                            )
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                photoFile
                            )
                            pendingCameraUri = uri
                            cameraLauncher.launch(uri)
                        }.onFailure { error ->
                            pendingCameraUri = null
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    "無法開啟相機：${error.localizedMessage ?: "請確認裝置有可用的相機應用程式"}"
                                )
                            }
                        }
                    },
                    enabled = sourceButtonsEnabled && aiAvailable,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("take_photo_ocr_button")
                ) { ScanSourceLabel(Icons.Default.CameraAlt, "拍照") }
                FilledTonalIconButton(
                    onClick = {
                        val activity = context.findActivity()
                        if (activity == null) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("此裝置無法啟動文件掃描")
                            }
                        } else {
                            isScannerLaunching = true
                            runCatching { GmsDocumentScanning.getClient(scannerOptions).getStartScanIntent(activity) }
                                .onFailure { error ->
                                    isScannerLaunching = false
                                    coroutineScope.launch { snackbarHostState.showSnackbar("文件掃描無法啟動：${error.localizedMessage ?: "此裝置不支援掃描服務"}") }
                                }
                                .getOrNull()
                                ?.addOnSuccessListener { sender ->
                                    documentScannerLauncher.launch(IntentSenderRequest.Builder(sender).build())
                                }
                                ?.addOnFailureListener { error ->
                                    isScannerLaunching = false
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            "文件掃描無法啟動：${error.localizedMessage ?: "請確認 Google Play 服務與網路"}"
                                        )
                                    }
                                }
                        }
                    },
                    enabled = sourceButtonsEnabled && aiAvailable,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("document_scanner_button")
                ) { ScanSourceLabel(Icons.Default.DocumentScanner, "掃描") }
                FilledTonalIconButton(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    enabled = sourceButtonsEnabled && aiAvailable,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("select_photo_ocr_button")
                ) { ScanSourceLabel(Icons.Default.PhotoLibrary, "相片") }
                FilledTonalIconButton(
                    onClick = {
                        docPickerLauncher.launch(
                            arrayOf(
                                "application/pdf",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "text/plain",
                                "text/csv",
                                "text/tab-separated-values",
                                "application/json",
                                "image/*"
                            )
                        )
                    },
                    enabled = sourceButtonsEnabled,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("select_file_doc_button")
                ) { ScanSourceLabel(Icons.Default.Folder, "檔案") }
            }

            if (pendingImages.isNotEmpty() && localCandidates.isNotEmpty()) {
                TextButton(onClick = { showImageQueue = !showImageQueue }, modifier = Modifier.fillMaxWidth()) {
                    Text("圖片 ${pendingImages.size} 張 · 完成 ${completedImages.size} 張${if (imageErrors.isEmpty()) "" else " · 失敗 ${imageErrors.size} 張"}", modifier = Modifier.weight(1f), maxLines = 1)
                    Icon(if (showImageQueue) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, "展開或收合圖片")
                }
            }
            if (pendingImages.isNotEmpty() && (localCandidates.isEmpty() || showImageQueue || isOcrScanning || pendingImages.any { it !in completedImages })) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().testTag("scan_image_queue")) {
                    itemsIndexed(pendingImages, key = { _, uri -> uri.toString() }) { index, uri ->
                        Column(Modifier.width(104.dp)) {
                            TextButton(onClick = { previewImage = uri }, modifier = Modifier.fillMaxWidth()) {
                                Box {
                                    AsyncImage(model = uri, contentDescription = "預覽第 ${index + 1} 張圖片", contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp))
                                    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = .9f), modifier = Modifier.align(Alignment.BottomEnd)) {
                                        Text("${index + 1} · ${if (uri in completedImages) "完成" else if (uri in imageErrors) "失敗" else "待辨識"}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            Row {
                                IconButton(enabled = sourceButtonsEnabled && index > 0, onClick = { val previous = pendingImages[index - 1]; pendingImages[index - 1] = uri; pendingImages[index] = previous }) { Icon(Icons.Default.KeyboardArrowUp, "第 ${index + 1} 張向前移動") }
                                IconButton(enabled = sourceButtonsEnabled, onClick = { pendingImages.remove(uri); imageErrors.remove(uri); completedImages.remove(uri) }) { Icon(Icons.Default.DeleteOutline, "移除第 ${index + 1} 張圖片") }
                            }
                        }
                    }
                }
                if (!isOcrScanning && pendingImages.any { it !in completedImages }) {
                    Button(enabled = sourceButtonsEnabled && aiAvailable && (scanMode != "自訂範圍" || customScan.isNotBlank()), onClick = { processImageUris(pendingImages.toList(), "圖片辨識") }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (imageErrors.isEmpty()) "開始辨識（${pendingImages.count { it !in completedImages }} 張）" else "重試未完成圖片（${pendingImages.count { it !in completedImages }} 張）", maxLines = 1)
                    }
                }
            }

            if (isOcrScanning || isScannerLaunching) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text(
                            if (isScannerLaunching) "正在準備文件掃描…" else recognitionProgress.ifBlank { "正在辨識圖片…" },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 2
                        )
                        if (imageJob != null) TextButton(onClick = { imageJob?.cancel() }) { Text("停止") }
                    }
                }
            }

            // Extracted Candidates Table List
            if (localCandidates.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("已選 ${localCandidates.count { it.isSelected }}／${localCandidates.size}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    TextButton(enabled = sourceButtonsEnabled, onClick = {
                        val select = localCandidates.any { !it.isSelected }
                        localCandidates.indices.forEach { localCandidates[it] = localCandidates[it].copy(isSelected = select) }
                    }) { Text(if (localCandidates.all { it.isSelected }) "取消全選" else "全選") }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        state = candidateListState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize().testTag("scan_results")
                    ) {
                        itemsIndexed(localCandidates) { index, candidate ->
                        var actionsExpanded by remember(candidate.word) { mutableStateOf(false) }
                        var showExamples by remember(candidate.word) { mutableStateOf(false) }
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = candidate.isSelected,
                                    enabled = sourceButtonsEnabled,
                                    onCheckedChange = { checked ->
                                        localCandidates[index] = candidate.copy(isSelected = checked)
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    if(existingCards.any{it.deckId==selectedDeckId&&it.normalizedWord==candidate.word.trim().lowercase()})Text("目標資料夾已有此單字",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.labelSmall)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = candidate.word,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (candidate.partOfSpeech.isNotBlank()) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.widthIn(max = 64.dp)
                                            ) {
                                                Text(
                                                    text = candidate.partOfSpeech,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (candidate.phonetic.isNotBlank()) Text(candidate.phonetic, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = candidate.definition.ifBlank { "待補齊" },
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                    )
                                    if (showExamples && candidate.exampleSentence.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "例: ${candidate.exampleSentence}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                    if (showExamples && candidate.exampleTranslation.isNotBlank()) {
                                        Text(
                                            text = candidate.exampleTranslation,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                Box {
                                    IconButton(enabled = sourceButtonsEnabled, onClick = { actionsExpanded = true }) { Icon(Icons.Default.MoreVert, "${candidate.word} 的操作") }
                                    DropdownMenu(expanded = actionsExpanded, onDismissRequest = { actionsExpanded = false }) {
                                        DropdownMenuItem(text = { Text("編輯") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { actionsExpanded = false; editingCandidate = index; editWord = candidate.word; editDefinition = candidate.definition })
                                        if (candidate.exampleSentence.isNotBlank() || candidate.exampleTranslation.isNotBlank()) DropdownMenuItem(text = { Text(if (showExamples) "收起例句" else "顯示例句") }, onClick = { actionsExpanded = false; showExamples = !showExamples })
                                        DropdownMenuItem(text = { Text("移除") }, leadingIcon = { Icon(Icons.Default.DeleteOutline, null) }, onClick = { actionsExpanded = false; localCandidates.removeAt(index) })
                                    }
                                }
                            }
                        }
                        }
                    }
                }
            } else if (pendingImages.isNotEmpty()) {
                val activeUri = pendingImages.firstOrNull { it !in completedImages } ?: pendingImages.first()
                AsyncImage(model = activeUri, contentDescription = "目前辨識圖片", contentScale = ContentScale.Fit, modifier = Modifier.weight(1f).fillMaxWidth().testTag("scan_large_preview"))
            } else if (!isOcrScanning && !isScannerLaunching) {
                Text("拍照、掃描文件，或選擇多張相片開始辨識。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
            }
        }
    }
    previewImage?.let { uri ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { previewImage = null }) {
            Surface(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth()) {
                    TextButton(onClick = { previewImage = null }, modifier = Modifier.align(Alignment.End)) { Text("關閉預覽") }
                    AsyncImage(model = uri, contentDescription = "圖片完整預覽", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp))
                    imageErrors[uri]?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ScanSourceLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, null, Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
