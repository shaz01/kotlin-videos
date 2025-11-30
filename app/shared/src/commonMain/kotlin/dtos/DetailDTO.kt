package dtos

import kotlinx.serialization.Serializable

@Serializable
data class DetailDTO(
    val name: String,
    val email: String,
    val phone: String,
)
