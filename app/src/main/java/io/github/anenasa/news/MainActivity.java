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

import com.chaquo.python.PyException;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.MergingMediaSource;

public class MainActivity extends AppCompatActivity {
    final String TAG = "MainActivity";

    int channelNum;
    ArrayList<Channel> channel;
    int channelLength_config;
    String input = "";
    String defaultFormat;
    String defaultVolume;
    boolean isShowErrorMessage;
    boolean enableBackgroundExtract;
    boolean invertChannelButtons;
    boolean hideNavigationBar;
    boolean hideStatusBar;

    YtDlp ytdlp;
    ExoPlayer player = null;
    SurfaceView playerView = null;
    TextView textView;
    TextView textInfo;
    TextView errorMessageView;
    Timer timer;
    TimerTask timerTask;

    SharedPreferences preferences;

    boolean isStarted;
    AudioManager audioManager;
    int errorCount = 0;
    boolean DO_NOT_PLAY_ON_START = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            ytdlp = new YtDlp(this);
        } catch (PyException | IOException e) {
            // Log.getStackTraceString does not output UnknownHostException
            // https://stackoverflow.com/questions/18544539/android-log-x-not-printing-stacktrace
            if(Log.getStackTraceString(e).isEmpty() && e.getMessage() != null) {
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
            }
            else
                Log.e(TAG, Log.getStackTraceString(e));
            // AlertDialog does not work in onCreate(), so I show it in onStart()
            return;
        }
        setContentView(R.layout.activity_main);
        audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        preferences = getSharedPreferences("io.github.anenasa.news", MODE_PRIVATE);
        channelNum = preferences.getInt("channelNum", 0);
        defaultFormat = preferences.getString("defaultFormat", "bv*+ba/b");
        defaultVolume = preferences.getString("defaultVolume", "1.0");
        isShowErrorMessage = preferences.getBoolean("isShowErrorMessage", false);
        enableBackgroundExtract = preferences.getBoolean("enableBackgroundExtract", false);
        invertChannelButtons = preferences.getBoolean("invertChannelButtons", false);
        hideNavigationBar = preferences.getBoolean("hideNavigationBar", false);
        hideStatusBar = preferences.getBoolean("hideStatusBar", false);

        player = new ExoPlayer.Builder(this).build();
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e(TAG, Log.getStackTraceString(error));
                showErrorMessage(error.toString());
                if(channel.get(channelNum).needParse() != Channel.NEEDPARSE_NO) {
                    if(errorCount > 0) {
                        // Force parse by removing video url
                        channel.get(channelNum).setVideo("");
                        errorCount = 0;
                    }
                    else errorCount++;
                }
                play(channelNum);
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                if(state == Player.STATE_READY){
                    errorMessageView.setText("");
                    textInfo.setText("");
                    errorCount = 0;
                }
                else if(state == Player.STATE_ENDED){
                    channel.get(channelNum).setVideo("");
                    play(channelNum);
                }
            }
        });
        playerView = findViewById(R.id.playerView);
        player.setVideoSurfaceView(playerView);
        textView = findViewById(R.id.textView);
        textInfo = findViewById(R.id.textInfo);
        errorMessageView = findViewById(R.id.errorMessage);

        readChannelList();
        if(channel == null){
            return;
        }
        if(channelNum >= channel.size()){
            resetChannelNum();
        }

        if(enableBackgroundExtract) {
            timer = new Timer();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    for (int i = 0; i < channel.size(); i++) {
                        try {
                            if (!channel.get(i).isHidden()) {
                                channel.get(i).parse(ytdlp);
                            }
                        } catch (JSONException | IOException | InterruptedException |
                                 InvalidAlgorithmParameterException | IllegalBlockSizeException |
                                 NoSuchPaddingException | BadPaddingException |
                                 NoSuchAlgorithmException |
                                 InvalidKeyException | PyException e) {
                            Log.e(TAG, Log.getStackTraceString(e));
                        }
                    }
                    saveSettings();
                }
            };
            // Delay timer for one second because if two requests are sent to
            // Hami Video at the same time, one of them will fail.
            timer.schedule(timerTask, 1000, 3600000);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ytdlp == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("yt-dlp 載入失敗");
            builder.setNegativeButton("確定", (dialog, id) -> finish());
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
        isStarted = true;
        // Do not call play() more than once
        // play() will be called in onActivityResult()
        if(DO_NOT_PLAY_ON_START){
            DO_NOT_PLAY_ON_START = false;
            return;
        }
        if(channel == null){
            return;
        }
        play(channelNum);
    }

    @Override
    protected void onResume() {
        super.onResume();
        int system_ui_flag = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        if (hideStatusBar){
            system_ui_flag += View.SYSTEM_UI_FLAG_FULLSCREEN;
        }
        if (hideNavigationBar){
            system_ui_flag += View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }
        if (system_ui_flag != View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) {
            getWindow().getDecorView().setSystemUiVisibility(system_ui_flag);
        }
    }

    void readChannelList() {
        channel = new ArrayList<>();
        try {
            File configFile = new File(getExternalFilesDir(null), "config.txt");
            InputStream inputStream;
            if(configFile.exists()){
                inputStream = new FileInputStream(configFile);
            }
            else{
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
                URL url = new URL("https://anenasa.github.io/channel/config.txt");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);
                urlConnection.setDoOutput(true);
                urlConnection.connect();
                inputStream = url.openStream();
            }
            readChannelListFromStream(inputStream);
            channelLength_config = channel.size();

            JSONObject customJsonObject = null;
            JSONObject customChannelList = null;
            JSONArray newChannelArray = null;
            File customFile = new File(getExternalFilesDir(null), "custom.txt");
            if(customFile.exists()){
                InputStream customStream = new FileInputStream(customFile);
                BufferedReader customReader = new BufferedReader(new InputStreamReader(customStream));
                StringBuilder customStringBuilder = new StringBuilder();
                for (String line; (line = customReader.readLine()) != null; ) {
                    customStringBuilder.append(line).append('\n');
                }
                customJsonObject = new JSONObject(customStringBuilder.toString());
                customChannelList = customJsonObject.getJSONObject("customChannelList");
                newChannelArray = customJsonObject.getJSONArray("newChannelArray");
            }

            // Read custom settings for this channel
            for(Channel ch: channel) {
                if (customJsonObject != null && customChannelList.has(ch.getName())) {
                    JSONObject customChannelObject = customChannelList.getJSONObject(ch.getName());
                    ch.setUrl(customChannelObject.getString("customUrl"));
                    ch.setName(customChannelObject.getString("customName"));
                    ch.setFormat(customChannelObject.getString("customFormat"));
                    ch.setVolume(customChannelObject.getString("customVolume"));
                    ch.setHeader(customChannelObject.getString("customHeader"));
                    ch.setHidden(customChannelObject.getBoolean("isHidden"));
                }
            }

            //Read custom channel set in app
            if(newChannelArray != null){
                for(int i = 0; i < newChannelArray.length(); i++){
                    JSONObject newChannelObject = newChannelArray.getJSONObject(i);
                    Channel ch = new Channel("", "", defaultFormat, Float.parseFloat(defaultVolume), "", new HashMap<>());
                    ch.setUrl(newChannelObject.getString("customUrl"));
                    ch.setName(newChannelObject.getString("customName"));
                    ch.setFormat(newChannelObject.getString("customFormat"));
                    ch.setVolume(newChannelObject.getString("customVolume"));
                    ch.setHeader(newChannelObject.getString("customHeader"));
                    ch.setHidden(newChannelObject.getBoolean("isHidden"));
                    channel.add(ch);
                }
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            channel = null;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("頻道清單讀取失敗，要重試嗎？");
            builder.setMessage(e.toString());
            builder.setPositiveButton("確定", (dialog, id) -> {
                readChannelList();
                if(channel == null){
                    return;
                }
                if(channelNum >= channel.size()){
                    resetChannelNum();
                }
                play(channelNum);
            });
            builder.setNeutralButton("進入設定", (dialogInterface, i) -> {
                showSettings(findViewById(R.id.container));
            });
            builder.setNegativeButton("退出", (dialog, id) -> finish());
            builder.setOnCancelListener(dialog -> finish());
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }

    void readChannelListFromStream(InputStream inputStream) throws JSONException, IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        for (String line; (line = reader.readLine()) != null; ) {
            stringBuilder.append(line).append('\n');
        }
        JSONObject json = new JSONObject(stringBuilder.toString());
        JSONArray channelList = json.getJSONArray("channelList");

        for(int i = 0; i < channelList.length(); i++){
            JSONObject channelObject = channelList.getJSONObject(i);
            if(channelObject.has("list")){
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
                URL url = new URL(channelObject.getString("list"));
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);
                urlConnection.setDoOutput(true);
                urlConnection.connect();
                readChannelListFromStream(url.openStream());
                continue;
            }
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
            String header = "";
            if (!channelObject.isNull("header")) {
                header = channelObject.getString("header");
            }
            Map<String, String> ytdlOptions = new HashMap<>();
            if (!channelObject.isNull("ytdl-options")) {
                JSONObject options = channelObject.getJSONObject("ytdl-options");
                for(Iterator<String> iter = options.keys(); iter.hasNext();) {
                    String key = iter.next();
                    String value = options.getString(key);
                    ytdlOptions.put(key, value);
                }
            }
            Channel ch = new Channel(url, name, format, volume, header, ytdlOptions);
            channel.add(ch);
        }
    }

    void toPrevChannel(){
        do {
            if (channelNum == 0) {
                channelNum = channel.size() - 1;
            } else {
                channelNum -= 1;
            }
        } while (channel.get(channelNum).isHidden());
        switchChannel(channelNum);
    }

    void toNextChannel(){
        do {
            if (channelNum == channel.size() - 1) {
                channelNum = 0;
            } else {
                channelNum += 1;
            }
        } while (channel.get(channelNum).isHidden());
        switchChannel(channelNum);
    }

    void switchChannel(int num)
    {
        player.stop();
        errorMessageView.setText("");
        channelNum = num;
        play(num);
    }

    @OptIn(markerClass = UnstableApi.class)
    void play(int num)
    {
        textInfo.setText(String.format("正在載入 %d %s", num, channel.get(num).getName()));
        new Thread(() -> {
            if(channel.get(num).needParse() == Channel.NEEDPARSE_YES) {
                try {
                    channel.get(num).parse(ytdlp);
                } catch (JSONException | IOException | InterruptedException | PyException |
                         InvalidAlgorithmParameterException | IllegalBlockSizeException |
                         NoSuchPaddingException | BadPaddingException | NoSuchAlgorithmException |
                         InvalidKeyException e) {
                    if(channelNum!=num) return;
                    Log.e(TAG, Log.getStackTraceString(e));
                    showErrorMessage(e.toString());
                }
            }
            if(channelNum!=num) return;

            DataSource.Factory factory = new DefaultHttpDataSource.Factory()
                    .setDefaultRequestProperties(channel.get(num).getHeaderMap());
            MediaSource mediaSource;
            String url = channel.get(num).getVideo();
            int split = url.indexOf('\n');
            if(split == -1){
                MediaItem mediaItem = MediaItem.fromUri(url);
                mediaSource = new DefaultMediaSourceFactory(factory)
                        .createMediaSource(mediaItem);
            }
            else{
                String firstUrl = url.substring(0, split);
                MediaItem firstMediaItem = MediaItem.fromUri(firstUrl);
                MediaSource firstMediaSource = new DefaultMediaSourceFactory(factory)
                        .createMediaSource(firstMediaItem);
                String secondUrl = url.substring(split + 1);
                MediaItem secondMediaItem = MediaItem.fromUri(secondUrl);
                MediaSource secondMediaSource = new DefaultMediaSourceFactory(factory)
                        .createMediaSource(secondMediaItem);
                mediaSource = new MergingMediaSource(firstMediaSource, secondMediaSource);
            }

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
                switchChannel(data.getIntExtra("channelNum", 0));
            }
            else{
                play(channelNum);
            }
        }
        else if(requestCode == 1){
            // ChannelInfoActivity - existing channel
            if (resultCode == Activity.RESULT_OK) {
                if(data.getBooleanExtra("delete", false)){
                    channel.remove(channelNum);
                    resetChannelNum();
                    play(channelNum);
                    saveSettings();
                    return;
                }

                if(data.getBooleanExtra("remove_cache", false)){
                    channel.get(channelNum).setVideo("");
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
                play(channelNum);
                saveSettings();
            }
        }
        else if (requestCode == 2) {
            // SettingsActivity
            if (resultCode == Activity.RESULT_OK) {
                defaultFormat = data.getStringExtra("defaultFormat");
                defaultVolume = data.getStringExtra("defaultVolume");
                isShowErrorMessage = data.getBooleanExtra("isShowErrorMessage", false);
                enableBackgroundExtract = data.getBooleanExtra("enableBackgroundExtract", false);
                invertChannelButtons = data.getBooleanExtra("invertChannelButtons", false);
                hideNavigationBar = data.getBooleanExtra("hideNavigationBar", false);
                hideStatusBar = data.getBooleanExtra("hideStatusBar", false);
                readChannelList();
                if(data.getBooleanExtra("remove_cache", false)){
                    for(Channel i: channel) i.setVideo("");
                }
                if(channel == null){
                    return;
                }
                if(channelNum >= channel.size()){
                    resetChannelNum();
                }
                play(channelNum);
            }
        }
        else if(requestCode == 3){
            // ChannelInfoActivity - new channel
            if (resultCode == Activity.RESULT_OK) {
                if(data.getBooleanExtra("delete", false)){
                    return;
                }
                Channel ch = new Channel("", "", defaultFormat, Float.parseFloat(defaultVolume), "", new HashMap<>());
                ch.setName(data.getStringExtra("customName"));
                ch.setHidden(data.getBooleanExtra("isHidden", false));
                ch.setUrl(data.getStringExtra("customUrl"));
                ch.setFormat(data.getStringExtra("customFormat"));
                ch.setVolume(data.getStringExtra("customVolume"));
                ch.setHeader(data.getStringExtra("customHeader"));
                channel.add(ch);
                saveSettings();
                switchChannel(channel.size()-1);
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
                        switchChannel(Integer.parseInt(input));
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
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_CHANNEL_DOWN:
                    if (invertChannelButtons) {
                        toNextChannel();
                    }
                    else{
                        toPrevChannel();
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_CHANNEL_UP:
                    if (invertChannelButtons) {
                        toPrevChannel();
                    }
                    else{
                        toNextChannel();
                    }
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
        if(event.getAction() != MotionEvent.ACTION_DOWN){
            return super.onTouchEvent(event);
        }
        getSupportFragmentManager().popBackStack();
        if((int)event.getX() < playerView.getWidth() / 3){
            if (invertChannelButtons) {
                toNextChannel();
            }
            else{
                toPrevChannel();
            }
        }
        else if((int)event.getX() < playerView.getWidth() / 3 * 2){
            showMenu();
        }
        else{
            if (invertChannelButtons) {
                toPrevChannel();
            }
            else{
                toNextChannel();
            }
        }
        return true;
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
        intent.putExtra("currentNum", channelNum);
        startActivityForResult(intent, 0);
        getSupportFragmentManager().popBackStack();
        DO_NOT_PLAY_ON_START = true;
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
        DO_NOT_PLAY_ON_START = true;
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
        DO_NOT_PLAY_ON_START = true;
    }

    public void showSettings(View view){
        Intent intentSettings = new Intent(this, SettingsActivity.class);
        intentSettings.putExtra("defaultFormat", defaultFormat);
        intentSettings.putExtra("defaultVolume", defaultVolume);
        intentSettings.putExtra("isShowErrorMessage", isShowErrorMessage);
        intentSettings.putExtra("enableBackgroundExtract", enableBackgroundExtract);
        intentSettings.putExtra("invertChannelButtons", invertChannelButtons);
        intentSettings.putExtra("hideNavigationBar", hideNavigationBar);
        intentSettings.putExtra("hideStatusBar", hideStatusBar);
        startActivityForResult(intentSettings, 2);
        getSupportFragmentManager().popBackStack();
        DO_NOT_PLAY_ON_START = true;
    }

    public void showChannelNumEdit(View view){
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("輸入頻道號碼")
                .setView(editText)
                .setPositiveButton("確定", (dialog, id) -> {
                    if(Integer.parseInt(editText.getText().toString()) < channel.size()){
                        switchChannel(Integer.parseInt(editText.getText().toString()));
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
        editor.putBoolean("isShowErrorMessage", isShowErrorMessage);
        editor.putBoolean("enableBackgroundExtract", enableBackgroundExtract);
        editor.putBoolean("invertChannelButtons", invertChannelButtons);
        editor.putBoolean("hideNavigationBar", hideNavigationBar);
        editor.putBoolean("hideStatusBar", hideStatusBar);
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
            String json_string = jsonObject.toString();
            File file = new File(getExternalFilesDir(null), "custom.txt");
            try (FileOutputStream stream = new FileOutputStream(file)) {
                stream.write(json_string.getBytes());
            }
        } catch (JSONException | IOException e) {
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
        if(!isShowErrorMessage) return;
        runOnUiThread(() -> {
            if(!player.isPlaying()) {
                errorMessageView.setText(message);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(player == null) return;
        player.release();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isStarted = false;
        if(player == null) return;
        player.stop();
        saveSettings();
    }
}
