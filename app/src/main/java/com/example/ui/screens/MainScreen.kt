package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.luminance
import com.example.ui.components.glassRim
import com.example.ui.components.LiquidDock
import com.example.ui.components.LiquidDockTab
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.ui.components.EditorialTopAppBar as TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.rememberResponsiveLayout
import kotlinx.coroutines.flow.distinctUntilChanged

private data class MainTabSpec(
    val label: String,
    val icon: ImageVector,
    val contentDescription: String,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    dashboardContent: @Composable () -> Unit,
    foldersContent: @Composable () -> Unit,
    studyContent: @Composable () -> Unit,
    quizContent: @Composable () -> Unit,
    libraryToolContent: @Composable () -> Unit = {},
    onOpenAssistant: () -> Unit = {},
    onOpenWordSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    grammarMode: Boolean = false,
    onToggleGrammarMode: () -> Unit = {},
    updateAvailable: Boolean = false,
    showBottomNavigation: Boolean = true,
    modifier: Modifier = Modifier
) {
    val responsive = rememberResponsiveLayout()
    val pageLayer = rememberGraphicsLayer()
    var pagePosition by remember { mutableStateOf(Offset.Zero) }
    var barPosition by remember { mutableStateOf(Offset.Zero) }
    val showNavigationLabels = !responsive.isLandscape &&
        !responsive.isSmallWidth &&
        !responsive.isLargeText
    val pagerState = rememberPagerState(
        initialPage = currentTab,
        pageCount = { 5 }
    )
    val tabs = listOf(
        MainTabSpec("今天", Icons.Default.Home, "今天", "nav_tab_home"),
        MainTabSpec("教材庫", Icons.Default.Folder, "教材庫", "nav_tab_folders"),
        MainTabSpec("練習", Icons.Default.Style, "練習", "nav_tab_study"),
        MainTabSpec("AI", Icons.Default.AutoAwesome, "Vocab AI", "nav_tab_ai")
    )
    val selectedNavigation = when(pagerState.currentPage) { 0 -> 0; 1,4 -> 1; else -> 2 }

    LaunchedEffect(currentTab) {
        if (pagerState.currentPage != currentTab) {
            pagerState.scrollToPage(currentTab)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                // Always report the settled page. The previous conditional captured
                // an old currentTab value and left the bottom bar behind after swipes.
                onTabSelected(page)
            }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (showBottomNavigation) {
                TopAppBar(
                    expandedHeight = 48.dp,
                    windowInsets = TopAppBarDefaults.windowInsets,
                    title = {
                        Crossfade(
                            targetState = pagerState.currentPage to grammarMode,
                            label = "main_page_title"
                        ) { (page, grammar) ->
                            if (page == 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(Modifier.padding(3.dp)) {
                                        listOf(false to "單字", true to "文法").forEach { (mode, label) ->
                                            Surface(
                                                onClick = { if (grammar != mode) onToggleGrammarMode() },
                                                shape = CircleShape,
                                                color = if (grammar == mode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                contentColor = if (grammar == mode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            ) {
                                                Text(
                                                    label,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    when(page) { 1 -> if(grammar) "文法教材庫" else "教材庫"; 2,3 -> "練習"; else -> if(grammar) "作文" else "文章" },
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenAssistant) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Vocab AI",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Box {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "設定",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (updateAvailable) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(8.dp)
                                            .background(MaterialTheme.colorScheme.error, CircleShape)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
        Column(Modifier.fillMaxSize()) {
            if(pagerState.currentPage in 1..4) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if(pagerState.currentPage == 1 || pagerState.currentPage == 4) {
                    TextButton(onClick = { onTabSelected(1) }) { Text(if(grammarMode) "文法庫" else "單字資料夾") }
                    TextButton(onClick = { onTabSelected(4) }, modifier = Modifier.testTag("nav_tab_library_tool")) { Text(if(grammarMode) "作文" else "文章") }
                } else {
                    TextButton(onClick = { onTabSelected(2) }) { Text("翻卡學習") }
                    TextButton(onClick = { onTabSelected(3) }, modifier = Modifier.testTag("nav_tab_quiz")) { Text("測驗練習") }
                }
            }
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onGloballyPositioned { pagePosition = it.positionInRoot() }
                .drawWithContent {
                    pageLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(pageLayer)
                }
        ) { page ->
            when (page) {
                0 -> dashboardContent()
                1 -> foldersContent()
                2 -> studyContent()
                3 -> quizContent()
                4 -> libraryToolContent()
            }
        }
        }

            if (showBottomNavigation) Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                LiquidDock(
                    tabs = tabs.map { LiquidDockTab(it.label, it.icon, it.testTag) },
                    selectedIndex = selectedNavigation,
                    onSelect = { if (it == 3) onOpenAssistant() else onTabSelected(it) },
                    onSearch = onOpenWordSearch,
                    searchLabel = if (grammarMode) "搜尋文法" else "搜尋英文單字",
                    showLabels = showNavigationLabels,
                    pageLayer = pageLayer,
                    pagePosition = pagePosition,
                    modifier = Modifier.widthIn(max = 476.dp).fillMaxWidth()
                )
            }
        }

    }
}
