package io.github.anenasa.news;

public class Channel {
    final int index;
    final String defaultUrl;
    final String defaultName;
    final String defaultFormat;
    final float defaultVolume;
    String video = "";
    String customUrl = "";
    String customName = "";
    String customFormat = "";
    String customVolume = "";
    boolean hidden = false;

    public Channel(int index, String url, String name, String format, float volume) {
        this.index = index;
        this.defaultUrl = url;
        this.defaultName = name;
        this.defaultFormat = format;
        this.defaultVolume = volume;
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

    public void setHidden(boolean hidden){
        this.hidden = hidden;
    }
}
