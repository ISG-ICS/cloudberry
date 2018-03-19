package edu.uci.ics.cloudberry.noah.adm.MyTweet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class Hashtag {
    @JsonIgnoreProperties(ignoreUnknown = true)
    private int[] indices;
    private String text;
    //

    public int[] getIndices() {
        return indices;
    }

    public String getText() {
        return text;
    }

    public void setIndices(int[] indices) {
        this.indices = indices;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text+":"+indices.toString();
    }
}
