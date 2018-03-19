package Tweet;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class MyTweet {
    private String created_at;//
    private long id;//
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String id_str;
    private String text;//
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String source;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private boolean truncated;
    private long in_reply_to_status_id;//
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String in_reply_to_status_id_str;
    private long in_reply_to_user_id;//
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String in_reply_to_user_id_str;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String in_reply_to_screen_name;
    private User user;//
    private Coordinates coordinates;//
    private Places place;//
    @JsonIgnoreProperties(ignoreUnknown = true)
    private long quoted_status_id;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String quoted_status_id_str;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private boolean is_quote_status;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private MyTweet quoted_status;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private MyTweet retweeted_status;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private int quote_count;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private int reply_count;
    private int retweet_count;//
    private int favorite_count;//
    private Entities entities;//
    @JsonIgnoreProperties(ignoreUnknown = true)
    private Entities extended_entities;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private boolean favorited;
    private boolean retweeted;//
    @JsonIgnoreProperties(ignoreUnknown = true)
    private boolean possibly_sensitive;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String filter_level;
    private String lang;//
    @JsonIgnoreProperties(ignoreUnknown = true)
    private Object matching_rules;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private long[] contributorsIDs;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private Object current_user_retweet;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private Object scopes;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private boolean withheld_copyright;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String[] withheld_in_countries;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String withheld_scope;
    private Object geo;//
    @JsonIgnoreProperties(ignoreUnknown = true)
    private int[] display_text_range;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String contributors;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String timestamp_ms;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String extended_tweet;
    //


    public String getExtended_tweet() {
        return extended_tweet;
    }

    public void setExtended_tweet(String extended_tweet) {
        this.extended_tweet = extended_tweet;
    }

    public String getTimestamp_ms() {
        return timestamp_ms;
    }

    public void setTimestamp_ms(String timestamp_ms) {
        this.timestamp_ms = timestamp_ms;
    }

    public String getContributors() {
        return contributors;
    }

    public void setContributors(String contributors) {
        this.contributors = contributors;
    }

    public int[] getDisplay_text_range() {
        return display_text_range;
    }

    public void setDisplay_text_range(int[] display_text_range) {
        this.display_text_range = display_text_range;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public long getIn_reply_to_status_id() {
        return in_reply_to_status_id;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public long getIn_reply_to_user_id() {
        return in_reply_to_user_id;
    }

    public String getCreated_at() {
        return created_at;
    }

    public String getId_str() {
        return id_str;
    }

    public String getIn_reply_to_screen_name() {
        return in_reply_to_screen_name;
    }

    public String getIn_reply_to_status_id_str() {
        return in_reply_to_status_id_str;
    }

    public String getSource() {
        return source;
    }

    public String getIn_reply_to_user_id_str() {
        return in_reply_to_user_id_str;
    }

    public User getUser() {
        return user;
    }

    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public long getQuoted_status_id() {
        return quoted_status_id;
    }

    public Places getPlace() {
        return place;
    }

    public void setId_str(String id_str) {
        this.id_str = id_str;
    }

    public boolean isIs_quote_status() {
        return is_quote_status;
    }

    public void setIn_reply_to_screen_name(String in_reply_to_screen_name) {
        this.in_reply_to_screen_name = in_reply_to_screen_name;
    }

    public void setIn_reply_to_status_id(long in_reply_to_status_id) {
        this.in_reply_to_status_id = in_reply_to_status_id;
    }

    public void setIn_reply_to_status_id_str(String in_reply_to_status_id_str) {
        this.in_reply_to_status_id_str = in_reply_to_status_id_str;
    }

    public void setIn_reply_to_user_id(long in_reply_to_user_id) {
        this.in_reply_to_user_id = in_reply_to_user_id;
    }

    public void setIn_reply_to_user_id_str(String in_reply_to_user_id_str) {
        this.in_reply_to_user_id_str = in_reply_to_user_id_str;
    }

    public String getQuoted_status_id_str() {
        return quoted_status_id_str;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    public void setPlace(Places place) {
        this.place = place;
    }

    public boolean isFavorited() {
        return favorited;
    }

    public void setIs_quote_status(boolean is_quote_status) {
        this.is_quote_status = is_quote_status;
    }

    public void setQuoted_status_id(long quoted_status_id) {
        this.quoted_status_id = quoted_status_id;
    }

    public boolean isPossibly_sensitive() {
        return possibly_sensitive;
    }

    @Override
    public String toString() {
        return getId()+":"+getText();
    }

    public boolean isRetweeted() {
        return retweeted;
    }

    public boolean isWithheld_copyright() {
        return withheld_copyright;
    }

    public Entities getEntities() {
        return entities;
    }

    public void setQuoted_status_id_str(String quoted_status_id_str) {
        this.quoted_status_id_str = quoted_status_id_str;
    }

    public Entities getExtended_entities() {
        return extended_entities;
    }

    public int getFavorite_count() {
        return favorite_count;
    }

    public int getQuote_count() {
        return quote_count;
    }

    public int getReply_count() {
        return reply_count;
    }

    public int getRetweet_count() {
        return retweet_count;
    }

    public MyTweet getQuoted_status() {
        return quoted_status;
    }

    public long[] getContributorsIDs() {
        return contributorsIDs;
    }

    public MyTweet getRetweeted_status() {
        return retweeted_status;
    }

    public Object getCurrent_user_retweet() {
        return current_user_retweet;
    }

    public void setQuote_count(int quote_count) {
        this.quote_count = quote_count;
    }

    public Object getGeo() {
        return geo;
    }

    public Object getMatching_rules() {
        return matching_rules;
    }

    public void setQuoted_status(MyTweet quoted_status) {
        this.quoted_status = quoted_status;
    }

    public void setFavorite_count(int favorite_count) {
        this.favorite_count = favorite_count;
    }

    public Object getScopes() {
        return scopes;
    }

    public String getFilter_level() {
        return filter_level;
    }

    public String getLang() {
        return lang;
    }

    public String getWithheld_scope() {
        return withheld_scope;
    }

    public void setRetweeted_status(MyTweet retweeted_status) {
        this.retweeted_status = retweeted_status;
    }

    public String[] getWithheld_in_countries() {
        return withheld_in_countries;
    }

    public void setEntities(Entities entities) {
        this.entities = entities;
    }

    public void setContributorsIDs(long[] contributorsIDs) {
        this.contributorsIDs = contributorsIDs;
    }

    public void setCurrent_user_retweet(Object current_user_retweet) {
        this.current_user_retweet = current_user_retweet;
    }

    public void setExtended_entities(Entities extended_entities) {
        this.extended_entities = extended_entities;
    }

    public void setFavorited(boolean favorited) {
        this.favorited = favorited;
    }

    public void setFilter_level(String filter_level) {
        this.filter_level = filter_level;
    }

    public void setReply_count(int reply_count) {
        this.reply_count = reply_count;
    }

    public void setPossibly_sensitive(boolean possibly_sensitive) {
        this.possibly_sensitive = possibly_sensitive;
    }

    public void setGeo(Object geo) {
        this.geo = geo;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public void setMatching_rules(Object matching_rules) {
        this.matching_rules = matching_rules;
    }

    public void setRetweet_count(int retweet_count) {
        this.retweet_count = retweet_count;
    }

    public void setRetweeted(boolean retweeted) {
        this.retweeted = retweeted;
    }

    public void setScopes(Object scopes) {
        this.scopes = scopes;
    }

    public void setWithheld_copyright(boolean withheld_copyright) {
        this.withheld_copyright = withheld_copyright;
    }

    public void setWithheld_in_countries(String[] withheld_in_countries) {
        this.withheld_in_countries = withheld_in_countries;
    }

    public void setWithheld_scope(String withheld_scope) {
        this.withheld_scope = withheld_scope;
    }
}