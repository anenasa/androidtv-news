package io.github.anenasa.news;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
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
    }

    @Override
    public boolean dispatchKeyEvent (KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER
                    || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("channelNum",channelListView.getSelectedItemPosition());
                setResult(Activity.RESULT_OK,returnIntent);
                finish();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
