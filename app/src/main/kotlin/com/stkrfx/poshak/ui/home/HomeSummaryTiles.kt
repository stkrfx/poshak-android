package com.stkrfx.poshak.ui.home

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stkrfx.poshak.data.AttendanceMonth

@Composable
internal fun SummaryGrid(month: AttendanceMonth) {
    val summary = month.summary
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Present", summary?.present ?: "—", Modifier.weight(1f), Color(0xFFD5F5E3))
            SummaryCard("Absent", summary?.absent ?: "—", Modifier.weight(1f), Color(0xFFFADBD8))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Payable days", summary?.payable ?: "—", Modifier.weight(1f), Color(0xFFFFF0B3))
            SummaryCard("Paid days", summary?.paid ?: "—", Modifier.weight(1f), Color(0xFFD6EAF8))
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier, backgroundColor: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                title,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF333333),
                textAlign = TextAlign.End,
            )
            Spacer(Modifier.height(4.dp))
            val numericValue = value.toIntOrNull()
            val animatedValue by animateIntAsState(
                targetValue = numericValue ?: 0,
                animationSpec = tween(durationMillis = 650),
                label = "summary-number-$title",
            )
            Text(
                text = numericValue?.let { animatedValue.toString() } ?: value,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF171717),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
            )
        }
    }
}
