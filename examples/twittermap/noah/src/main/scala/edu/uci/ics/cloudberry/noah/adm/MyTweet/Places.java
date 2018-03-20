package edu.uci.ics.cloudberry.noah.adm.MyTweet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class Places {
    private String id;//
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String url;
    private String place_type;//
    private String name;//
    private String full_name;//
    private String country_code;//
    private String country;//
    private BoundingBox bounding_box;//
    @JsonIgnoreProperties(ignoreUnknown = true)
    private Object attributes;

    //

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getPlace_type() {
        return place_type;
    }

    public String getName() {
        return name;
    }

    public String getFull_name() {
        return full_name;
    }

    public String getCountry_code() {
        return country_code;
    }

    public String getCountry() {
        return country;
    }

    public BoundingBox getBounding_box() {
        return bounding_box;
    }

    public Object getAttributes() {
        return attributes;
    }

    public void setAttributes(Object attributes) {
        this.attributes = attributes;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setFull_name(String full_name) {
        this.full_name = full_name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPlace_type(String place_type) {
        this.place_type = place_type;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setBounding_box(BoundingBox bounding_box) {
        this.bounding_box = bounding_box;
    }

    public void setCountry_code(String country_code) {
        this.country_code = country_code;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("{\"country\":\"%s\",\"country_code\":\"%s\",\"full_name\":\"%s\",\"id\":\"%s\",\"name\":\"%s\",\"place_type\":\"%s\",\"bounding_box\":%s", getCountry(), getCountry_code(), getFull_name(), getId(), getName(), getPlace_type(), getBounding_box().toString());
    }
}
