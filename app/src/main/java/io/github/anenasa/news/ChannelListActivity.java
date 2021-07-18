package io.github.anenasa.news;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class ChannelListActivity extends Activity {

    final String TAG = "ChannelListActivity";
    ListView channelListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_list);
        String[] channelArray = getIntent().getExtras().getStringArray("nameArray");
        boolean[] isHiddenArray = getIntent().getExtras().getBooleanArray("isHiddenArray");
        ArrayList<String> displayArray = new ArrayList<String>();
        ArrayList<Integer> channelNumArray = new ArrayList<Integer>();
        for(int i=0; i<channelArray.length; i++){
            if(!isHiddenArray[i]) {
                displayArray.add(i + ". " + channelArray[i]);
                channelNumArray.add(i);
            }
        }
        channelListView = findViewById(R.id.channelListView);
        channelListView.setItemsCanFocus(true);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, displayArray);
        channelListView.setAdapter(adapter);
        channelListView.setOnItemClickListener(new ListView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("channelNum", channelNumArray.get(i));
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        });
    }
}
