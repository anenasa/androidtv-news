package io.github.anenasa.news

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChannelConfig(
    val channelList: List<ChannelData>
)

@Serializable
data class ChannelData(
    val url: String,
    val name: String,
    @SerialName("ytdl-format") val format: String? = null,
    val volume: Float? = null,
    val header: String = "",
    @SerialName("ytdl-options") val ytdlOptions: Map<String, String> = emptyMap(),
    val list: String = "",
    val isWebView: Boolean = false,
)

@Serializable
data class CustomChannel(
    val customUrl: String,
    val customName: String,
    val customFormat: String,
    val customVolume: String,
    val customHeader: String = "",
    val isHidden: Boolean,
)
