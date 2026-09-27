package com.flyonz.ceritaria.studio.core.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdminMembershipDto(
    @SerialName("user_id") val userId: String,
)
