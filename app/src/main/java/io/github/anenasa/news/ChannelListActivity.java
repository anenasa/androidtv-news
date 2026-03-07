package io.github.anenasa.news;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class ChannelListActivity extends Activity {

    ListView channelListView;
    final String TAG = "ChannelListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_list);
        Bundle intentExtras = getIntent().getExtras();
        if (intentExtras == null) {
            Toast.makeText(this, "Error: intentExtras is null", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error: intentExtras is null");
            finish();
            return;
        }
        String[] channelArray = intentExtras.getStringArray("nameArray");
        boolean[] isHiddenArray = intentExtras.getBooleanArray("isHiddenArray");
        if (channelArray == null || isHiddenArray == null) {
            Toast.makeText(this, "Error: array is null", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error: array is null");
            finish();
            return;
        }
        int currentNum = intentExtras.getInt("currentNum");
        ArrayList<String> displayArray = new ArrayList<>();
        ArrayList<Integer> channelNumArray = new ArrayList<>();
        for(int i=0; i<channelArray.length; i++){
            if(!isHiddenArray[i]) {
                displayArray.add(i + ". " + channelArray[i]);
                channelNumArray.add(i);
            }
        }
        channelListView = findViewById(R.id.channelListView);
        channelListView.setItemsCanFocus(true);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.mylistview, displayArray);
        channelListView.setAdapter(adapter);
        channelListView.setSelection(currentNum);
        channelListView.setOnItemClickListener((adapterView, view, i, l) -> {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("channelNum", channelNumArray.get(i));
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
        });
    }
}
