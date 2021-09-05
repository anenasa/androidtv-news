package io.github.anenasa.news;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class ChannelListActivity extends Activity {

    ListView channelListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_list);
        String[] channelArray = getIntent().getExtras().getStringArray("nameArray");
        boolean[] isHiddenArray = getIntent().getExtras().getBooleanArray("isHiddenArray");
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
        channelListView.setOnItemClickListener((adapterView, view, i, l) -> {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("channelNum", channelNumArray.get(i));
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
        });
    }
}
