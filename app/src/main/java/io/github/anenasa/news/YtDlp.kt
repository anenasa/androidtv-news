package io.github.anenasa.news

import android.content.Context
import android.util.Log
import com.chaquo.python.PyException
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException

class YtDlp private constructor(val externalFilesDir: File, val nativeLibraryDir: String, var useExternalJS: Boolean) {
    private val ytDlp: PyObject
    private val cookies: String

    init {
        val py = Python.getInstance()
        val filename = File(externalFilesDir, "yt-dlp").toString()
        val path = py.getModule("sys")["path"] ?: throw PyException("sys.path is null")
        path.callAttr("insert", 0, filename)
        ytDlp = py.getModule("yt_dlp")
        version = py.getModule("yt_dlp.version")["__version__"].toString()
        cookies = File(externalFilesDir, "cookies.txt").toString()

        // Fix OpenSSL configuration error of nodejs
        val environ = py.getModule("os")["environ"] ?: throw PyException("os.environ is null")
        environ.callAttr("__setitem__", "OPENSSL_CONF", "/dev/null")
    }

    /**
     * Extract video url
     * @param url url to extract
     * @param option python dictionary of options for yt-dlp
     * @return extracted video url
     * @throws PyException extraction failed
     * @see [list of options](https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/YoutubeDL.py.L184)
     */
    fun extract(url: String, option: PyObject): String {
        if (useExternalJS) {
            setEjs(option)
        }
        option.callAttr("__setitem__", "cookiefile", cookies)

        var ydl = ytDlp.callAttr("YoutubeDL", option)
        var infoDict: PyObject
        try {
            infoDict = ydl.callAttr("extract_info", url, false)
        } catch (e: PyException) {
            if (useExternalJS) {
                throw e
            }
            Log.e(TAG, "取得影片網址失敗，將使用 EJS 重試")
            setEjs(option)
            ydl = ytDlp.callAttr("YoutubeDL", option)
            infoDict = ydl.callAttr("extract_info", url, false)
        }

        // url is in entries for playlist
        infoDict.callAttr("get", "entries")?.let {
            infoDict = it.callAttr("__getitem__", 0)
        }

        return infoDict.callAttr("get", "url")?.toString() ?:
        // url is in requested_formats for merging multiple formats
            infoDict.callAttr("get", "requested_formats")?.let {
            "${it.callAttr("__getitem__", 0).callAttr("__getitem__", "url")}\n" +
                    "${it.callAttr("__getitem__", 1).callAttr("__getitem__", "url")}"
        } ?: throw PyException("找不到影片網址")
    }

    /**
     * Add EJS to option
     * @param option python dictionary of options for yt-dlp
     */
    private fun setEjs(option: PyObject) {
        @Suppress("KotlinConstantConditions")
        if (BuildConfig.USE_API_21) {
            val ejsPath = "$nativeLibraryDir/libqjs.so"
            val path = Python.getInstance().builtins.callAttr("dict")
            path.callAttr("__setitem__", "path", ejsPath)
            val jsRuntimes = Python.getInstance().builtins.callAttr("dict")
            jsRuntimes.callAttr("__setitem__", "quickjs", path)
            option.callAttr("__setitem__", "js_runtimes", jsRuntimes)
        } else {
            val ejsPath = "$nativeLibraryDir/libnode.so"
            val path = Python.getInstance().builtins.callAttr("dict")
            path.callAttr("__setitem__", "path", ejsPath)
            val jsRuntimes = Python.getInstance().builtins.callAttr("dict")
            jsRuntimes.callAttr("__setitem__", "node", path)
            option.callAttr("__setitem__", "js_runtimes", jsRuntimes)
        }
    }

    companion object {
        private const val TAG = "YtDlp"
        var version: String? = null

        /**
         * Create YtDlp object
         * @param context Context
         * @param updateYtDlpOnStart Download yt-dlp from GitHub before initializing
         * @param useExternalJS Use external js runtime by default, otherwise only after failure
         * @exception IOException Downloading yt-dlp failed
         * @exception PyException Python exception
         */
        fun create(context: Context, updateYtDlpOnStart: Boolean, useExternalJS: Boolean): YtDlp {
            val externalFilesDir: File = context.getExternalFilesDir(null) ?: throw IOException("externalFilesDir is null")
            val nativeLibraryDir: String = context.applicationInfo.nativeLibraryDir
            // TODO: Return Result class
            try {
                if (updateYtDlpOnStart || !File(externalFilesDir, "yt-dlp").exists()) {
                    download(externalFilesDir)
                }
                return YtDlp(externalFilesDir, nativeLibraryDir, useExternalJS)
            } catch (e: Exception) {
                File(externalFilesDir, "yt-dlp").delete()
                throw e
            }
        }

        /**
         * Download yt-dlp binary to external storage
         * @param externalFilesDir Context.getExternalFilesDir(null)
         * @exception IOException Downloading yt-dlp failed
         */
        fun download(externalFilesDir: File?) {
            externalFilesDir ?: throw IOException("externalFilesDir is null")
            val file = File(externalFilesDir, "yt-dlp.part")
            val request = Request.Builder()
                .url("https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp")
                .build()
            MyApplication.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("下載失敗：$response")
                }
                file.sink().buffer().use { sink ->
                    sink.writeAll(response.body.source())
                }
            }
            if (!file.renameTo(File(externalFilesDir, "yt-dlp"))) {
                throw IOException("Renaming failed")
            }
        }
    }
}
