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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
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
        if(url.startsWith("https://hamivideo.hinet.net/") || url.startsWith("https://embed.4gtv.tv/") ||
                url.startsWith("https://www.ftvnews.com.tw/live/live-video/1/")){
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

    void parse(YtDlp ytdlp) throws JSONException, IOException, InterruptedException, PyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        String url = getUrl();
        if(url.startsWith("https://hamivideo.hinet.net/") && url.endsWith(".do")){
            String id = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("."));
            OkHttpClient okHttpClient = new OkHttpClient();
            Request.Builder okHttpRequestBuilder = new Request.Builder()
                    .url("https://hamivideo.hinet.net/api/play.do?freeProduct=1&id=" + id);
            for(Map.Entry<String, String> entry : getHeaderMap().entrySet()){
                okHttpRequestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
            Request okHttpRequest = okHttpRequestBuilder.build();
            Response response = okHttpClient.newCall(okHttpRequest).execute();
            ResponseBody body = response.body();
            if (body == null) throw new IOException("body is null");
            JSONObject object = new JSONObject(body.string());
            url = object.getString("url");
        }
        else if(url.equals("https://news.ebc.net.tw/live")){
            Document doc = Jsoup.connect(url).get();
            Element el = doc.selectFirst("div#live-slider div.live-else-little-box");
            if(el == null) throw new IOException("找不到 div");
            url = el.attr("data-code");
        }
        else if(url.startsWith("https://today.line.me/tw/v2/article/")){
            Document doc = Jsoup.connect(url).get();
            Element el =doc.selectFirst("script:containsData(__NUXT__)");
            if(el == null) throw new IOException("找不到 script");
            String script = el.data();
            String id = script.substring(script.indexOf("broadcastId:")+13);
            id = id.substring(0,id.indexOf("\""));
            OkHttpClient okHttpClient = new OkHttpClient();
            Request okHttpRequest = new Request.Builder()
                    .url("https://today.line.me/webapi/glplive/broadcasts/" + id)
                    .build();
            Response response = okHttpClient.newCall(okHttpRequest).execute();
            ResponseBody body = response.body();
            if (body == null) throw new IOException("body is null");
            JSONObject object = new JSONObject(body.string());
            url = object.getJSONObject("hlsUrls").getString("abr");
        }
        else if(url.startsWith("https://embed.4gtv.tv/") || url.startsWith("https://www.ftvnews.com.tw/live/live-video/1/")){
            String id;
            if(url.startsWith("https://www.ftvnews.com.tw/live/live-video/1/")){
                id = url.substring(url.lastIndexOf("/") + 1);
            }
            else{
                Document doc = Jsoup.connect(url).get();
                Element el = doc.selectFirst("script:containsData(ChannelId)");
                if(el == null) throw new IOException("找不到 script");
                String script = el.data();
                id = script.substring(script.indexOf("ChannelId") + 12);
                id = id.substring(0, id.indexOf("\""));
            }
            OkHttpClient okHttpClient = new OkHttpClient();
            Request okHttpRequest = new Request.Builder()
                    .url("https://app.4gtv.tv/Data/GetChannelURL_Mozai.ashx?callback=channelname&Type=LIVE&ChannelId=" + id)
                    .build();
            Response response = okHttpClient.newCall(okHttpRequest).execute();
            ResponseBody body = response.body();
            if (body == null) throw new IOException("body is null");
            String bodyString = body.string();
            String videoUrl = bodyString.substring(bodyString.indexOf("VideoURL") + 11);
            url = videoUrl.substring(0, videoUrl.indexOf("\""));
        }
        else if(url.startsWith("https://www.litv.tv/channel/watch.do")){
            String id = url.substring(url.indexOf("content_id") + 11);
            if(id.contains("&")) id = id.substring(0, id.indexOf("&"));

            OkHttpClient okHttpClient = new OkHttpClient();
            String data = String.format("{\"type\":\"auth\",\"contentId\":\"%s\",\"contentType\":\"channel\"}", id);
            RequestBody requestBody = RequestBody.create(data, MediaType.parse("application/json"));
            Request.Builder okHttpRequestBuilder = new Request.Builder()
                    .url("https://www.litv.tv/channel/ajax/getUrl")
                    .post(requestBody);
            for(Map.Entry<String, String> entry : getHeaderMap().entrySet()){
                okHttpRequestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
            Request okHttpRequest = okHttpRequestBuilder.build();
            Response response = okHttpClient.newCall(okHttpRequest).execute();
            ResponseBody body = response.body();
            if (body == null) throw new IOException("body is null");
            JSONObject object = new JSONObject(body.string());
            url = object.getString("fullpath");
        }
        else if(url.startsWith("https://www.ofiii.com/channel/watch/")){
            OkHttpClient okHttpClient = new OkHttpClient();
            // Get device id
            Request deviceidRequest = new Request.Builder().url("https://www.ofiii.com/api/deviceId").build();
            Response deviceidResponse = okHttpClient.newCall(deviceidRequest).execute();
            ResponseBody deviceidBody = deviceidResponse.body();
            if (deviceidBody == null) throw new IOException("body is null");
            String deviceid = deviceidBody.string().replace("\"","");

            String id = url.substring(url.lastIndexOf("/") + 1);
            Request.Builder okHttpRequestBuilder = new Request.Builder()
                    .url(String.format("https://cdi.ofiii.com/ofiii_cdi/video/urls?device_type=pc&device_id=%s&media_type=channel&asset_id=%s&project_num=OFWEB00", deviceid, id));
            for(Map.Entry<String, String> entry : getHeaderMap().entrySet()){
                okHttpRequestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
            Request okHttpRequest = okHttpRequestBuilder.build();
            Response response = okHttpClient.newCall(okHttpRequest).execute();
            ResponseBody body = response.body();
            if (body == null) throw new IOException("body is null");
            JSONObject object = new JSONObject(body.string());
            url = object.getJSONArray("asset_urls").getString(0);
        }

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
        setVideo(ytdlp.extract(url, option));
    }
}
