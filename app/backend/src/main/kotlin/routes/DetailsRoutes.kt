package org.company.backend.routes

import dtos.DetailDTO
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.company.backend.inject
import org.company.backend.network.DetailApi

const val DETAILS_ROUTE = "/details"
const val HEALTH_ROUTE = "/health"

fun Route.getDetailsRoute() {
    val detailApi by inject<DetailApi>()
    
    get(DETAILS_ROUTE) {
        try {
            val response = detailApi.getDetails("")
            val detailDTO = response.results.firstOrNull()?.let { user ->
                DetailDTO(
                    name = "${user.name.first} ${user.name.last}",
                    email = user.email,
                    phone = user.phone
                )
            } ?: DetailDTO(
                name = "No data",
                email = "No data",
                phone = "No data"
            )
            
            call.respond(HttpStatusCode.OK, detailDTO)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}

fun Route.getHealthRoute() {
    get(HEALTH_ROUTE) {
        // TODO: Implement proper health checks:
        // - Test external API connectivity
        // - Check database connections
        // - Verify other dependencies
        call.respond(HttpStatusCode.OK, mapOf("status" to "healthy"))
    }
}