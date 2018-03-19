package edu.uci.ics.cloudberry.noah.adm.MyTweet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class UserMention {
    private long id;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String id_str;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private int[] indices;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String name;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String screen_name;
    //

    @Override
    public String toString() {
        return super.toString();
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setScreen_name(String screen_name) {
        this.screen_name = screen_name;
    }

    public String getScreen_name() {
        return screen_name;
    }

    public String getId_str() {
        return id_str;
    }

    public void setId_str(String id_str) {
        this.id_str = id_str;
    }

    public void setIndices(int[] indices) {
        this.indices = indices;
    }

    public long getId() {
        return id;
    }

    public int[] getIndices() {
        return indices;
    }
}
