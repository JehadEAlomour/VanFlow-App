package com.jehadalomour.flowvan.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.designsystem.components.Fv
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.stringResource
import com.jehadalomour.flowvan.core.network.dto.MyRouteStopDto
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TodayRouteScreen(
    onBack: () -> Unit,
    onOpenCustomer: (String) -> Unit,
    onNavigateTo: (String) -> Unit = {},
    viewModel: TodayRouteViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Fv.BgDeepest)) {
        // ── Top bar ──────────────────────────────────────────────────────────
        Surface(modifier = Modifier.fillMaxWidth(), color = Fv.Surface, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Fv.SurfaceTop)
                        .border(0.5.dp, Fv.Border, RoundedCornerShape(11.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_back),
                        contentDescription = null,
                        tint = Fv.TextHigh,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    stringResource(Res.string.route_today_title),
                    color = Fv.TextHigh,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .background(Fv.Blue)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        "${state.doneCount} / ${state.total}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Fv.Border))

        // ── Body ─────────────────────────────────────────────────────────────
        when {
            state.isLoading -> CenterBox { CircularProgressIndicator(color = Fv.Blue) }
            state.error != null -> CenterBox {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error ?: "", color = Fv.Red, fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Fv.Blue)
                            .clickable(onClick = viewModel::load)
                            .padding(horizontal = 18.dp, vertical = 9.dp),
                    ) {
                        Text(stringResource(Res.string.common_retry), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            state.stops.isEmpty() -> CenterBox {
                Text(stringResource(Res.string.route_no_visits_today), color = Fv.TextMid, fontSize = 14.sp)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                items(state.stops, key = { it.id }) { stop ->
                    TodayStopCard(
                        index = state.stops.indexOf(stop) + 1,
                        stop = stop,
                        marking = state.markingId == stop.customerId,
                        onClick = { onOpenCustomer(stop.customerId) },
                        onNavigate = if (stop.lat != null && stop.lng != null) {
                            { onNavigateTo(stop.customerId) }
                        } else null,
                        onMarkDone = { viewModel.markTodoDone(stop.customerId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun TodayStopCard(
    index: Int,
    stop: MyRouteStopDto,
    marking: Boolean,
    onClick: () -> Unit,
    onNavigate: (() -> Unit)?,
    onMarkDone: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Fv.Surface)
            .border(0.5.dp, Fv.Border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Column {
            // Header row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Fv.SurfaceTop)
                        .border(0.5.dp, Fv.Border, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$index", color = Fv.TextMid, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stop.customerName,
                        color = Fv.TextHigh,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull(stop.city, stop.customerNumber).joinToString(" · "),
                        color = Fv.TextLow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                if (onNavigate != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Fv.Blue)
                            .clickable(onClick = onNavigate),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(Res.drawable.ic_map),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }

            // Admin note
            if (!stop.note.isNullOrBlank()) {
                Spacer(Modifier.height(9.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Fv.SurfaceTop)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Text(stringResource(Res.string.route_note_prefix, stop.note ?: ""), color = Fv.TextMid, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            // To-do
            val todoText = stop.todo
            if (!todoText.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_check_circle),
                        contentDescription = null,
                        tint = if (stop.todoDoneToday) Fv.Green else Fv.Amber,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        todoText,
                        color = Fv.TextHigh,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    if (stop.todoDoneToday) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(Fv.Green.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(stringResource(Res.string.route_done_check), color = Fv.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(Fv.Green)
                                .clickable(enabled = !marking, onClick = onMarkDone)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (marking) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(14.dp),
                                )
                            } else {
                                Text(stringResource(Res.string.route_complete), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
