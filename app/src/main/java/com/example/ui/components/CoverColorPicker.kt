package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.util.Locale

/** Native Compose controls; no WebView, network, or extra picker application. */
@Composable
fun CoverColorPicker(initial:String,onColorChanged:(String?)->Unit){
    val initialHsv=remember(initial){FloatArray(3).apply{android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(initial),this)}}
    var hue by remember(initial){mutableFloatStateOf(initialHsv[0])}
    var saturation by remember(initial){mutableFloatStateOf(initialHsv[1])}
    var brightness by remember(initial){mutableFloatStateOf(initialHsv[2])}
    var hex by remember(initial){mutableStateOf(initial.uppercase(Locale.ROOT))}
    fun emit(){hex=String.format(Locale.ROOT,"#%06X",android.graphics.Color.HSVToColor(floatArrayOf(hue,saturation,brightness)) and 0xFFFFFF);onColorChanged(hex)}
    val preview=Color(android.graphics.Color.HSVToColor(floatArrayOf(hue,saturation,brightness)))
    Column(Modifier.fillMaxWidth().heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
            Box(Modifier.size(44.dp).clip(CircleShape).background(preview).semantics{contentDescription="顏色預覽"})
            Text("拖曳色盤選色，再調整下方色相。",style=MaterialTheme.typography.bodyMedium)
        }
        Canvas(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp)).testTag("cover_color_board")
            .semantics{contentDescription="自訂顏色色盤，可拖曳調整飽和度與亮度，也可直接輸入色碼"}
            .pointerInput(Unit){
                awaitEachGesture{
                    val down=awaitFirstDown(requireUnconsumed=false)
                    fun select(point:Offset){if(size.width>0&&size.height>0){saturation=(point.x/size.width).coerceIn(0f,1f);brightness=1f-(point.y/size.height).coerceIn(0f,1f);emit()}}
                    select(down.position);down.consume()
                    do{val event=awaitPointerEvent();event.changes.filter{it.pressed}.forEach{select(it.position);it.consume()}}while(event.changes.any{it.pressed})
                }
            }){
            drawRect(Brush.horizontalGradient(listOf(Color.White,Color.hsv(hue,1f,1f))))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent,Color.Black)))
            val point=Offset(saturation*size.width,(1f-brightness)*size.height)
            drawCircle(Color.Black,8.dp.toPx(),point,style=Stroke(3.dp.toPx()))
            drawCircle(Color.White,8.dp.toPx(),point,style=Stroke(2.dp.toPx()))
        }
        Text("色相",style=MaterialTheme.typography.labelMedium)
        Box(contentAlignment=Alignment.Center){
            Box(Modifier.fillMaxWidth().padding(horizontal=12.dp).height(12.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(Color.Red,Color.Yellow,Color.Green,Color.Cyan,Color.Blue,Color.Magenta,Color.Red))))
            Slider(value=hue,onValueChange={hue=it;emit()},valueRange=0f..360f,colors=SliderDefaults.colors(thumbColor=Color.hsv(hue,1f,1f),activeTrackColor=Color.Transparent,inactiveTrackColor=Color.Transparent),modifier=Modifier.fillMaxWidth().semantics{contentDescription="色相"})
        }
        ModernTextField(hex,{value->
            hex=value.take(7)
            if(hex.matches(Regex("#[a-fA-F0-9]{6}"))){val hsv=FloatArray(3);android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(hex),hsv);hue=hsv[0];saturation=hsv[1];brightness=hsv[2];onColorChanged(hex.uppercase(Locale.ROOT))}else onColorChanged(null)
        },label={Text("色碼 #RRGGBB")},singleLine=true,isError=!hex.matches(Regex("#[a-fA-F0-9]{6}")),modifier=Modifier.fillMaxWidth().testTag("cover_color_hex"))
    }
}
