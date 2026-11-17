package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip

object SelectionIcons {
    val Multiple: ImageVector by lazy { ImageVector.Builder("MultipleSelection",24.dp,24.dp,24f,24f).apply {
        path(fill=null,stroke=androidx.compose.ui.graphics.SolidColor(Color.Black),strokeLineWidth=1.8f,strokeLineCap=androidx.compose.ui.graphics.StrokeCap.Round,strokeLineJoin=androidx.compose.ui.graphics.StrokeJoin.Round) { moveTo(19f,13f);lineTo(19f,20f);lineTo(3f,20f);lineTo(3f,4f);lineTo(13f,4f);moveTo(7f,11f);lineTo(11f,15f);lineTo(21f,5f);moveTo(17f,2f);lineTo(18f,4f);moveTo(22f,9f);lineTo(20f,8f);moveTo(23f,2f);lineTo(21f,3f) }
    }.build() }
    val All: ImageVector by lazy { ImageVector.Builder("SelectAllCards",24.dp,24.dp,24f,24f).apply {
        path(fill=null,stroke=androidx.compose.ui.graphics.SolidColor(Color.Black),strokeLineWidth=1.8f,strokeLineCap=androidx.compose.ui.graphics.StrokeCap.Round,strokeLineJoin=androidx.compose.ui.graphics.StrokeJoin.Round) { moveTo(4f,17f);lineTo(2f,17f);lineTo(2f,2f);lineTo(17f,2f);lineTo(17f,4f);moveTo(7f,20f);lineTo(5f,20f);lineTo(5f,5f);lineTo(20f,5f);lineTo(20f,7f);moveTo(10f,8f);lineTo(20f,8f);quadTo(22f,8f,22f,10f);lineTo(22f,20f);quadTo(22f,22f,20f,22f);lineTo(10f,22f);quadTo(8f,22f,8f,20f);lineTo(8f,10f);quadTo(8f,8f,10f,8f);moveTo(12f,15f);lineTo(14.5f,17.5f);lineTo(19f,13f) }
    }.build() }
}

@Composable
fun SelectionAction(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick,enabled=enabled,modifier=Modifier.requiredSize(44.dp).clip(CircleShape).background(Color.White.copy(alpha=.12f))) { Icon(icon,label,Modifier.size(24.dp),tint=Color.White.copy(alpha=if(enabled)1f else .4f)) }
}
