package io.github.anenasa.news

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast

class ChannelListActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channel_list)
        val intentExtras = intent.extras ?: run {
            Toast.makeText(this, "Error: intentExtras is null", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Error: intentExtras is null")
            finish()
            return
        }
        val channelArray: Array<String> = intentExtras.getStringArray("nameArray") ?: run {
            Toast.makeText(this, "Error: channelArray is null", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Error: channelArray is null")
            finish()
            return
        }
        val isHiddenArray: BooleanArray = intentExtras.getBooleanArray("isHiddenArray") ?: booleanArrayOf()
        val currentNum = intentExtras.getInt("currentNum")
        val displayArray = ArrayList<String>()
        val channelNumArray = ArrayList<Int>()
        for (i in channelArray.indices) {
            if (!isHiddenArray[i]) {
                displayArray.add("$i. ${channelArray[i]}")
                channelNumArray.add(i)
            }
        }

        findViewById<ListView>(R.id.channelListView)?.apply {
            itemsCanFocus = true
            val adapter = ArrayAdapter(context, R.layout.mylistview, displayArray)
            setAdapter(adapter)
            setSelection(currentNum)
            setOnItemClickListener { _: AdapterView<*>?, _: View?, i: Int, _: Long ->
                val returnIntent = Intent()
                returnIntent.putExtra("channelNum", channelNumArray[i])
                setResult(RESULT_OK, returnIntent)
                finish()
            }
        }

    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_CHANNEL_UP -> {
                    val listView = findViewById<ListView>(R.id.channelListView) ?: return super.dispatchKeyEvent(event)
                    val firstVisible = listView.firstVisiblePosition
                    val lastVisible = listView.lastVisiblePosition
                    val pageSize = (lastVisible - firstVisible).coerceAtLeast(1)
                    val targetPosition = if (event.keyCode == KeyEvent.KEYCODE_CHANNEL_UP) {
                        (firstVisible - pageSize).coerceAtLeast(0)
                    } else { // KEYCODE_CHANNEL_DOWN
                        (firstVisible + pageSize).coerceAtMost(listView.adapter.count - 1)
                    }
                    listView.setSelection(targetPosition)
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    companion object {
        const val TAG = "ChannelListActivity"
    }
}
