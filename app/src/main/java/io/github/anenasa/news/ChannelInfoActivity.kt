package io.github.anenasa.news

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat

class ChannelInfoActivity : AppCompatActivity() {
    var fragment: ChannelInfoFragment = ChannelInfoFragment()
    var isNewChannel: Boolean = false
    var channelIndex: Int = 0
    var defaultUrl: String = ""
    var defaultName: String = ""
    var defaultFormat: String = ""
    var defaultVolume: Float = 1f
    var defaultHeader: String = ""
    var customUrl: String = ""
    var customName: String = ""
    var customFormat: String = ""
    var customVolume: String = ""
    var customHeader: String = ""
    var channelIsHidden: Boolean = false
    var channelWidth: Int = 0
    var channelHeight: Int = 0
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
        isNewChannel = intentExtras.getBoolean("isNewChannel")
        channelIndex = intentExtras.getInt("index")
        defaultUrl = intentExtras.getString("defaultUrl").orEmpty()
        defaultName = intentExtras.getString("defaultName").orEmpty()
        defaultFormat = intentExtras.getString("defaultFormat").orEmpty()
        defaultVolume = intentExtras.getFloat("defaultVolume")
        defaultHeader = intentExtras.getString("defaultHeader").orEmpty()
        customUrl = intentExtras.getString("customUrl").orEmpty()
        customName = intentExtras.getString("customName").orEmpty()
        customFormat = intentExtras.getString("customFormat").orEmpty()
        customVolume = intentExtras.getString("customVolume").orEmpty()
        customHeader = intentExtras.getString("customHeader").orEmpty()
        channelIsHidden = intentExtras.getBoolean("isHidden")
        channelWidth = intentExtras.getInt("width")
        channelHeight = intentExtras.getInt("height")
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, fragment)
            .commit()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent().apply {
                    putExtra("customName", customName)
                    putExtra("isHidden", channelIsHidden)
                    putExtra("customUrl", customUrl)
                    putExtra("customFormat", customFormat)
                    putExtra("customVolume", customVolume)
                    putExtra("customHeader", customHeader)
                    putExtra("remove_cache", removeCache)
                }
                setResult(RESULT_OK, intent)
                finish()
            }
        })
    }

    class ChannelInfoFragment : PreferenceFragmentCompat() {
        val myActivity: ChannelInfoActivity by lazy { requireActivity() as ChannelInfoActivity }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.activity_channel_info, rootKey)

            findPreference<EditTextPreference?>("name")?.apply {
                if (myActivity.customName.isEmpty()) {
                    setSummary(myActivity.defaultName)
                    setText(myActivity.defaultName)
                } else {
                    setSummary(myActivity.customName)
                    setText(myActivity.customName)
                }
                setOnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                    myActivity.customName = newValue.toString()
                    if (newValue.toString().isEmpty()) {
                        preference?.setSummary(myActivity.defaultName)
                    } else {
                        preference?.setSummary(newValue.toString())
                    }
                    true
                }
            }

            findPreference<SwitchPreferenceCompat?>("isHidden")?.apply {
                setChecked(myActivity.channelIsHidden)
                setOnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                    myActivity.channelIsHidden = newValue as Boolean
                    true
                }
            }

            findPreference<Preference?>("number")?.apply {
                setSummary(myActivity.channelIndex.toString())
            }

            findPreference<EditTextPreference?>("url")?.apply {
                if (myActivity.customUrl.isEmpty()) {
                    setSummary(myActivity.defaultUrl)
                    setText(myActivity.defaultUrl)
                } else {
                    setSummary(myActivity.customUrl)
                    setText(myActivity.customUrl)
                }
                setOnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                    myActivity.customUrl = newValue.toString()
                    if (newValue.toString().isEmpty()) {
                        preference?.setSummary(myActivity.defaultUrl)
                    } else {
                        preference?.setSummary(newValue.toString())
                    }
                    true
                }
            }

            findPreference<EditTextPreference?>("format")?.apply {
                if (myActivity.customFormat.isEmpty()) {
                    setSummary(myActivity.defaultFormat)
                    setText(myActivity.defaultFormat)
                } else {
                    setSummary(myActivity.customFormat)
                    setText(myActivity.customFormat)
                }
                setOnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                    myActivity.customFormat = newValue.toString()
                    if (newValue.toString().isEmpty()) {
                        preference?.setSummary(myActivity.defaultFormat)
                    } else {
                        preference?.setSummary(newValue.toString())
                    }
                    true
                }
            }

            findPreference<EditTextPreference?>("volume")?.apply {
                if (myActivity.customVolume.isEmpty()) {
                    setSummary(myActivity.defaultVolume.toString())
                    setText(myActivity.defaultVolume.toString())
                } else {
                    setSummary(myActivity.customVolume)
                    setText(myActivity.customVolume)
                }
                setOnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                    myActivity.customVolume = newValue.toString()
                    if (newValue.toString().isEmpty()) {
                        preference?.setSummary(myActivity.defaultVolume.toString())
                    } else {
                        preference?.setSummary(newValue.toString())
                    }
                    true
                }
            }

            findPreference<Preference?>("size")?.apply {
                setSummary("${myActivity.channelWidth}×${myActivity.channelHeight}")
            }

            findPreference<EditTextPreference?>("header")?.apply {
                if (myActivity.customHeader.isEmpty()) {
                    setSummary(myActivity.defaultHeader)
                    setText(myActivity.defaultHeader)
                } else {
                    setSummary(myActivity.customHeader)
                    setText(myActivity.customHeader)
                }
                setOnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                    myActivity.customHeader = newValue.toString()
                    if (newValue.toString().isEmpty()) {
                        preference?.setSummary(myActivity.defaultHeader)
                    } else {
                        preference?.setSummary(newValue.toString())
                    }
                    true
                }
            }

            findPreference<Preference?>("remove_cache")?.apply {
                setOnPreferenceClickListener { preference: Preference? ->
                    myActivity.removeCache = true
                    preference?.setSummary("將移除快取")
                    true
                }
            }

            findPreference<Preference?>("delete")?.apply {
                isVisible = myActivity.isNewChannel
                setOnPreferenceClickListener { _: Preference? ->
                    val builder = AlertDialog.Builder(activity)
                        .setTitle("刪除")
                        .setMessage("確定要刪除嗎？")
                        .setPositiveButton("確定") { _: DialogInterface?, _: Int ->
                            val returnIntent = Intent()
                            returnIntent.putExtra("delete", true)
                            myActivity.setResult(RESULT_OK, returnIntent)
                            myActivity.finish()
                        }
                        .setNegativeButton("取消", null)
                    val alertDialog = builder.create()
                    alertDialog.show()
                    true
                }
            }
        }
    }

    companion object {
        const val TAG = "ChannelInfoActivity"
    }
}
