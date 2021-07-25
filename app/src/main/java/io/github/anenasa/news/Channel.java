package io.github.anenasa.news;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Channel {
    final int index;
    final String defaultUrl;
    final String defaultName;
    final String defaultFormat;
    final float defaultVolume;
    final String defaultHeader;
    String video = "";
    String customUrl = "";
    String customName = "";
    String customFormat = "";
    String customVolume = "";
    String customHeader = "";
    boolean hidden = false;

    public Channel(int index, String url, String name, String format, float volume, String header) {
        this.index = index;
        this.defaultUrl = url;
        this.defaultName = name;
        this.defaultFormat = format;
        this.defaultVolume = volume;
        this.defaultHeader = header;
    }

    public int getIndex(){
        return index;
    }

    public String getUrl(){
        if(customUrl.isEmpty()){
            return defaultUrl;
        }
        else{
            return customUrl;
        }
    }

    public String getName(){
        if(customName.isEmpty()) {
            return defaultName;
        }
        else{
            return customName;
        }
    }

    public String getFormat(){
        if(customFormat.isEmpty()) {
            return defaultFormat;
        }
        else{
            return customFormat;
        }
    }

    public float getVolume(){
        if(customVolume.isEmpty()) {
            return defaultVolume;
        }
        else{
            return Float.parseFloat(customVolume);
        }
    }

    public String getHeader(){
        if(customHeader.isEmpty()) {
            return defaultHeader;
        }
        else{
            return customHeader;
        }
    }

    public String getVideo(){
        return video;
    }

    public boolean isHidden(){
        return hidden;
    }

    public void setVideo(String video) {
        this.video = video;
    }

    public void setUrl(String url){
        customUrl = url;
    }

    public void setName(String name){
        customName = name;
    }

    public void setFormat(String format){
        customFormat = format;
    }

    public void setVolume(String volume){
        customVolume = volume;
    }

    public void setHeader(String header){
        customHeader = header;
    }

    public void setHidden(boolean hidden){
        this.hidden = hidden;
    }

    void parse() throws JSONException, IOException, YoutubeDLException, InterruptedException {
        String url = getUrl();
        YoutubeDLRequest request;
        if(url.startsWith("https://hamivideo.hinet.net/channel/")){
            String id = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("."));
            OkHttpClient okHttpClient = new OkHttpClient();
            okHttpClient.setConnectTimeout(30, TimeUnit.SECONDS);
            okHttpClient.setReadTimeout(30, TimeUnit.SECONDS);
            Request okHttpRequest = new Request.Builder()
                    .url("https://hamivideo.hinet.net/api/play.do?freeProduct=1&id=" + id)
                    .build();
            Response response = okHttpClient.newCall(okHttpRequest).execute();
            JSONObject object = new JSONObject(response.body().string());
            request = new YoutubeDLRequest(object.getString("url"));
        }
        else{
            request = new YoutubeDLRequest(url);
        }
        request.addOption("-f", getFormat());
        if(!getHeader().isEmpty()) {
            request.addOption("--add-header", getHeader());
        }
        VideoInfo streamInfo = YoutubeDL.getInstance().getInfo(request);
        setVideo(streamInfo.getUrl());
    }
}
