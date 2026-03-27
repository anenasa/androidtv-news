package io.github.anenasa.news

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.codekidlabs.storagechooser.StorageChooser
import io.github.anenasa.news.YtDlp.Companion.download
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.concurrent.thread
import androidx.core.net.toUri

class SettingsActivity : AppCompatActivity() {
    var defaultFormat: String = ""
    var defaultVolume: String = ""
    var isShowErrorMessage: Boolean = false
    var enableBackgroundExtract: Boolean = false
    var invertChannelButtons: Boolean = false
    var hideNavigationBar: Boolean = false
    var hideStatusBar: Boolean = false
    var useExternalJS: Boolean = false
    var updateYtDlpOnStart: Boolean = false
    var ytDlpUpdated: Boolean = false
    var removeCache: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val intentExtras = intent.extras ?: run {
            Toast.makeText(this, "Error: intentExtras is null", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Error: intentExtras is null")
            finish()
            return
        }
        defaultFormat = intentExtras.getString("defaultFormat") ?: "bv*+ba/b"
        defaultVolume = intentExtras.getString("defaultVolume") ?: "1.0"
        isShowErrorMessage = intentExtras.getBoolean("isShowErrorMessage")
        enableBackgroundExtract = intentExtras.getBoolean("enableBackgroundExtract")
        invertChannelButtons = intentExtras.getBoolean("invertChannelButtons")
        hideNavigationBar = intentExtras.getBoolean("hideNavigationBar")
        hideStatusBar = intentExtras.getBoolean("hideStatusBar")
        useExternalJS = intentExtras.getBoolean("useExternalJS")
        updateYtDlpOnStart = intentExtras.getBoolean("updateYtDlpOnStart")
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, SettingsFragment())
            .commit()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent().apply {
                    putExtra("defaultFormat", defaultFormat)
                    putExtra("defaultVolume", defaultVolume)
                    putExtra("isShowErrorMessage", isShowErrorMessage)
                    putExtra("enableBackgroundExtract", enableBackgroundExtract)
                    putExtra("invertChannelButtons", invertChannelButtons)
                    putExtra("hideNavigationBar", hideNavigationBar)
                    putExtra("hideStatusBar", hideStatusBar)
                    putExtra("useExternalJS", useExternalJS)
                    putExtra("updateYtDlpOnStart", updateYtDlpOnStart)
                    putExtra("remove_cache", removeCache)
                    putExtra("ytDlpUpdated", ytDlpUpdated)
                }
                setResult(RESULT_OK, intent)
                finish()
            }
        })
    }

    private fun showFileChooser(outputName: String) {
        val chooser = StorageChooser.Builder()
            .withActivity(this)
            .withFragmentManager(fragmentManager)
            .withMemoryBar(true)
            .allowCustomPath(true)
            .setType(StorageChooser.FILE_PICKER)
            .build()
        chooser.show()
        chooser.setOnSelectListener { path: String? ->
            try {
                FileInputStream(path).use { inputStream ->
                    copyToExternal(inputStream, outputName)
                }
            } catch (e: IOException) {
                Toast.makeText(this, "檔案匯入失敗", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "showFileChooser error", e)
            }
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        val myActivity: SettingsActivity by lazy {
            requireActivity() as SettingsActivity
        }
        var saveFileName: String = ""

        private val requestPermissionLauncher = registerForActivityResult(RequestPermission()) { isGranted: Boolean? ->
            if (isGranted == true) {
                myActivity.showFileChooser(saveFileName)
            }
        }

        private val selectFileLauncher = registerForActivityResult<String?, Uri?>(GetContent()) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult
            try {
                myActivity.contentResolver.openInputStream(uri).use { inputStream ->
                    if (inputStream == null) {
                        throw IOException("inputStream is null")
                    }
                    myActivity.copyToExternal(inputStream, saveFileName)
                }
            } catch (e: IOException) {
                Toast.makeText(myActivity, "檔案匯入失敗", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "selectFileLauncher error", e)
            }
        }

        @SuppressLint("ObsoleteSdkInt")
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.activity_settings, rootKey)

            findPreference<Preference>("config")?.apply {
                setOnPreferenceClickListener { _: Preference? ->
                    saveToFile("config.txt")
                    true
                }
            }


            findPreference<EditTextPreference>("configurl")?.apply {
                setOnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                    val file = File(requireActivity().getExternalFilesDir(null), "config.txt")
                    // Without deleting first, when config.txt is already created with adb push,
                    // writing will fail with "java.io.FileNotFoundException" "open failed: EACCES (Permission denied)"
                    if (file.exists() && !file.delete()) {
                        Log.e(TAG, "failed to delete file")
                    }
                    if (!newValue.toString().isEmpty()) {
                        try {
                            FileOutputStream(file).use { stream ->
                                stream.write(("""{"channelList": [{"list": "$newValue"}]}""").toByteArray())
                            }
                        } catch (e: IOException) {
                            Log.e(TAG, "prefConfigurl error", e)
                            preference?.setSummary(e.message)
                        }
                    }
                    true
                }
            }

            findPreference<Preference>("cookies")?.apply {
                setOnPreferenceClickListener { _: Preference? ->
                    saveToFile("cookies.txt")
                    true
                }
            }

            findPreference<EditTextPreference>("format")?.apply {
                setSummary(myActivity.defaultFormat)
                setText(myActivity.defaultFormat)
                setOnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                    if (newValue.toString().isEmpty()) {
                        myActivity.defaultFormat = "bv*+ba/b"
                        preference?.setSummary("bv*+ba/b")
                    } else {
                        myActivity.defaultFormat = newValue.toString()
                        preference?.setSummary(newValue.toString())
                    }
                    true
                }
            }

            findPreference<EditTextPreference>("volume")?.apply {
                setSummary(myActivity.defaultVolume)
                setText(myActivity.defaultVolume)
                setOnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                    if (newValue.toString().isEmpty()) {
                        myActivity.defaultVolume = "1.0"
                        preference?.setSummary("1.0")
                    } else {
                        myActivity.defaultVolume = newValue.toString()
                        preference?.setSummary(newValue.toString())
                    }
                    true
                }
            }

            findPreference<SwitchPreference>("isShowErrorMessage")?.apply {
                setChecked(myActivity.isShowErrorMessage)
                setOnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                    myActivity.isShowErrorMessage = newValue as Boolean
                    true
                }
            }

            findPreference<SwitchPreference>("enableBackgroundExtract")?.apply {
                setChecked(myActivity.enableBackgroundExtract)
                setOnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                    myActivity.enableBackgroundExtract = newValue as Boolean
                    true
                }
            }

            findPreference<SwitchPreference>("invertChannelButtons")?.apply {
                setChecked(myActivity.invertChannelButtons)
                setOnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                    myActivity.invertChannelButtons = newValue as Boolean
                    true
                }
            }

            findPreference<SwitchPreference>("hideNavigationBar")?.apply {
                setChecked(myActivity.hideNavigationBar)
                setOnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                    myActivity.hideNavigationBar = newValue as Boolean
                    true
                }
            }

            findPreference<SwitchPreference>("hideStatusBar")?.apply {
                setChecked(myActivity.hideStatusBar)
                setOnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                    myActivity.hideStatusBar = newValue as Boolean
                    true
                }
            }

            findPreference<SwitchPreference>("useExternalJS")?.apply {
                setChecked(myActivity.useExternalJS)
                setOnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                    myActivity.useExternalJS = newValue as Boolean
                    true
                }
            }

            findPreference<SwitchPreference>("updateYtDlpOnStart")?.apply {
                setChecked(myActivity.updateYtDlpOnStart)
                setOnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                    myActivity.updateYtDlpOnStart = newValue as Boolean
                    true
                }
            }

            findPreference<Preference>("update_ytdlp")?.apply {
                setSummary(YtDlp.version)
                setOnPreferenceClickListener { _: Preference? ->
                    setSummary("正在更新")
                    thread {
                        try {
                            download(myActivity.getExternalFilesDir(null))
                            myActivity.runOnUiThread { setSummary("已更新，將重新啟動應用程式") }
                            myActivity.ytDlpUpdated = true
                        } catch (e: IOException) {
                            myActivity.runOnUiThread { setSummary("更新時發生錯誤") }
                            Log.e(TAG, "Preference update_ytdlp error", e)
                        }
                    }
                    true
                }
            }

            findPreference<Preference>("update")?.apply {
                setOnPreferenceClickListener { _: Preference? ->
                    setSummary("正在檢查更新")
                    thread {
                        try {
                            val request = Request.Builder()
                                .url("https://raw.githubusercontent.com/anenasa/androidtv-news/main/VERSION")
                                .build()
                            val versionNew: String?
                            MyApplication.okHttpClient.newCall(request).execute().use { response ->
                                if (!response.isSuccessful) {
                                    throw IOException("下載失敗：$response")
                                }
                                versionNew = response.body.string().trim()
                            }
                            if (versionNew != BuildConfig.VERSION_NAME.substringBefore("-")) {
                                myActivity.runOnUiThread { setSummary("有新版本") }
                                val apiString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) "" else "-api21"
                                val link = "https://github.com/anenasa/androidtv-news/releases/download/v$versionNew/androidtv-news$apiString-$versionNew.apk"
                                val intent = Intent(Intent.ACTION_VIEW, link.toUri())
                                startActivity(intent)
                            } else {
                                myActivity.runOnUiThread { setSummary("已經是最新版本") }
                            }
                        } catch (e: IOException) {
                            myActivity.runOnUiThread { setSummary("更新時發生錯誤") }
                            Log.e(TAG, "Preference update error", e)
                        }
                    }
                    true
                }
            }

            findPreference<Preference>("about")?.apply {
                setOnPreferenceClickListener { preference: Preference? ->
                    val stringBuilder = StringBuilder()
                    stringBuilder.append("新聞直播 ${BuildConfig.VERSION_NAME} 版\n")
                        .append("專案頁面：https://github.com/anenasa/androidtv-news\n")
                        .append("授權：GNU General Public License v3.0\n")
                        .append("函式庫：\n")
                        .append("yt-dlp - The Unlicense\n")
                        .append("Chaquopy - MIT License\n")
                        .append("Node.js - MIT License\n")
                        .append("QuickJS - MIT License\n")
                        .append("ExoPlayer - Apache License 2.0\n")
                        .append("OkHttp - Apache License 2.0\n")
                        .append("Storage Chooser - Mozilla Public License Version 2.0\n\n")
                    try {
                        resources.openRawResource(R.raw.gpl3).bufferedReader().use { reader ->
                            stringBuilder.append(reader.readText()).append('\n')
                        }
                        stringBuilder.append("The Unlicense").append('\n')
                        resources.openRawResource(R.raw.unlicense).bufferedReader().use { reader ->
                            stringBuilder.append(reader.readText()).append('\n')
                        }
                        stringBuilder.append("MIT License").append('\n')
                        resources.openRawResource(R.raw.chaquopy).bufferedReader().use { reader ->
                            stringBuilder.append(reader.readText()).append('\n')
                        }
                        resources.openRawResource(R.raw.nodejs).bufferedReader().use { reader ->
                            stringBuilder.append(reader.readText()).append('\n')
                        }
                        resources.openRawResource(R.raw.quickjs).bufferedReader().use { reader ->
                            stringBuilder.append(reader.readText()).append('\n')
                        }
                        resources.openRawResource(R.raw.apache2).bufferedReader().use { reader ->
                            stringBuilder.append(reader.readText()).append('\n')
                        }
                        resources.openRawResource(R.raw.mpl2).bufferedReader().use { reader ->
                            stringBuilder.append(reader.readText())
                        }
                    } catch (e: IOException) {
                        Toast.makeText(myActivity, "讀取許可證失敗", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Read license error", e)
                    }

                    AlertDialog.Builder(preference?.context)
                        .setTitle("關於")
                        .setMessage(stringBuilder.toString())
                        .setPositiveButton("確定") { dialog: DialogInterface?, _: Int -> dialog?.dismiss() }
                        .show()
                    true
                }
            }

            findPreference<Preference>("remove_cache")?.apply {
                setOnPreferenceClickListener { preference: Preference? ->
                    myActivity.removeCache = true
                    preference?.setSummary("將移除快取")
                    true
                }
            }
        }

        fun saveToFile(outputName: String) {
            saveFileName = outputName

            // Check if file picker is available
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/*"
            }
            val packageManager = requireActivity().packageManager
            val componentName = intent.resolveActivity(packageManager)
            // Possible results of componentName if no third-party file picker is installed:
            // com.android.documentsui on non-Android TV
            // null on Android TV <= 10
            // com.google.android.tv.frameworkpackagestubs on Android TV 11
            // com.android.tv.frameworkpackagestubs on Android TV 12 to 16
            if (componentName != null && !componentName.packageName.endsWith("frameworkpackagestubs")) {
                selectFileLauncher.launch("text/*")
            } else {
                // No file picker available
                // https://stackoverflow.com/a/38715569/20756028
                if (ContextCompat.checkSelfPermission(myActivity, Manifest.permission.READ_EXTERNAL_STORAGE) == -1) {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                } else {
                    myActivity.showFileChooser(outputName)
                }
            }
        }
    }

    fun copyToExternal(inputStream: InputStream, outputName: String) {
        val outputFile = File(getExternalFilesDir(null), outputName)
        // Without deleting first, when output file is already created with adb push,
        // writing will fail with "java.io.FileNotFoundException" "open failed: EACCES (Permission denied)"
        if (outputFile.exists() && !outputFile.delete()) {
            Log.e(TAG, "failed to delete file")
        }
        FileOutputStream(outputFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }

    companion object {
        const val TAG = "SettingsActivity"
    }
}
