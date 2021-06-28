package io.github.anenasa.news;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.widget.TextView;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    final String TAG = "MainActivity";

    int channelNum;
    Channel[] channel;
    String input = "";

    SimpleExoPlayer player = null;
    SurfaceView playerView = null;
    TextView textView;

    Timer timer;
    TimerTask timerTask;

    SharedPreferences preferences;

    Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = getSharedPreferences("io.github.anenasa.news", MODE_PRIVATE);

        readChannelList();
        channelNum = preferences.getInt("channelNum", 0);
        if(channelNum >= channel.length){
            channelNum = 0;
        }

        try {
            YoutubeDL.getInstance().init(getApplication());
        } catch (YoutubeDLException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        player = new SimpleExoPlayer.Builder(this).build();
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(ExoPlaybackException error) {
                Log.e(TAG, Log.getStackTraceString(error));
                if(needParse(channelNum) != 0) {
                    parse(channelNum);
                }
                play(channelNum);
            }
        });
        playerView = findViewById(R.id.playerView);
        player.setVideoSurfaceView(playerView);
        textView = findViewById(R.id.textView);

        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                for(int i = 0; i < channel.length; i++){
                    parse(i);
                }
                saveSettings();
            }
        };
        timer.scheduleAtFixedRate(timerTask, 0, 3600000);
    }

    @Override
    protected void onStart() {
        super.onStart();
        play(channelNum);
    }

    void readChannelList() {
        try {
            File customFile = new File(getExternalFilesDir(null), "config.txt");
            InputStream inputStream;
            if(customFile.exists()){
                inputStream = new FileInputStream(customFile);
            }
            else{
                File customUrl = new File(getExternalFilesDir(null), "url.txt");
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
                URL url;
                if(customUrl.exists()){
                    InputStream urlStream = new FileInputStream(customUrl);
                    BufferedReader urlReader = new BufferedReader(new InputStreamReader(urlStream));
                    url = new URL(urlReader.readLine());
                }
                else{
                    url = new URL("https://anenasa.github.io/channel/config.txt");
                }
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);
                urlConnection.setDoOutput(true);
                urlConnection.connect();
                inputStream = url.openStream();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            for (String line; (line = reader.readLine()) != null; ) {
                stringBuilder.append(line).append('\n');
            }
            JSONObject json = new JSONObject(stringBuilder.toString());
            JSONArray channelList = json.getJSONArray("channelList");
            channel = new Channel[channelList.length()];
            for(int i = 0; i < channelList.length(); i++){

                JSONObject channelObject = channelList.getJSONObject(i);
                String url = channelObject.getString("url");
                String name = channelObject.getString("name");
                String format = channelObject.getString("ytdl-format");
                float volume = (float) channelObject.getDouble("volume");
                channel[i] = new Channel(i, url, name, format, volume);
                channel[i].setVideo(preferences.getString(url + format, ""));
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    void parse(int num)
    {
        YoutubeDLRequest request = new YoutubeDLRequest(channel[num].url);
        request.addOption("-f", channel[num].format);
        VideoInfo streamInfo = null;
        try {
            streamInfo = YoutubeDL.getInstance().getInfo(request);
            channel[num].setVideo(streamInfo.getUrl());
        } catch (YoutubeDLException | InterruptedException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    int needParse(int num){
        if(channel[num].video.isEmpty()) {
            return 1;
        }
        if(channel[num].url.endsWith("m3u8")){
            return 0;
        }
        if(channel[num].url.startsWith("https://www.youtube.com/")){
            long current = System.currentTimeMillis() / 1000;
            int pos = channel[num].video.indexOf("expire") + 7;
            long expire = Long.parseLong(channel[num].video.substring(pos, pos + 10));
            if(current < expire){
                return 0;
            }
            else{
                return 1;
            }
        }
        return 2;
    }

    void play(int num)
    {
        new Thread( new Runnable() {
            @Override
            public void run() {
                if(needParse(num) == 1) {
                    parse(num);
                }
                MediaItem mediaItem = MediaItem.fromUri(channel[num].video);
                // player needs to run on main thread
                mHandler.post(new Runnable() {
                    @Override
                    public void run () {
                        if(num != channelNum){
                            // Already switched to another channel, do not play this
                            return;
                        }
                        player.setMediaItem(mediaItem);
                        player.prepare();
                        player.setVolume(channel[num].volume);
                        player.play();
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                channelNum = data.getIntExtra("channelNum", 0);
                player.stop();
                play(channelNum);
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent (KeyEvent event){
        if(event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if(input.equals("")){
                        Intent intent = new Intent(this, ChannelListActivity.class);
                        String[] nameArray = new String[channel.length];
                        for(int i = 0; i < channel.length; i++){
                            nameArray[i] = channel[i].name;
                        }
                        intent.putExtra("nameArray",nameArray);
                        startActivityForResult(intent, 0);
                    }
                    else if(Integer.parseInt(input) < channel.length){
                        channelNum = Integer.parseInt(input);
                        player.stop();
                        play(channelNum);
                        clearInput();
                    }
                    else{
                        clearInput();
                    }
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    if(!input.equals("")){
                        clearInput();
                        return true;
                    }
                    else{
                        return super.dispatchKeyEvent(event);
                    }
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_CHANNEL_UP:
                    if (channelNum == 0) {
                        channelNum = channel.length - 1;
                    } else {
                        channelNum -= 1;
                    }
                    player.stop();
                    play(channelNum);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_CHANNEL_DOWN:
                    if (channelNum == channel.length - 1) {
                        channelNum = 0;
                    } else {
                        channelNum += 1;
                    }
                    player.stop();
                    play(channelNum);
                    return true;
                case KeyEvent.KEYCODE_0:
                    appendInput(0);
                    return true;
                case KeyEvent.KEYCODE_1:
                    appendInput(1);
                    return true;
                case KeyEvent.KEYCODE_2:
                    appendInput(2);
                    return true;
                case KeyEvent.KEYCODE_3:
                    appendInput(3);
                    return true;
                case KeyEvent.KEYCODE_4:
                    appendInput(4);
                    return true;
                case KeyEvent.KEYCODE_5:
                    appendInput(5);
                    return true;
                case KeyEvent.KEYCODE_6:
                    appendInput(6);
                    return true;
                case KeyEvent.KEYCODE_7:
                    appendInput(7);
                    return true;
                case KeyEvent.KEYCODE_8:
                    appendInput(8);
                    return true;
                case KeyEvent.KEYCODE_9:
                    appendInput(9);
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    void appendInput(int num) {
        input += num;
        textView.setText(input);
    }

    void clearInput() {
        input = "";
        textView.setText("");
    }

    void saveSettings() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("channelNum", channelNum);
        for (Channel value : channel) {
            editor.putString(value.url + value.format, value.video);
        }
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }

    @Override
    protected void onStop() {
        super.onStop();
        player.stop();
        saveSettings();
    }
}
