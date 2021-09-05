package io.github.anenasa.news;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    final String TAG = "MainActivity";

    int channelNum;
    ArrayList<Channel> channel;
    int channelLength_config;
    String input = "";
    String defaultFormat;
    String defaultVolume;

    SimpleExoPlayer player = null;
    SurfaceView playerView = null;
    TextView textView;
    TextView errorMessageView;

    Timer timer;
    TimerTask timerTask;

    SharedPreferences preferences;

    boolean isStarted;
    AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        preferences = getSharedPreferences("io.github.anenasa.news", MODE_PRIVATE);
        channelNum = preferences.getInt("channelNum", 0);
        defaultFormat = preferences.getString("defaultFormat", "best");
        defaultVolume = preferences.getString("defaultVolume", "1.0");

        try {
            YoutubeDL.getInstance().init(getApplication());
        } catch (YoutubeDLException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            errorMessageView.setText(e.toString());
        }
        player = new SimpleExoPlayer.Builder(this).build();
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(ExoPlaybackException error) {
                Log.e(TAG, Log.getStackTraceString(error));
                errorMessageView.setText(error.toString());
                if(channel.get(channelNum).needParse() != Channel.NEEDPARSE_NO) {
                    // Force parse by removing video url
                    channel.get(channelNum).setVideo("");
                }
                play(channelNum);
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                if(state == Player.STATE_READY){
                    errorMessageView.setText("");
                }
            }
        });
        playerView = findViewById(R.id.playerView);
        player.setVideoSurfaceView(playerView);
        textView = findViewById(R.id.textView);
        errorMessageView = findViewById(R.id.errorMessage);

        readChannelList();
        if(channel == null){
            return;
        }
        if(channelNum >= channel.size()){
            resetChannelNum();
        }

        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                for(int i = 0; i < channel.size(); i++){
                    if (!channel.get(i).isHidden()) {
                        try {
                            channel.get(i).parse();
                        } catch (IOException | YoutubeDLException | JSONException | InterruptedException e) {
                            Log.e(TAG, Log.getStackTraceString(e));
                        }
                    }
                }
                saveSettings();
            }
        };
        // Delay timer for one second because if two requests are sent to
        // Hami Video at the same time, one of them will fail.
        timer.scheduleAtFixedRate(timerTask, 1000, 3600000);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(channel == null){
            return;
        }
        isStarted = true;
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
            channelLength_config = channelList.length();

            JSONObject customJsonObject = null;
            JSONObject customChannelList = null;
            channel = new ArrayList<>();
            if(!preferences.getString("jsonSettings", "").isEmpty()) {
                customJsonObject = new JSONObject(preferences.getString("jsonSettings", ""));
                customChannelList = customJsonObject.getJSONObject("customChannelList");
                JSONArray newChannelArray = customJsonObject.getJSONArray("newChannelArray");
                for(int i = 0; i < newChannelArray.length(); i++){
                    JSONObject newChannelObject = newChannelArray.getJSONObject(i);
                    channel.add(new Channel("", "", defaultFormat, Float.parseFloat(defaultVolume), ""));
                    channel.get(i).setUrl(newChannelObject.getString("customUrl"));
                    channel.get(i).setName(newChannelObject.getString("customName"));
                    channel.get(i).setFormat(newChannelObject.getString("customFormat"));
                    channel.get(i).setVolume(newChannelObject.getString("customVolume"));
                    channel.get(i).setHeader(newChannelObject.getString("customHeader"));
                    channel.get(i).setHidden(newChannelObject.getBoolean("isHidden"));
                }
            }
            for(int i = 0; i < channelList.length(); i++){

                JSONObject channelObject = channelList.getJSONObject(i);
                String url = channelObject.getString("url");
                String name = channelObject.getString("name");
                String format;
                if(channelObject.isNull("ytdl-format")) {
                    format = defaultFormat;
                }
                else{
                    format = channelObject.getString("ytdl-format");
                }
                float volume;
                if(channelObject.isNull("volume")) {
                    volume = Float.parseFloat(defaultVolume);
                }
                else{
                    volume = (float) channelObject.getDouble("volume");
                }
                String header;
                if(channelObject.isNull("header")) {
                    header = "";
                }
                else{
                    header = channelObject.getString("header");
                }
                channel.add(i, new Channel(url, name, format, volume, header));
                if(customJsonObject != null && customChannelList.has(name)) {
                    JSONObject customChannelObject = customChannelList.getJSONObject(name);
                    channel.get(i).setUrl(customChannelObject.getString("customUrl"));
                    channel.get(i).setName(customChannelObject.getString("customName"));
                    channel.get(i).setFormat(customChannelObject.getString("customFormat"));
                    channel.get(i).setVolume(customChannelObject.getString("customVolume"));
                    channel.get(i).setHeader(customChannelObject.getString("customHeader"));
                    channel.get(i).setHidden(customChannelObject.getBoolean("isHidden"));
                }
                channel.get(i).setVideo(preferences.getString(channel.get(i).getUrl() + channel.get(i).getFormat(), ""));
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            channel = null;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("頻道清單讀取失敗");
            builder.setMessage(e.toString());
            builder.setPositiveButton("確定", (dialog, id) -> finish());
            builder.setOnCancelListener(dialog -> finish());
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }

    void play(int num)
    {
        new Thread(() -> {
            if(channel.get(num).needParse() == Channel.NEEDPARSE_YES) {
                try {
                    channel.get(num).parse();
                } catch (JSONException | IOException | YoutubeDLException | InterruptedException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                    showErrorMessage(e.toString());
                }
            }
            // Fix crash if channel.get(num) is already deleted
            if(num == channel.size()){
                return;
            }
            MediaItem mediaItem = MediaItem.fromUri(channel.get(num).getVideo());
            Map<String, String> map = new HashMap<>();
            if(!channel.get(num).getHeader().isEmpty()) {
                String[] headers = channel.get(num).getHeader().split("\\\\r\\\\n");
                for (String header : headers) {
                    String[] header_split = header.split(":", 2);
                    if(header_split.length != 2){
                        showErrorMessage("header 格式錯誤");
                    }
                    else {
                        map.put(header_split[0], header_split[1]);
                    }
                }
            }
            DataSource.Factory factory = new DefaultHttpDataSource.Factory()
                    .setDefaultRequestProperties(map);
            MediaSource mediaSource = new DefaultMediaSourceFactory(factory)
                    .createMediaSource(mediaItem);
            // player needs to run on main thread
            runOnUiThread(() -> {
                if(num != channelNum){
                    // Already switched to another channel, do not play this
                    return;
                }
                player.setMediaSource(mediaSource);
                player.prepare();
                player.setVolume(channel.get(num).getVolume());
                if(isStarted){
                    player.play();
                }
            });
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            // ChannelListActivity
            if (resultCode == Activity.RESULT_OK) {
                channelNum = data.getIntExtra("channelNum", 0);
                player.stop();
                play(channelNum);
            }
        }
        else if(requestCode == 1){
            // ChannelInfoActivity - existing channel
            if (resultCode == Activity.RESULT_OK) {
                if(data.getBooleanExtra("delete", false)){
                    channel.remove(channelNum);
                    resetChannelNum();
                    return;
                }

                String url_old = channel.get(channelNum).getUrl();
                String url_new;
                if(data.getStringExtra("customUrl").isEmpty()) {
                    url_new = channel.get(channelNum).defaultUrl;
                }
                else{
                    url_new = data.getStringExtra("customUrl");
                }
                String format_old = channel.get(channelNum).getFormat();
                String format_new;
                if(data.getStringExtra("customFormat").isEmpty()){
                    format_new = channel.get(channelNum).defaultFormat;
                }
                else{
                    format_new = data.getStringExtra("customFormat");
                }
                if(!url_old.equals(url_new) || !format_old.equals(format_new)){
                    channel.get(channelNum).setVideo("");
                }
                channel.get(channelNum).setName(data.getStringExtra("customName"));
                channel.get(channelNum).setHidden(data.getBooleanExtra("isHidden", false));
                channel.get(channelNum).setUrl(data.getStringExtra("customUrl"));
                channel.get(channelNum).setFormat(data.getStringExtra("customFormat"));
                channel.get(channelNum).setVolume(data.getStringExtra("customVolume"));
                channel.get(channelNum).setHeader(data.getStringExtra("customHeader"));
                saveSettings();
            }
        }
        else if (requestCode == 2) {
            // SettingsActivity
            if (resultCode == Activity.RESULT_OK) {
                String newFormat = data.getStringExtra("defaultFormat");
                String newVolume = data.getStringExtra("defaultVolume");
                if(!defaultFormat.equals(newFormat) || !defaultVolume.equals(newVolume)){
                    defaultFormat = newFormat;
                    defaultVolume = newVolume;
                    readChannelList();
                    play(channelNum);
                }
            }
        }
        else if(requestCode == 3){
            // ChannelInfoActivity - new channel
            if (resultCode == Activity.RESULT_OK) {
                if(data.getBooleanExtra("delete", false)){
                    return;
                }
                channelNum = channel.size();
                channel.add(new Channel("", "", defaultFormat, Float.parseFloat(defaultVolume), ""));
                channel.get(channelNum).setName(data.getStringExtra("customName"));
                channel.get(channelNum).setHidden(data.getBooleanExtra("isHidden", false));
                channel.get(channelNum).setUrl(data.getStringExtra("customUrl"));
                channel.get(channelNum).setFormat(data.getStringExtra("customFormat"));
                channel.get(channelNum).setVolume(data.getStringExtra("customVolume"));
                channel.get(channelNum).setHeader(data.getStringExtra("customHeader"));
                saveSettings();
                play(channelNum);
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent (KeyEvent event){
        if(event.getAction() == KeyEvent.ACTION_DOWN && getSupportFragmentManager().getFragments().isEmpty()) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if(input.equals("")){
                        showMenu();
                    }
                    else if(Integer.parseInt(input) < channel.size()){
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
                    }
                    else{
                        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                                .setTitle("退出")
                                .setMessage("是否要退出新聞直播？")
                                .setPositiveButton("確定", (dialog, id) -> finish())
                                .setNegativeButton("取消", (dialogInterface, i) -> {});
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_CHANNEL_UP:
                    do {
                        if (channelNum == 0) {
                            channelNum = channel.size() - 1;
                        } else {
                            channelNum -= 1;
                        }
                    } while (channel.get(channelNum).isHidden());
                    player.stop();
                    play(channelNum);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_CHANNEL_DOWN:
                    do {
                        if (channelNum == channel.size() - 1) {
                            channelNum = 0;
                        } else {
                            channelNum += 1;
                        }
                    } while (channel.get(channelNum).isHidden());
                    player.stop();
                    play(channelNum);
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
                    return true;
                case KeyEvent.KEYCODE_INFO:
                    showChannelInfo(findViewById(R.id.container));
                    return true;
                case KeyEvent.KEYCODE_MENU:
                    showSettings(findViewById(R.id.container));
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN && getSupportFragmentManager().getFragments().isEmpty()){
            showMenu();
            return true;
        } else {
            return super.onTouchEvent(event);
        }
    }

    void appendInput(int num) {
        input += num;
        textView.setText(input);
    }

    void clearInput() {
        input = "";
        textView.setText("");
    }

    public void showMenu(){
        MenuFragment fragment = new MenuFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment)
                .addToBackStack(fragment.getClass().getName())
                .commit();
    }

    public void showChannelList(View view){
        Intent intent = new Intent(this, ChannelListActivity.class);
        String[] nameArray = new String[channel.size()];
        boolean[] isHiddenArray = new boolean[channel.size()];
        for(int i = 0; i < channel.size(); i++){
            nameArray[i] = channel.get(i).getName();
            isHiddenArray[i] = channel.get(i).isHidden();
        }
        intent.putExtra("nameArray", nameArray);
        intent.putExtra("isHiddenArray", isHiddenArray);
        startActivityForResult(intent, 0);
        getSupportFragmentManager().popBackStack();
    }

    public void showChannelInfo(View view){
        Intent intent = new Intent(this, ChannelInfoActivity.class);
        intent.putExtra("isNewChannel", channelNum >= channelLength_config);
        intent.putExtra("index", channelNum);
        intent.putExtra("defaultUrl", channel.get(channelNum).defaultUrl);
        intent.putExtra("defaultName", channel.get(channelNum).defaultName);
        intent.putExtra("defaultFormat", channel.get(channelNum).defaultFormat);
        intent.putExtra("defaultVolume", channel.get(channelNum).defaultVolume);
        intent.putExtra("defaultHeader", channel.get(channelNum).defaultHeader);
        intent.putExtra("customUrl", channel.get(channelNum).customUrl);
        intent.putExtra("customName", channel.get(channelNum).customName);
        intent.putExtra("customFormat", channel.get(channelNum).customFormat);
        intent.putExtra("customVolume", channel.get(channelNum).customVolume);
        intent.putExtra("customHeader", channel.get(channelNum).customHeader);
        intent.putExtra("isHidden", channel.get(channelNum).isHidden());
        intent.putExtra("width", player.getVideoSize().width);
        intent.putExtra("height", player.getVideoSize().height);
        startActivityForResult(intent, 1);
        getSupportFragmentManager().popBackStack();
    }

    public void addNewChannel(View view) {
        Intent intent = new Intent(this, ChannelInfoActivity.class);
        intent.putExtra("isNewChannel", true);
        intent.putExtra("index", channel.size());
        intent.putExtra("defaultUrl", "");
        intent.putExtra("defaultName", "");
        intent.putExtra("defaultFormat", defaultFormat);
        intent.putExtra("defaultVolume", Float.parseFloat(defaultVolume));
        intent.putExtra("defaultHeader", "");
        intent.putExtra("customUrl", "");
        intent.putExtra("customName", "");
        intent.putExtra("customFormat", "");
        intent.putExtra("customVolume", "");
        intent.putExtra("customHeader", "");
        intent.putExtra("isHidden", false);
        intent.putExtra("width", 0);
        intent.putExtra("height", 0);
        startActivityForResult(intent, 3);
        getSupportFragmentManager().popBackStack();
    }

    public void showSettings(View view){
        Intent intentSettings = new Intent(this, SettingsActivity.class);
        intentSettings.putExtra("defaultFormat", defaultFormat);
        intentSettings.putExtra("defaultVolume", defaultVolume);
        startActivityForResult(intentSettings, 2);
        getSupportFragmentManager().popBackStack();
    }

    public void showChannelNumEdit(View view){
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("輸入頻道號碼")
                .setView(editText)
                .setPositiveButton("確定", (dialog, id) -> {
                    if(Integer.parseInt(editText.getText().toString()) < channel.size()){
                        channelNum = Integer.parseInt(editText.getText().toString());
                        player.stop();
                        play(channelNum);
                    }
                })
                .setNegativeButton("取消", null);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        getSupportFragmentManager().popBackStack();
    }

    void saveSettings() {
        if(channel == null){
            return;
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("channelNum", channelNum);
        editor.putString("defaultFormat", defaultFormat);
        editor.putString("defaultVolume", defaultVolume);
        for (Channel value : channel) {
            editor.putString(value.getUrl() + value.getFormat(), value.getVideo());
        }
        try {
            JSONObject jsonObject = new JSONObject();
            JSONObject channelListObject = new JSONObject();
            JSONArray newChannelArray = new JSONArray();
            for(int i = 0; i < channel.size(); i++){
                JSONObject channelObject = new JSONObject();
                channelObject.put("customUrl", channel.get(i).customUrl);
                channelObject.put("customName", channel.get(i).customName);
                channelObject.put("customFormat", channel.get(i).customFormat);
                channelObject.put("customVolume", channel.get(i).customVolume);
                channelObject.put("customHeader", channel.get(i).customHeader);
                channelObject.put("isHidden", channel.get(i).isHidden());
                if(i < channelLength_config) {
                    channelListObject.put(channel.get(i).defaultName, channelObject);
                }
                else{
                    newChannelArray.put(channelObject);
                }
            }
            jsonObject.put("customChannelList", channelListObject);
            jsonObject.put("newChannelArray", newChannelArray);
            editor.putString("jsonSettings", jsonObject.toString());
        } catch (JSONException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        editor.apply();
    }

    void resetChannelNum(){
        channelNum = 0;
        while (channel.get(channelNum).isHidden()){
            channelNum++;
        }
    }

    void showErrorMessage(String message){
        runOnUiThread(() -> {
            if(!player.isPlaying()) {
                errorMessageView.setText(message);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isStarted = false;
        player.stop();
        saveSettings();
    }
}
