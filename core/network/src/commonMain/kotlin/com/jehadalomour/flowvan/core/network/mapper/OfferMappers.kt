package com.jehadalomour.flowvan.core.network.mapper

import com.jehadalomour.flowvan.core.model.AppliedOffer
import com.jehadalomour.flowvan.core.model.FreeLine
import com.jehadalomour.flowvan.core.model.LineOffer
import com.jehadalomour.flowvan.core.model.OfferChoice
import com.jehadalomour.flowvan.core.model.OfferEvaluation
import com.jehadalomour.flowvan.core.model.OfferLineAdj
import com.jehadalomour.flowvan.core.model.OfferTotals
import com.jehadalomour.flowvan.core.model.ServerLine
import com.jehadalomour.flowvan.core.network.dto.EvaluationResultDto
import com.jehadalomour.flowvan.core.network.http.filsToJod

/**
 * Map the evaluation DTO (integer fils) into the domain model (JOD doubles). The
 * `pendingChoices` list is derived from `appliedOffers[].freeItemChoice` — a
 * FREE_ITEM_CHOICE offer surfaces there until the rep picks an item.
 */
fun EvaluationResultDto.toOfferEvaluation(): OfferEvaluation = OfferEvaluation(
    adjustedLines = lines
        .filter { it.lineDiscountFils > 0 }
        .map { OfferLineAdj(itemNumber = it.itemNumber, discountJod = it.lineDiscountFils.filsToJod()) },
    freeLines = freeLines.map {
        FreeLine(
            itemNumber = it.itemNumber,
            qty = it.qty,
            unitPriceJod = it.unitPriceFils.filsToJod(),
            offerId = it.offerId,
        )
    },
    invoiceDiscountJod = invoiceDiscountFils.filsToJod(),
    appliedOffers = appliedOffers.map {
        AppliedOffer(
            offerId = it.offerId,
            name = it.name,
            summary = it.summary,
            type = it.type,
        )
    },
    pendingChoices = appliedOffers.mapNotNull { ao ->
        ao.freeItemChoice?.takeIf { it.choices.isNotEmpty() }?.let { c ->
            OfferChoice(offerId = ao.offerId, choices = c.choices, qty = c.qty)
        }
    },
    serverLines = lines.map {
        ServerLine(
            itemNumber = it.itemNumber,
            qty = it.qty,
            unitPriceJod = it.unitPriceFils.filsToJod(),
            lineDiscountJod = it.lineDiscountFils.filsToJod(),
            lineNetJod = it.lineNetFils.filsToJod(),
            offers = it.offers.map { o ->
                LineOffer(
                    offerId = o.offerId,
                    name = o.name,
                    pct = o.pct,
                    discountJod = o.discountFils.filsToJod(),
                )
            },
        )
    },
    totals = OfferTotals(
        subtotalJod = totals.subtotalFils.filsToJod(),
        lineDiscountJod = totals.lineDiscountFils.filsToJod(),
        invoiceDiscountJod = totals.invoiceDiscountFils.filsToJod(),
        totalDiscountJod = totals.totalDiscountFils.filsToJod(),
        taxJod = totals.taxFils.filsToJod(),
        grandTotalJod = totals.grandTotalFils.filsToJod(),
    ),
)
