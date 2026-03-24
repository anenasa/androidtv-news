package io.github.anenasa.news

import com.chaquo.python.Python
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException

class Channel(
    val defaultUrl: String,
    val defaultName: String,
    val defaultFormat: String,
    val defaultVolume: Float,
    val defaultHeader: String,
    val ydlOptions: MutableMap<String, String>
) {
    var video: String = ""
    var customUrl: String = ""
    var customName: String = ""
    var customFormat: String = ""
    var customVolume: String = ""
    var customHeader: String = ""
    var isHidden: Boolean = false
    val headerMap: MutableMap<String, String>

    init {
        headerMap = header.split("\\r\\n")
            .filter { it.isNotEmpty() }
            .associate {
                it.substringBefore(":") to it.substringAfter(":").removePrefix(" ")
            }
            .toMutableMap()
    }

    var url: String
        get() {
            return customUrl.ifEmpty { defaultUrl }
        }
        set(url) {
            customUrl = url
        }

    var name: String
        get() {
            return customName.ifEmpty { defaultName }
        }
        set(name) {
            customName = name
        }

    var format: String
        get() {
            return customFormat.ifEmpty { defaultFormat }
        }
        set(format) {
            customFormat = format
        }

    val volume: Float
        get() {
            return if (customVolume.isEmpty()) {
                defaultVolume
            } else {
                customVolume.toFloat()
            }
        }

    var header: String
        get() {
            return customHeader.ifEmpty { defaultHeader }
        }
        set(header) {
            customHeader = header
            headerMap.clear()
            customHeader.ifEmpty { defaultHeader }
                .split("\\r\\n")
                .filter { it.isNotEmpty() }
                .associateTo(headerMap) {
                    it.substringBefore(":") to it.substringAfter(":").removePrefix(" ")
                }
        }

    fun setVolume(volume: String) {
        customVolume = volume
    }

    fun needExtract(): Int {
        val url = url
        if (video.isEmpty()) {
            return NEED_EXTRACT_YES
        }
        if (url.endsWith("m3u8")) {
            return NEED_EXTRACT_NO
        }
        if (url.startsWith("https://www.youtube.com/")) {
            val current = System.currentTimeMillis() / 1000
            val expire = video.substringAfter("expire/").substringBefore("/").toLong()
            return if (current < expire) {
                NEED_EXTRACT_NO
            } else {
                NEED_EXTRACT_YES
            }
        }
        if (url.startsWith("https://hamivideo.hinet.net/") || url.startsWith("https://embed.4gtv.tv/") ||
            url.startsWith("https://www.ftvnews.com.tw/live/live-video/1/")
        ) {
            val current = System.currentTimeMillis() / 1000
            val pos = video.indexOf("expires") + 8
            val expire = video.substring(pos, pos + 10).toLong()
            return if (current < expire) {
                NEED_EXTRACT_NO
            } else {
                NEED_EXTRACT_YES
            }
        }
        return NEED_EXTRACT_UNKNOWN
    }

    fun extract(ytDlp: YtDlp, okHttpClient: OkHttpClient) {
        var url = url
        if (url.startsWith("https://hamivideo.hinet.net/") && url.endsWith(".do")) {
            val id = url.substringAfterLast("/").substringBeforeLast(".")
            val okHttpRequestBuilder = Request.Builder()
                .url("https://hamivideo.hinet.net/api/play.do?freeProduct=1&id=$id")
            for (entry in headerMap.entries) {
                okHttpRequestBuilder.addHeader(entry.key, entry.value)
            }
            val okHttpRequest = okHttpRequestBuilder.build()
            okHttpClient.newCall(okHttpRequest).execute().use { response ->
                val jSONObject = JSONObject(response.body.string())
                url = jSONObject.getString("url")
            }
            headerMap.putIfAbsent("Referer", "https://hamivideo.hinet.net/")
        } else if (url.startsWith("https://embed.4gtv.tv/") || url.startsWith("https://www.ftvnews.com.tw/live/live-video/1/")) {
            var id: String
            if (url.startsWith("https://www.ftvnews.com.tw/live/live-video/1/")) {
                id = url.substringAfterLast("/")
            } else {
                val okHttpRequest = Request.Builder().url(url).build()
                okHttpClient.newCall(okHttpRequest).execute().use { response ->
                    val doc = Jsoup.parse(response.body.string())
                    val script = doc.selectFirst("script:containsData(ChannelId)")?.data() ?: throw IOException("找不到 script")
                    id = script.substringAfter("ChannelId: \"").substringBefore("\"")
                }
            }
            val okHttpRequest = Request.Builder()
                .url("https://app.4gtv.tv/Data/GetChannelURL_Mozai.ashx?callback=channelname&Type=LIVE&ChannelId=$id")
                .build()
            okHttpClient.newCall(okHttpRequest).execute().use { response ->
                val bodyString = response.body.string()
                url = bodyString.substringAfter("VideoURL\":\"").substringBefore("\"")
            }
        } else if (url.startsWith("https://www.ofiii.com/channel/watch/")) {
            // Get device id
            val deviceIdRequest = Request.Builder().url("https://www.ofiii.com/api/deviceId").build()
            val deviceId: String
            okHttpClient.newCall(deviceIdRequest).execute().use { response ->
                deviceId = response.body.string().removeSurrounding("\"")
            }
            val id = url.substringAfterLast("/")
            val okHttpRequestBuilder = Request.Builder()
                .url("https://cdi.ofiii.com/ofiii_cdi/video/urls?device_type=pc&device_id=$deviceId&media_type=channel&asset_id=$id&project_num=OFWEB00")
            for (entry in headerMap.entries) {
                okHttpRequestBuilder.addHeader(entry.key, entry.value)
            }
            val okHttpRequest = okHttpRequestBuilder.build()
            okHttpClient.newCall(okHttpRequest).execute().use { response ->
                val jSONObject = JSONObject(response.body.string())
                url = jSONObject.getJSONArray("asset_urls").getString(0)
            }
        }

        val option = Python.getInstance().builtins.callAttr("dict")
        option.callAttr("__setitem__", "format", format)
        if (!headerMap.isEmpty()) {
            val headerDict = Python.getInstance().builtins.callAttr("dict")
            for (entry in headerMap.entries) {
                headerDict.callAttr("__setitem__", entry.key, entry.value)
            }
            option.callAttr("__setitem__", "http_headers", headerDict)
        }
        for (entry in ydlOptions.entries) {
            option.callAttr("__setitem__", entry.key, entry.value)
        }
        video = ytDlp.extract(url, option)
    }

    companion object {
        const val NEED_EXTRACT_NO: Int = 0
        const val NEED_EXTRACT_YES: Int = 1
        const val NEED_EXTRACT_UNKNOWN: Int = 2
    }
}
