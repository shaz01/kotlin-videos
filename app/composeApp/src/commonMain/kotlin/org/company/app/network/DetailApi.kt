package org.company.app.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class DetailApi(private val httpClient: HttpClient) {
    suspend fun getDetails(name: String): String {
        val details = httpClient.get("details?name=$name").body<String>()
        return details
    }
}