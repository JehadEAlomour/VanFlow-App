package com.jehadalomour.flowvan.screens.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ComingSoonScreen(titleAr: String, titleEn: String, phaseLabel: String, onBack: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Text("←", color = Fv.TextHigh, fontSize = 22.sp) }
                Text(titleAr, color = Fv.TextHigh, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("🚧", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text(titleAr, color = Fv.TextHigh, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(titleEn, color = Fv.TextMid, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text("قادم في $phaseLabel", color = Fv.Blue, fontSize = 12.sp)
            }
        }
    }
}
