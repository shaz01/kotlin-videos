package org.company.backend

import org.company.backend.routes.getDetailsRoute
import org.company.backend.routes.getHealthRoute
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        getDetailsRoute()
        getHealthRoute()
    }
}
