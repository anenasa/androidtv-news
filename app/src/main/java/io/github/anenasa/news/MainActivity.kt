package io.github.anenasa.news

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import io.github.anenasa.news.YtDlp.Companion.create
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import kotlin.system.exitProcess
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class MainActivity : AppCompatActivity() {

    var channelNum: Int = 0
    var channel: ArrayList<Channel> = ArrayList()
    var channelLengthOfConfig: Int = 0
    var input: String = ""
    var defaultFormat: String = ""
    var defaultVolume: String = ""
    var isShowErrorMessage: Boolean = false
    var enableBackgroundExtract: Boolean = false
    var invertChannelButtons: Boolean = false
    var hideNavigationBar: Boolean = false
    var hideStatusBar: Boolean = false
    var useExternalJS: Boolean = false
    var updateYtDlpOnStart: Boolean = false
    var channelListLoaded: Boolean = false

    var ytDlp: YtDlp? = null
    val player: ExoPlayer by lazy { ExoPlayer.Builder(this).build() }
    val playerView: SurfaceView by lazy { findViewById(R.id.playerView) }
    val textView: TextView by lazy { findViewById(R.id.textView) }
    val textInfo: TextView by lazy { findViewById(R.id.textInfo) }
    val errorMessageView: TextView by lazy { findViewById(R.id.errorMessage) }
    var backgroundExtractJob: Job? = null
    var readChannelListJob: Job? = null
    val okHttpClient: OkHttpClient = MyApplication.okHttpClient

    val preferences: SharedPreferences by lazy { getSharedPreferences("io.github.anenasa.news", MODE_PRIVATE) }

    var isStarted: Boolean = true
    val audioManager: AudioManager by lazy { applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager }
    var errorCount: Int = 0
    var doNotPlayOnStart: Boolean = false

    private val showChannelListLauncher = registerForActivityResult(StartActivityForResult()) { result: ActivityResult? ->
        val data = result?.data
        if (result?.resultCode == RESULT_OK && data != null) {
            switchChannel(data.getIntExtra("channelNum", 0))
        } else {
            play(channelNum)
        }
    }

    private val showChannelInfoLauncher = registerForActivityResult(StartActivityForResult()) { result: ActivityResult? ->
        val data = result?.data
        if (result?.resultCode == RESULT_OK && data != null) {
            if (data.getBooleanExtra("delete", false)) {
                channel.removeAt(channelNum)
                resetChannelNum()
                play(channelNum)
                saveSettings()
                return@registerForActivityResult
            }

            if (data.getBooleanExtra("remove_cache", false)) {
                channel[channelNum].clearVideo()
            }

            // If url or format changes, clear video to extract again
            val urlOld = channel[channelNum].url
            var urlNew = data.getStringExtra("customUrl")?.takeIf { it.isNotEmpty() }
                ?: channel[channelNum].defaultUrl
            val formatOld = channel[channelNum].format
            var formatNew = data.getStringExtra("customFormat")?.takeIf { it.isNotEmpty() }
                ?: channel[channelNum].defaultFormat
            if (urlOld != urlNew || formatOld != formatNew) {
                channel[channelNum].clearVideo()
            }

            channel[channelNum].name = data.getStringExtra("customName").orEmpty()
            channel[channelNum].isHidden = data.getBooleanExtra("isHidden", false)
            channel[channelNum].url = data.getStringExtra("customUrl").orEmpty()
            channel[channelNum].format = data.getStringExtra("customFormat").orEmpty()
            channel[channelNum].setVolume(data.getStringExtra("customVolume").orEmpty())
            channel[channelNum].header = data.getStringExtra("customHeader").orEmpty()
            play(channelNum)
            saveSettings()
        } else {
            play(channelNum)
        }
    }

    private val addNewChannelLauncher = registerForActivityResult(StartActivityForResult()) { result: ActivityResult? ->
        val data = result?.data
        if (result?.resultCode == RESULT_OK && data != null && !data.getBooleanExtra("delete", false)) {
            val ch = Channel(
                "",
                "",
                defaultFormat,
                defaultVolume.toFloat(),
                "",
                HashMap()
            ).apply {
                name = data.getStringExtra("customName").orEmpty()
                isHidden = data.getBooleanExtra("isHidden", false)
                url = data.getStringExtra("customUrl").orEmpty()
                format = data.getStringExtra("customFormat").orEmpty()
                setVolume(data.getStringExtra("customVolume").orEmpty())
                header = data.getStringExtra("customHeader").orEmpty()
            }
            channel.add(ch)
            saveSettings()
            switchChannel(channel.size - 1)
        } else {
            play(channelNum)
        }
    }

    private val showSettingsLauncher = registerForActivityResult(StartActivityForResult()) { result: ActivityResult? ->
        val data = result?.data
        if (result?.resultCode == RESULT_OK && data != null) {
            defaultFormat = data.getStringExtra("defaultFormat").orEmpty()
            defaultVolume = data.getStringExtra("defaultVolume").orEmpty()
            isShowErrorMessage = data.getBooleanExtra("isShowErrorMessage", false)
            enableBackgroundExtract = data.getBooleanExtra("enableBackgroundExtract", false)
            invertChannelButtons = data.getBooleanExtra("invertChannelButtons", false)
            hideNavigationBar = data.getBooleanExtra("hideNavigationBar", false)
            hideStatusBar = data.getBooleanExtra("hideStatusBar", false)
            useExternalJS = data.getBooleanExtra("useExternalJS", false)
            updateYtDlpOnStart = data.getBooleanExtra("updateYtDlpOnStart", false)
            if (data.getBooleanExtra("ytDlpUpdated", false)) {
                // Kill process, so new version of yt-dlp can be loaded
                exitProcess(0)
            }
            MyApplication.cookieJar.loadFile(File(getExternalFilesDir(null), "cookies.txt"))
            saveSettings()

            ytDlp!!.useExternalJS = useExternalJS

            backgroundExtractJob?.cancel()
            readChannelListJob = lifecycleScope.launch(Dispatchers.IO) {
                while (true) {
                    readChannelList()
                    delay(5000)
                }
            }
            if (data.getBooleanExtra("remove_cache", false)) {
                for (ch in channel) ch.clearVideo()
            }
        } else {
            play(channelNum)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        channelNum = preferences.getInt("channelNum", 0)
        defaultFormat = preferences.getString("defaultFormat", "bv*+ba/b")!!
        defaultVolume = preferences.getString("defaultVolume", "1.0")!!
        isShowErrorMessage = preferences.getBoolean("isShowErrorMessage", false)
        enableBackgroundExtract = preferences.getBoolean("enableBackgroundExtract", false)
        invertChannelButtons = preferences.getBoolean("invertChannelButtons", false)
        hideNavigationBar = preferences.getBoolean("hideNavigationBar", false)
        hideStatusBar = preferences.getBoolean("hideStatusBar", false)
        useExternalJS = preferences.getBoolean("useExternalJS", false)
        updateYtDlpOnStart = preferences.getBoolean("updateYtDlpOnStart", false)

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Player error", error)
                showErrorMessage(error.message)
                if (channel.getOrNull(channelNum)?.needExtract() != Channel.NEED_EXTRACT_NO) {
                    if (errorCount > 0) {
                        // Force extract by removing video url
                        channel.getOrNull(channelNum)?.clearVideo()
                        errorCount = 0
                    } else errorCount++
                }
                play(channelNum)
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    errorMessageView.text = ""
                    textInfo.text = ""
                    errorCount = 0
                } else if (state == Player.STATE_ENDED) {
                    channel[channelNum].clearVideo()
                    play(channelNum)
                }
            }
        })
        player.setVideoSurfaceView(playerView)
        MyApplication.cookieJar.loadFile(File(getExternalFilesDir(null), "cookies.txt"))
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch(Dispatchers.IO) { initializeYtDlp() }
        isStarted = true
        // Do not call play() more than once
        // play() will be called in onActivityResult()
        if (doNotPlayOnStart) {
            doNotPlayOnStart = false
            return
        }
        if (channelListLoaded) {
            play(channelNum)
        }
    }

    override fun onResume() {
        super.onResume()
        var systemUiFlag = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        if (hideStatusBar) {
            systemUiFlag += View.SYSTEM_UI_FLAG_FULLSCREEN
        }
        if (hideNavigationBar) {
            systemUiFlag += View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
        if (systemUiFlag != View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) {
            window.decorView.systemUiVisibility = systemUiFlag
        }
    }

    suspend fun initializeYtDlp() {
        if (ytDlp != null) {
            // Already initialized
            return
        }
        try {
            ytDlp = create(this, updateYtDlpOnStart, useExternalJS)
            readChannelListJob = lifecycleScope.launch(Dispatchers.IO) {
                while (true) {
                    readChannelList()
                    delay(5000)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, e.toString(), e)
            withContext(Dispatchers.Main) {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("yt-dlp 載入失敗")
                    .setMessage(e.message)
                    .setCancelable(false)
                    .setNegativeButton("確定") { _, _ -> finish() }
                    .show()
            }
        }
    }

    suspend fun readChannelList() {
        if (!isStarted) {
            return
        }
        channelListLoaded = false
        channel = ArrayList()
        try {
            val configFile = File(getExternalFilesDir(null), "config.txt")
            if (configFile.exists()) {
                configFile.bufferedReader().use { reader ->
                    readChannelListFromString(reader.readText())
                }
            } else {
                val request = Request.Builder()
                    .url("https://anenasa.gitlab.io/channel/config.txt")
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("下載失敗：$response")
                    }
                    readChannelListFromString(response.body.string())
                }
            }
            channelLengthOfConfig = channel.size

            var customChannelList: JSONObject? = null
            var newChannelArray: JSONArray? = null
            val customFile = File(getExternalFilesDir(null), "custom.txt")
            if (customFile.exists()) {
                customFile.bufferedReader().use { reader ->
                    val customString = reader.readText()
                    val customJsonObject = JSONObject(customString)
                    customChannelList = customJsonObject.getJSONObject("customChannelList")
                    newChannelArray = customJsonObject.getJSONArray("newChannelArray")
                }
            }

            // Read custom settings for this channel
            for (ch in channel) {
                if (customChannelList?.has(ch.name) == true) {
                    val customChannelObject = customChannelList.getJSONObject(ch.name)
                    ch.url = customChannelObject.getString("customUrl")
                    ch.name = customChannelObject.getString("customName")
                    ch.format = customChannelObject.getString("customFormat")
                    ch.setVolume(customChannelObject.getString("customVolume"))
                    ch.header = customChannelObject.getString("customHeader")
                    ch.isHidden = customChannelObject.getBoolean("isHidden")
                }
            }

            //Read custom channel set in app
            if (newChannelArray != null) {
                for (i in 0..<newChannelArray.length()) {
                    val newChannelObject = newChannelArray.getJSONObject(i)
                    val ch = Channel(
                        "",
                        "",
                        defaultFormat,
                        defaultVolume.toFloat(),
                        "",
                        HashMap()
                    ).apply {
                        url = newChannelObject.getString("customUrl")
                        name = newChannelObject.getString("customName")
                        format = newChannelObject.getString("customFormat")
                        setVolume(newChannelObject.getString("customVolume"))
                        header = newChannelObject.getString("customHeader")
                        isHidden = newChannelObject.getBoolean("isHidden")
                    }
                    channel.add(ch)
                }
            }
            channelListLoaded = true
            withContext(Dispatchers.Main) { errorMessageView.text = "" }

            if (channelNum >= channel.size) {
                resetChannelNum()
            }
            withContext(Dispatchers.Main) { play(channelNum) }

            if (enableBackgroundExtract) {
                backgroundExtractJob = lifecycleScope.launch(Dispatchers.IO) {
                    while (true) {
                        // Prevent NPE
                        val channelInThread = channel
                        for (i in channelInThread.indices) {
                            try {
                                if (!channelInThread[i].isHidden) {
                                    channelInThread[i].extract(ytDlp!!, okHttpClient)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Background extract error", e)
                            }
                            yield()
                        }
                        delay(3600000)
                    }
                }
            }
            readChannelListJob?.cancel()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                errorMessageView.text = String.format(
                    "頻道清單讀取失敗，按 OK 或螢幕進入設定\n%s",
                    e.message
                )
            }
            Log.e(TAG, e.toString(), e)
            channel = ArrayList()
        }
    }

    fun readChannelListFromString(content: String) {
        val json = JSONObject(content)
        val channelList = json.getJSONArray("channelList")

        for (i in 0..<channelList.length()) {
            val channelObject = channelList.getJSONObject(i)
            if (channelObject.has("list")) {
                val request = Request.Builder()
                    .url(channelObject.getString("list"))
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("下載失敗：$response")
                    }
                    readChannelListFromString(response.body.string())
                }
                continue
            }
            val url = channelObject.getString("url")
            val name = channelObject.getString("name")
            val format: String = channelObject.optString("ytdl-format", defaultFormat)
            val volume: Float = channelObject.optDouble("volume", defaultVolume.toDouble()).toFloat()
            val header: String = channelObject.optString("header", "")
            val ytdlOptions: MutableMap<String, String> = HashMap()
            channelObject.optJSONObject("ytdl-options")?.let {
                val keys = it.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = it.getString(key)
                    ytdlOptions[key] = value
                }
            }
            val ch = Channel(url, name, format, volume, header, ytdlOptions)
            channel.add(ch)
        }
    }

    fun toPrevChannel() {
        do {
            if (channelNum == 0) {
                channelNum = channel.size - 1
            } else {
                channelNum -= 1
            }
        } while (channel[channelNum].isHidden)
        switchChannel(channelNum)
    }

    fun toNextChannel() {
        do {
            if (channelNum == channel.size - 1) {
                channelNum = 0
            } else {
                channelNum += 1
            }
        } while (channel[channelNum].isHidden)
        switchChannel(channelNum)
    }

    fun switchChannel(num: Int) {
        player.stop()
        errorMessageView.text = ""
        channelNum = num
        play(num)
    }

    @OptIn(markerClass = [UnstableApi::class])
    fun play(num: Int) {
        textInfo.text = String.format(
            Locale.ROOT,
            "正在載入 %d %s",
            num,
            channel[num].name
        )
        lifecycleScope.launch(Dispatchers.IO) {
            if (channel[num].needExtract() == Channel.NEED_EXTRACT_YES) {
                try {
                    channel[num].extract(ytDlp!!, okHttpClient)
                } catch (e: Exception) {
                    if (channelNum != num) return@launch
                    Log.e(TAG, "Channel.parse error", e)
                    withContext(Dispatchers.Main) { showErrorMessage(e.message) }
                }
            }
            if (channelNum != num) return@launch

            val factory: DataSource.Factory = DefaultHttpDataSource.Factory()
                .setDefaultRequestProperties(channel[num].headerMap)
            val url = channel[num].video
            val split = url.indexOf('\n')
            val mediaSource: MediaSource = if (split == -1) {
                val mediaItem = MediaItem.fromUri(url)
                DefaultMediaSourceFactory(factory)
                    .createMediaSource(mediaItem)
            } else {
                val firstUrl = url.substring(0, split)
                val firstMediaItem = MediaItem.fromUri(firstUrl)
                val firstMediaSource = DefaultMediaSourceFactory(factory)
                    .createMediaSource(firstMediaItem)
                val secondUrl = url.substring(split + 1)
                val secondMediaItem = MediaItem.fromUri(secondUrl)
                val secondMediaSource = DefaultMediaSourceFactory(factory)
                    .createMediaSource(secondMediaItem)
                MergingMediaSource(firstMediaSource, secondMediaSource)
            }

            // player needs to run on main thread
            withContext(Dispatchers.Main) {
                if (num != channelNum) {
                    // Already switched to another channel, do not play this
                    return@withContext
                }
                player.setMediaSource(mediaSource)
                player.prepare()
                player.volume = channel[num].volume
                if (isStarted) {
                    player.play()
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && supportFragmentManager.fragments
                .isEmpty()
        ) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    if (!channelListLoaded) {
                        readChannelListJob?.let {
                            it.cancel()
                            showSettings(findViewById(R.id.container))
                        }
                    } else if (input.isEmpty()) {
                        showMenu()
                    } else if (input.toInt() < channel.size) {
                        switchChannel(input.toInt())
                        clearInput()
                    } else {
                        clearInput()
                    }
                    return true
                }

                KeyEvent.KEYCODE_BACK -> {
                    if (!input.isEmpty()) {
                        clearInput()
                    } else {
                        AlertDialog.Builder(this)
                            .setTitle("退出")
                            .setMessage("是否要退出新聞直播？")
                            .setPositiveButton("確定") { _, _ -> finish() }
                            .setNegativeButton("取消", null)
                            .show()
                    }
                    return true
                }

                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                    if (!channelListLoaded) return true
                    if (invertChannelButtons) {
                        toNextChannel()
                    } else {
                        toPrevChannel()
                    }
                    return true
                }

                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP -> {
                    if (!channelListLoaded) return true
                    if (invertChannelButtons) {
                        toPrevChannel()
                    } else {
                        toNextChannel()
                    }
                    return true
                }

                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    audioManager.adjustVolume(
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_PLAY_SOUND
                    )
                    return true
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    audioManager.adjustVolume(
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_PLAY_SOUND
                    )
                    return true
                }

                KeyEvent.KEYCODE_INFO -> {
                    if (!channelListLoaded) return true
                    showChannelInfo(findViewById(R.id.container))
                    return true
                }

                KeyEvent.KEYCODE_MENU -> {
                    if (!channelListLoaded) return true
                    showSettings(findViewById(R.id.container))
                    return true
                }

                KeyEvent.KEYCODE_0 -> {
                    appendInput(0)
                    return true
                }

                KeyEvent.KEYCODE_1 -> {
                    appendInput(1)
                    return true
                }

                KeyEvent.KEYCODE_2 -> {
                    appendInput(2)
                    return true
                }

                KeyEvent.KEYCODE_3 -> {
                    appendInput(3)
                    return true
                }

                KeyEvent.KEYCODE_4 -> {
                    appendInput(4)
                    return true
                }

                KeyEvent.KEYCODE_5 -> {
                    appendInput(5)
                    return true
                }

                KeyEvent.KEYCODE_6 -> {
                    appendInput(6)
                    return true
                }

                KeyEvent.KEYCODE_7 -> {
                    appendInput(7)
                    return true
                }

                KeyEvent.KEYCODE_8 -> {
                    appendInput(8)
                    return true
                }

                KeyEvent.KEYCODE_9 -> {
                    appendInput(9)
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) {
            return super.onTouchEvent(event)
        }
        supportFragmentManager.popBackStack()
        if (!channelListLoaded) {
            readChannelListJob?.let {
                it.cancel()
                showSettings(findViewById(R.id.container))
            }
        } else if (event.x.toInt() < playerView.width / 3) {
            if (invertChannelButtons) {
                toNextChannel()
            } else {
                toPrevChannel()
            }
        } else if (event.x.toInt() < playerView.width / 3 * 2) {
            showMenu()
        } else {
            if (invertChannelButtons) {
                toPrevChannel()
            } else {
                toNextChannel()
            }
        }
        return true
    }

    fun appendInput(num: Int) {
        input += num
        textView.text = input
    }

    fun clearInput() {
        input = ""
        textView.text = ""
    }

    fun showMenu() {
        val fragment = MenuFragment()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(fragment.javaClass.name)
            .commit()
    }

    fun showChannelList(view: View) {
        val nameArray = arrayOfNulls<String>(channel.size)
        val isHiddenArray = BooleanArray(channel.size)
        for (i in channel.indices) {
            nameArray[i] = channel[i].name
            isHiddenArray[i] = channel[i].isHidden
        }
        val intent = Intent(this, ChannelListActivity::class.java).apply {
            putExtra("nameArray", nameArray)
            putExtra("isHiddenArray", isHiddenArray)
            putExtra("currentNum", channelNum)
        }
        showChannelListLauncher.launch(intent)
        supportFragmentManager.popBackStack()
        doNotPlayOnStart = true
    }

    fun showChannelInfo(view: View) {
        val intent = Intent(this, ChannelInfoActivity::class.java).apply {
            putExtra("isNewChannel", channelNum >= channelLengthOfConfig)
            putExtra("index", channelNum)
            putExtra("defaultUrl", channel[channelNum].defaultUrl)
            putExtra("defaultName", channel[channelNum].defaultName)
            putExtra("defaultFormat", channel[channelNum].defaultFormat)
            putExtra("defaultVolume", channel[channelNum].defaultVolume)
            putExtra("defaultHeader", channel[channelNum].defaultHeader)
            putExtra("customUrl", channel[channelNum].customUrl)
            putExtra("customName", channel[channelNum].customName)
            putExtra("customFormat", channel[channelNum].customFormat)
            putExtra("customVolume", channel[channelNum].customVolume)
            putExtra("customHeader", channel[channelNum].customHeader)
            putExtra("isHidden", channel[channelNum].isHidden)
            putExtra("width", player.videoSize.width)
            putExtra("height", player.videoSize.height)
        }
        showChannelInfoLauncher.launch(intent)
        supportFragmentManager.popBackStack()
        doNotPlayOnStart = true
    }

    fun addNewChannel(view: View) {
        val intent = Intent(this, ChannelInfoActivity::class.java).apply {
            putExtra("isNewChannel", true)
            putExtra("index", channel.size)
            putExtra("defaultUrl", "")
            putExtra("defaultName", "")
            putExtra("defaultFormat", defaultFormat)
            putExtra("defaultVolume", defaultVolume.toFloat())
            putExtra("defaultHeader", "")
            putExtra("customUrl", "")
            putExtra("customName", "")
            putExtra("customFormat", "")
            putExtra("customVolume", "")
            putExtra("customHeader", "")
            putExtra("isHidden", false)
            putExtra("width", 0)
            putExtra("height", 0)
        }
        addNewChannelLauncher.launch(intent)
        supportFragmentManager.popBackStack()
        doNotPlayOnStart = true
    }

    fun showSettings(view: View) {
        val intent = Intent(this, SettingsActivity::class.java).apply {
            putExtra("defaultFormat", defaultFormat)
            putExtra("defaultVolume", defaultVolume)
            putExtra("isShowErrorMessage", isShowErrorMessage)
            putExtra("enableBackgroundExtract", enableBackgroundExtract)
            putExtra("invertChannelButtons", invertChannelButtons)
            putExtra("hideNavigationBar", hideNavigationBar)
            putExtra("hideStatusBar", hideStatusBar)
            putExtra("useExternalJS", useExternalJS)
            putExtra("updateYtDlpOnStart", updateYtDlpOnStart)
        }
        showSettingsLauncher.launch(intent)
        supportFragmentManager.popBackStack()
        doNotPlayOnStart = true
    }

    fun showChannelNumEdit(view: View) {
        val editText = EditText(this)
        editText.inputType = InputType.TYPE_CLASS_NUMBER
        AlertDialog.Builder(this)
            .setTitle("輸入頻道號碼")
            .setView(editText)
            .setPositiveButton("確定") { _, _ ->
                if (editText.text.toString().toInt() < channel.size) {
                    switchChannel(editText.text.toString().toInt())
                }
            }
            .setNegativeButton("取消", null)
            .show()
        supportFragmentManager.popBackStack()
    }

    fun saveSettings() {
        preferences.edit {
            putInt("channelNum", channelNum)
            putString("defaultFormat", defaultFormat)
            putString("defaultVolume", defaultVolume)
            putBoolean("isShowErrorMessage", isShowErrorMessage)
            putBoolean("enableBackgroundExtract", enableBackgroundExtract)
            putBoolean("invertChannelButtons", invertChannelButtons)
            putBoolean("hideNavigationBar", hideNavigationBar)
            putBoolean("hideStatusBar", hideStatusBar)
            putBoolean("useExternalJS", useExternalJS)
            putBoolean("updateYtDlpOnStart", updateYtDlpOnStart)
        }

        // Channel list not initialized, do not write empty list
        if (!channelListLoaded) return
        try {
            val channelListObject = JSONObject()
            val newChannelArray = JSONArray()
            for (i in channel.indices) {
                val channelObject = JSONObject().apply {
                    put("customUrl", channel[i].customUrl)
                    put("customName", channel[i].customName)
                    put("customFormat", channel[i].customFormat)
                    put("customVolume", channel[i].customVolume)
                    put("customHeader", channel[i].customHeader)
                    put("isHidden", channel[i].isHidden)
                }
                if (i < channelLengthOfConfig) {
                    channelListObject.put(channel[i].defaultName, channelObject)
                } else {
                    newChannelArray.put(channelObject)
                }
            }
            val jsonObject = JSONObject().apply {
                put("customChannelList", channelListObject)
                put("newChannelArray", newChannelArray)
            }
            val jsonString = jsonObject.toString()
            val file = File(getExternalFilesDir(null), "custom.txt")
            FileOutputStream(file).use { stream ->
                stream.write(jsonString.toByteArray())
            }
        } catch (e: Exception) {
            Toast.makeText(this, "儲存設定失敗", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Save settings error", e)
        }
    }

    fun resetChannelNum() {
        channelNum = 0
        while (channel[channelNum].isHidden) {
            channelNum++
        }
    }

    fun showErrorMessage(message: String?) {
        if (!isShowErrorMessage) return
        if (!player.isPlaying) {
            errorMessageView.text = message
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

    override fun onStop() {
        super.onStop()
        isStarted = false
        player.stop()
        saveSettings()
    }

    companion object {
        const val TAG = "MainActivity"
    }
}
