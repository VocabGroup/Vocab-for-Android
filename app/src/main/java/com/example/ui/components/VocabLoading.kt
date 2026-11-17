package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun VocabLoading(label:String="Vocab"){
    val transition=rememberInfiniteTransition(label="Vocab 載入")
    val scale by transition.animateFloat(.94f,1f,infiniteRepeatable(tween(900),RepeatMode.Reverse),label="呼吸")
    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)){
        Image(painterResource(R.drawable.ic_launcher_generated),"Vocab",Modifier.size(84.dp).graphicsLayer{scaleX=scale;scaleY=scale}.clip(RoundedCornerShape(24.dp)))
        Text(label,style=MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun AiThinkingDots(){
    val transition=rememberInfiniteTransition(label="AI 處理中")
    Row(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically){
        repeat(3){i->val alpha by transition.animateFloat(.25f,1f,infiniteRepeatable(tween(450,delayMillis=i*150),RepeatMode.Reverse),label="AI 點 $i")
            Text("●",color=MaterialTheme.colorScheme.primary.copy(alpha=alpha))
        }
    }
}
