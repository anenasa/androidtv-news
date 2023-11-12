package io.github.anenasa.news;

import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
    final Map<String, String> ytdlOptions;
    String video = "";
    String customUrl = "";
    String customName = "";
    String customFormat = "";
    String customVolume = "";
    String customHeader = "";
    boolean hidden = false;

    public Channel(String url, String name, String format, float volume, String header, Map<String, String> ytdlOptions) {
        this.defaultUrl = url;
        this.defaultName = name;
        this.defaultFormat = format;
        this.defaultVolume = volume;
        this.defaultHeader = header;
        this.ytdlOptions = ytdlOptions;
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

    public Map<String, String> getHeaderMap(){
        String[] headers = getHeader().split("\\\\r\\\\n");
        Map<String, String> map = new HashMap<>();
        for (String header : headers) {
            int pos = header.indexOf(":");
            if(pos != -1){
                map.put(header.substring(0, pos), header.substring(pos + 1));
            }
        }
        return map;
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
        String url = getUrl();
        if(getVideo().isEmpty() || url.startsWith("https://today.line.me")) {
            return NEEDPARSE_YES;
        }
        if(url.endsWith("m3u8")){
            return NEEDPARSE_NO;
        }
        if(url.startsWith("https://www.youtube.com/")){
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
        if(url.startsWith("https://hamivideo.hinet.net/") || url.startsWith("https://embed.4gtv.tv/") || url.startsWith("https://www.ftvnews.com.tw/live/live-video/1/")){
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

    void parse(YtDlp ytdlp) throws JSONException, IOException, InterruptedException, PyException {
        PyObject option = Python.getInstance().getBuiltins().callAttr("dict");
        option.callAttr("__setitem__", "format", getFormat());
        if(!getHeader().isEmpty()) {
            PyObject header_dict = Python.getInstance().getBuiltins().callAttr("dict");
            for(Map.Entry<String, String> entry : getHeaderMap().entrySet()){
                header_dict.callAttr("__setitem__", entry.getKey(), entry.getValue());
            }
            option.callAttr("__setitem__", "http_headers", header_dict);
        }
        for (Map.Entry<String, String> entry : this.ytdlOptions.entrySet()) {
            option.callAttr("__setitem__", entry.getKey(), entry.getValue());
        }
        setVideo(ytdlp.extract(getUrl(), option));
    }
}
