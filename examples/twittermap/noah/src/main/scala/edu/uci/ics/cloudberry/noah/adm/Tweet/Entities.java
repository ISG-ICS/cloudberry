package Tweet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class Entities {
    private Hashtag[] hashtags;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private Object media;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private Object urls;
    private Object user_mentions;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private Object symbols;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private Object polls;
    //

    @Override
    public String toString() {
        return hashtags.toString();
    }

    public Hashtag[] getHashtags() {
        return hashtags;
    }

    public Object getMedia() {
        return media;
    }

    public Object getPolls() {
        return polls;
    }

    public Object getSymbols() {
        return symbols;
    }

    public Object getUrls() {
        return urls;
    }

    public Object getUser_mentions() {
        return user_mentions;
    }

    public void setHashtags(Hashtag[] hashtags) {
        this.hashtags = hashtags;
    }

    public void setMedia(Object media) {
        this.media = media;
    }

    public void setPolls(Object polls) {
        this.polls = polls;
    }

    public void setSymbols(Object symbols) {
        this.symbols = symbols;
    }

    public void setUrls(Object urls) {
        this.urls = urls;
    }

    public void setUser_mentions(Object user_mentions) {
        this.user_mentions = user_mentions;
    }
}
