package io.github.anenasa.news;

public class Channel {
    public int index;
    public String url;
    public String name;
    public String format;
    public float volume;
    public String video = "";

    public Channel(int index, String url, String name, String format, float volume) {
        this.index = index;
        this.url = url;
        this.name = name;
        this.format = format;
        this.volume = volume;
    }


    public void setVideo(String video) {
        this.video = video;
    }
}
