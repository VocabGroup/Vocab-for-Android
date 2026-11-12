package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class LiquidDockTab(val label: String, val icon: ImageVector, val tag: String)

/** Separate search circle and one lifted, spring-driven selection lens. */
@Composable
fun LiquidDock(
    tabs: List<LiquidDockTab>, selectedIndex: Int, onSelect: (Int) -> Unit,
    onSearch: () -> Unit, searchLabel: String, showLabels: Boolean,
    pageLayer: GraphicsLayer, pagePosition: Offset, modifier: Modifier = Modifier
) {
    val lift = remember { Animatable(0f) }
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(selectedIndex) {
        if (initialized) {
            launch { lift.animateTo(1f, spring(.7f, 700f)); lift.animateTo(0f, spring(.58f, 220f)) }
        }
        initialized = true
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BoxWithConstraints(Modifier.weight(1f).height(66.dp).testTag("glass_navigation")) {
            GlassBackdrop(Modifier.matchParentSize(), pageLayer, pagePosition)
            val width = (maxWidth - 10.dp) / tabs.size
            val x by animateDpAsState(width * selectedIndex, spring(.62f, 320f), label = "glass_bubble_position")
            Box(Modifier.padding(start = 5.dp, top = 5.dp).offset(x = x).width(width).height(56.dp)
                .graphicsLayer { translationY = -7.dp.toPx() * lift.value; scaleX = 1f + .13f * lift.value; scaleY = 1f + .22f * lift.value }
                .shadow(6.dp, CircleShape, ambientColor = Color.Black.copy(alpha = .1f), spotColor = Color.Black.copy(alpha = .18f))
                .clip(CircleShape).background(MaterialTheme.colorScheme.surface.copy(alpha = .42f)).glassRim(CircleShape))
            Row(Modifier.fillMaxSize().padding(5.dp)) {
                tabs.forEachIndexed { index, tab ->
                    Surface(onClick = { onSelect(index) }, shape = CircleShape, color = Color.Transparent,
                        contentColor = if (index == selectedIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f).fillMaxHeight().testTag(tab.tag).semantics { selected = index == selectedIndex }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(23.dp))
                            if (showLabels) Text(tab.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                    }
                }
            }
        }
        Box(Modifier.size(66.dp)) {
            GlassBackdrop(Modifier.matchParentSize(), pageLayer, pagePosition)
            IconButton(onClick = onSearch, modifier = Modifier.fillMaxSize()) { Icon(Icons.Default.Search, searchLabel) }
        }
    }
}

@Composable
private fun GlassBackdrop(modifier: Modifier, pageLayer: GraphicsLayer, pagePosition: Offset) {
    var position by remember { mutableStateOf(Offset.Zero) }
    val fallback = android.os.Build.VERSION.SDK_INT < 31
    val dark = MaterialTheme.colorScheme.surface.luminance() < .5f
    Box(modifier.shadow(14.dp, CircleShape, ambientColor = Color.Black.copy(alpha = .14f), spotColor = Color.Black.copy(alpha = .2f))
        .clip(CircleShape).glassRim(CircleShape).onGloballyPositioned { position = it.positionInRoot() }) {
        Canvas(Modifier.matchParentSize().graphicsLayer { if (!fallback) renderEffect = BlurEffect(28f, 28f) }) {
            val delta = pagePosition - position
            with(drawContext.canvas) { save(); translate(delta.x, delta.y); drawLayer(pageLayer); restore() }
        }
        Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.surface.copy(alpha = if (fallback) .94f else if (dark) .48f else .22f)))
    }
}
