package com.jehadalomour.flowvan.core.network.api

import com.jehadalomour.flowvan.core.network.dto.CreateProspectSearchDto
import com.jehadalomour.flowvan.core.network.dto.ProspectCategoriesDto
import com.jehadalomour.flowvan.core.network.dto.ProspectDto
import com.jehadalomour.flowvan.core.network.dto.ProspectSearchDto
import com.jehadalomour.flowvan.core.network.http.FlowVanApiClient
import com.jehadalomour.flowvan.core.network.http.getData
import com.jehadalomour.flowvan.core.network.http.postData

/**
 * Lead finding for the salesman. Backs the "find customers near me" screen and
 * reuses the same /prospecting endpoints the dashboard drives — the server does
 * the Google Places call, phone lookup and de-dup; the app only asks and shows.
 *
 * The search endpoint accepts canFindCustomers, so a SALES account can call it;
 * a rep without the flag is refused server-side (403), not just hidden.
 */
class ProspectingApi(private val client: FlowVanApiClient) {

    /** The category allow-list; `featured` is the short one-tap subset. */
    suspend fun categories(): ProspectCategoriesDto = client.getData("prospecting/categories")

    /**
     * Run a search and return its prospects. The response nests the search and
     * its rows; the ViewModel reads `prospects` back with [prospectsFor].
     */
    suspend fun search(body: CreateProspectSearchDto): SearchResult =
        client.postData("prospecting/searches", body)

    /** Prospects belonging to one search, nearest-first ordering left to the caller. */
    suspend fun prospectsFor(searchId: String): List<ProspectDto> =
        client.getData("prospecting/prospects?searchId=$searchId&limit=100")
}

/** The POST /prospecting/searches envelope: the run, plus the leads it found. */
@kotlinx.serialization.Serializable
data class SearchResult(
    val search: ProspectSearchDto,
    val prospects: List<ProspectDto> = emptyList(),
)
