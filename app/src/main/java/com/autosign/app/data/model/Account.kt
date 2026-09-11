package com.autosign.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Serializable
sealed class Account {
    abstract val id: String
    abstract val nickname: String
    abstract val lastSignResult: String
    abstract val lastSignTime: Long
    abstract val discordAvatarUrl: String

    @Serializable
    data class HoYoLab(
        override val id: String,
        override val nickname: String,
        val token: String,
        val genshin: Boolean = false,
        val honkaiStarRail: Boolean = false,
        val honkai3: Boolean = false,
        val tearsOfThemis: Boolean = false,
        val zenlessZoneZero: Boolean = false,
        override val lastSignResult: String = "",
        override val lastSignTime: Long = 0L,
        override val discordAvatarUrl: String = ""
    ) : Account()

    @Serializable
    data class SkPort(
        override val id: String,
        override val nickname: String,
        val accountToken: String,
        val language: String = "en",
        override val lastSignResult: String = "",
        override val lastSignTime: Long = 0L,
        override val discordAvatarUrl: String = ""
    ) : Account()
}

@Serializable
data class AppSettings(
    val scheduleEnabled: Boolean = false,
    val scheduleHour: Int = 10,
    val scheduleMinute: Int = 0,
    val androidNotifications: Boolean = true,
    val discordWebhook: String = "",
    val discordUserId: String = ""
)

enum class GameType(val displayName: String) {
    GENSHIN("Genshin Impact"),
    STAR_RAIL("Honkai: Star Rail"),
    HONKAI_3("Honkai Impact 3rd"),
    TEARS_OF_THEMIS("Tears of Themis"),
    ZENLESS_ZONE_ZERO("Zenless Zone Zero"),
    ENDFIELD("Arknights: Endfield")
}

