package com.jehadalomour.flowvan.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jehadalomour.flowvan.core.data.repository.OfferRepository
import com.jehadalomour.flowvan.core.designsystem.components.Fv
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.ic_back
import com.jehadalomour.flowvan.core.designsystem.resources.offer_type_item
import com.jehadalomour.flowvan.core.designsystem.resources.offer_type_payment
import com.jehadalomour.flowvan.core.designsystem.resources.offers_all_days
import com.jehadalomour.flowvan.core.designsystem.resources.offers_empty
import com.jehadalomour.flowvan.core.designsystem.resources.offers_priority
import com.jehadalomour.flowvan.core.designsystem.resources.offers_reward_discount
import com.jehadalomour.flowvan.core.designsystem.resources.offers_reward_gift
import com.jehadalomour.flowvan.core.designsystem.resources.offers_title
import com.jehadalomour.flowvan.core.model.OfferDefinition
import com.jehadalomour.flowvan.core.model.OfferReward
import com.jehadalomour.flowvan.core.model.OfferType
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Read-only list of the rep's ACTIVE offers, cached locally by [OfferRepository]. Opened from
 * the Home action grid. Refreshes best-effort on entry, then observes the local cache so it
 * works offline. No evaluation here — just a human-readable view of what's running.
 */
@Composable
fun OffersScreen(
    onBack: () -> Unit,
    repository: OfferRepository = koinInject(),
) {
    val offers by repository.observeActive().collectAsState(initial = emptyList())

    Surface(modifier = Modifier.fillMaxSize(), color = Fv.BgDeepest) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_back),
                    contentDescription = null,
                    tint = Fv.TextHigh,
                    modifier = Modifier.size(36.dp).clickable(onClick = onBack).padding(8.dp),
                )
                Text(
                    stringResource(Res.string.offers_title),
                    color = Fv.TextHigh,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            if (offers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(Res.string.offers_empty),
                        color = Fv.TextMid,
                        fontSize = 13.sp,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(offers, key = { it.id }) { offer -> OfferCard(offer) }
                }
            }
        }
    }
}

@Composable
private fun OfferCard(offer: OfferDefinition) {
    val typeLabel = when (offer.type) {
        OfferType.PAYMENT_METHOD_DISCOUNT -> stringResource(Res.string.offer_type_payment)
        OfferType.ITEM_QTY_REWARD -> stringResource(Res.string.offer_type_item)
    }
    val accent = when (offer.type) {
        OfferType.PAYMENT_METHOD_DISCOUNT -> Fv.Teal
        OfferType.ITEM_QTY_REWARD -> Fv.Purple
    }
    val discountLabel = stringResource(Res.string.offers_reward_discount)
    val giftLabel = stringResource(Res.string.offers_reward_gift)
    val rewardText = when (val r = offer.reward) {
        is OfferReward.LinePercent -> "$discountLabel ${r.basePercent.trimPercent()}%"
        is OfferReward.ItemPercent -> "$discountLabel ${r.basePercent.trimPercent()}%"
        is OfferReward.LineAmount -> "$discountLabel ${(r.baseAmountFils / 1000.0).trimAmount()}"
        is OfferReward.Gift -> giftLabel
        else -> typeLabel
    }
    val scheduleText = if (offer.daysOfWeek == null) stringResource(Res.string.offers_all_days) else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Fv.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(accent.copy(alpha = 0.14f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(rewardText.take(1), color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(offer.name, color = Fv.TextHigh, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(2.dp))
                Text("$typeLabel · $rewardText", color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (scheduleText != null) {
                    Spacer(Modifier.size(2.dp))
                    Text(scheduleText, color = Fv.TextMid, fontSize = 11.sp)
                }
            }
        }
    }
}

/** "10.0" → "10", "12.5" → "12.5" — drop a trailing .0 for percent display. */
private fun Double.trimPercent(): String {
    val i = toInt()
    return if (this == i.toDouble()) i.toString() else toString()
}

private fun Double.trimAmount(): String {
    val i = toInt()
    return if (this == i.toDouble()) i.toString() else ((this * 1000).toInt() / 1000.0).toString()
}
