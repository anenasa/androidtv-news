package io.github.anenasa.news;

import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class Channel {
    public static final int NEEDPARSE_NO = 0;
    public static final int NEEDPARSE_YES = 1;
    public static final int NEEDPARSE_UNKNOWN = 2;

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

    public Channel(String url, String name, String format, float volume, String header) {
        this.defaultUrl = url;
        this.defaultName = name;
        this.defaultFormat = format;
        this.defaultVolume = volume;
        this.defaultHeader = header;
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

    int needParse(){
        if(getVideo().isEmpty()) {
            return NEEDPARSE_YES;
        }
        if(getUrl().endsWith("m3u8")){
            return NEEDPARSE_NO;
        }
        if(getUrl().startsWith("https://www.youtube.com/")){
            long current = System.currentTimeMillis() / 1000;
            int pos = getVideo().indexOf("expire") + 7;
            long expire = Long.parseLong(getVideo().substring(pos, pos + 10));
            if(current < expire){
                return NEEDPARSE_NO;
            }
            else{
                return NEEDPARSE_YES;
            }
        }
        if(getUrl().startsWith("https://hamivideo.hinet.net/channel/")){
            long current = System.currentTimeMillis() / 1000;
            int pos = getVideo().indexOf("expires") + 8;
            long expire = Long.parseLong(getVideo().substring(pos, pos + 10));
            if(current < expire){
                return NEEDPARSE_NO;
            }
            else{
                return NEEDPARSE_YES;
            }
        }
        return NEEDPARSE_UNKNOWN;
    }

    void parse() throws JSONException, IOException, YoutubeDLException, InterruptedException {
        String url = getUrl();
        YoutubeDLRequest request;
        if(url.startsWith("https://hamivideo.hinet.net/channel/") && url.endsWith(".do")){
            String id = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("."));
            OkHttpClient okHttpClient = new OkHttpClient();
            Request okHttpRequest = new Request.Builder()
                    .url("https://hamivideo.hinet.net/api/play.do?freeProduct=1&id=" + id)
                    .build();
            Response response = okHttpClient.newCall(okHttpRequest).execute();
            ResponseBody body = response.body();
            if (body == null) throw new IOException("body is null");
            JSONObject object = new JSONObject(body.string());
            request = new YoutubeDLRequest(object.getString("url"));
        }
        else if(url.equals("https://news.ebc.net.tw/live")){
            Document doc = Jsoup.connect(url).get();
            Element el = doc.selectFirst("div#live-slider div.live-else-little-box");
            if(el == null) throw new IOException("找不到 div");
            String src = el.attr("data-code");
            request = new YoutubeDLRequest(src);

        }
        else{
            request = new YoutubeDLRequest(url);
        }
        request.addOption("-f", getFormat());
        if(!getHeader().isEmpty()) {
            String[] headers = getHeader().split("\\\\r\\\\n");
            for (String header : headers) {
                request.addOption("--add-header", header);
            }

        }
        VideoInfo streamInfo = YoutubeDL.getInstance().getInfo(request);
        setVideo(streamInfo.getUrl());
    }
}
