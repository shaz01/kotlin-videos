package org.company.backend.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

@Serializable
data class RandomUserResponse(
    val results: List<UserResult> = emptyList()
)

@Serializable
data class UserResult(
    val name: Name = Name(),
    val email: String = "",
    val phone: String = ""
)

@Serializable
data class Name(
    val title: String = "",
    val first: String = "",
    val last: String = ""
)

class DetailApi(private val httpClient: HttpClient) {
    suspend fun getDetails(name: String): RandomUserResponse {
        val details = httpClient.get("https://randomuser.me/api/").body<RandomUserResponse>()
        return details
    }
}