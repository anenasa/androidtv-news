package io.github.anenasa.news;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ChannelListActivity extends Activity {

    final String TAG = "ChannelListActivity";
    ListView channelListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_list);
        String[] channelArray = getIntent().getExtras().getStringArray("nameArray");
        for(int i=0;i<channelArray.length;i++){
            channelArray[i] = i + ". " + channelArray[i];
        }
        channelListView = findViewById(R.id.channelListView);
        channelListView.setItemsCanFocus(true);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, channelArray);
        channelListView.setAdapter(adapter);
        channelListView.setOnItemClickListener(new ListView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("channelNum", i);
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        });
    }
}
