package com.jehadalomour.flowvan.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeBar(
    fromMillis: Long,
    toMillis: Long,
    onRangeSelected: (fromMillis: Long, toMillis: Long) -> Unit,
) {
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DateButton(
            label = stringResource(Res.string.from_date),
            millis = fromMillis,
            modifier = Modifier.weight(1f),
            onClick = { showFromPicker = true },
        )
        DateButton(
            label = stringResource(Res.string.to_date),
            millis = toMillis,
            modifier = Modifier.weight(1f),
            onClick = { showToPicker = true },
        )
    }

    if (showFromPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = fromMillis)
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showFromPicker = false
                    val selected = pickerState.selectedDateMillis ?: fromMillis
                    onRangeSelected(selected, maxOf(toMillis, selected + 86_400_000L - 1))
                }) { Text(stringResource(Res.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { showFromPicker = false }) { Text(stringResource(Res.string.cancel)) } },
        ) { DatePicker(state = pickerState) }
    }

    if (showToPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = toMillis)
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showToPicker = false
                    val selected = pickerState.selectedDateMillis ?: toMillis
                    onRangeSelected(minOf(fromMillis, selected), selected + 86_400_000L - 1)
                }) { Text(stringResource(Res.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { showToPicker = false }) { Text(stringResource(Res.string.cancel)) } },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun DateButton(label: String, millis: Long, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(Fv.Surface, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, color = Fv.TextMid, fontSize = 10.sp)
        Spacer(Modifier.height(2.dp))
        Text(millis.toDateString(), color = Fv.TextHigh, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun <T> FilterChipRow(
    filters: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filters) { filter ->
            val active = filter == selected
            Box(
                modifier = Modifier
                    .clickable { onSelect(filter) }
                    .background(
                        if (active) Fv.Blue else Fv.Surface,
                        RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    label(filter),
                    color = if (active) Fv.TextHigh else Fv.TextMid,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
fun SummaryPill(label: String, value: String, accent: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Fv.Surface, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = Fv.TextMid, fontSize = 10.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

fun Long.toDateString(): String {
    val tz = TimeZone.currentSystemDefault()
    val dt = Instant.fromEpochMilliseconds(this).toLocalDateTime(tz)
    val d = dt.date
    return "${d.dayOfMonth.toString().padStart(2, '0')}/${d.monthNumber.toString().padStart(2, '0')}/${d.year}"
}

fun Long.toDateTimeString(): String {
    val tz = TimeZone.currentSystemDefault()
    val dt = Instant.fromEpochMilliseconds(this).toLocalDateTime(tz)
    val d = dt.date
    val h = dt.hour.toString().padStart(2, '0')
    val m = dt.minute.toString().padStart(2, '0')
    return "${d.dayOfMonth.toString().padStart(2, '0')}/${d.monthNumber.toString().padStart(2, '0')} $h:$m"
}
