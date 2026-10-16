package com.dobdmitry.drawnpets.ui.games

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dobdmitry.drawnpets.ui.common.PaperType
import com.dobdmitry.drawnpets.ui.common.paperSurface

@Composable
internal fun GameHeader(title: String, coins: Int?, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).paperSurface(GamePaper,14.dp).clickable(onClick=onBack),contentAlignment=Alignment.Center) {
            Text("‹",color=GameInk,fontSize=32.sp)
        }
        Text(title,Modifier.weight(1f).padding(horizontal=12.dp),fontFamily=PaperType,fontSize=20.sp,fontWeight=FontWeight.ExtraBold,color=GameInk)
        if(coins!=null) Row(Modifier.paperSurface(GamePaper,14.dp).padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
            GameToken("🪙",Modifier.size(24.dp))
            Text(" $coins",fontFamily=PaperType,color=GameInk,fontWeight=FontWeight.Bold)
        }
    }
}
