package com.jehadalomour.flowvan.core.network.api

import com.jehadalomour.flowvan.core.network.dto.AppVersionDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

/**
 * Reads this customer's update manifest — a static JSON file on the update host,
 * sitting next to the APK it describes.
 *
 * NOT ON THE API SERVER, AND NOT THROUGH [FlowVanApiClient]. Three reasons, and
 * each one is a way the obvious version of this fails:
 *
 *  - The update host is 7softwarejo.com for every customer; the API is on each
 *    customer's own box. Serving the manifest from the API would mean a customer
 *    whose server is down cannot be repaired by an update — the one moment an
 *    update matters most.
 *  - [FlowVanApiClient] attaches a bearer and unwraps the `{success, data}`
 *    envelope. A build old enough to need replacing is often one whose tokens the
 *    server has already stopped accepting, so authentication here would mean the
 *    app can only repair itself while it is already working.
 *  - It is a plain file. No backend code to deploy per customer — releasing is
 *    uploading two files.
 *
 * Its own lenient [Json] rather than the app's: this payload is written by hand
 * during a release, and a stray field in it must not be the reason a fleet stops
 * updating.
 */
class AppVersionApi(
    private val httpClient: HttpClient,
    private val manifestUrl: String,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun latest(): AppVersionDto {
        // Cache-busted. Cloudflare sits in front of the update host, and a cached
        // manifest is a fleet that keeps being told about the build it already has
        // — or worse, keeps being walled off by a floor that has since been lowered.
        val stamp = Clock.System.now().toEpochMilliseconds()
        val url = manifestUrl + (if ('?' in manifestUrl) "&" else "?") + "t=" + stamp
        return json.decodeFromString(httpClient.get(url).bodyAsText())
    }
}
