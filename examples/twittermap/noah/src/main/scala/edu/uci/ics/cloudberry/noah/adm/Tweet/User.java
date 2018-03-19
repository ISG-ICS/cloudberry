package Tweet;
import java.util.Date;
import java.util.Map;
import com.fasterxml.jackson.annotation.*;
public class User {
    private long id;//
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String id_str;
    private String name;//
    private String screen_name;//
    private String location;//
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String url;
    private String description;//
    @JsonIgnoreProperties(ignoreUnknown = true)
    private Map<String,Object> derived;//enrichment field
    @JsonProperty("protected")@JsonIgnoreProperties(ignoreUnknown = true)
    private boolean protected_;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private boolean verified;
    private int followers_count;//
    private int friends_count;//
    @JsonIgnoreProperties(ignoreUnknown = true)
    private int listed_count;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private int favourites_count;
    private int statuses_count;//
    private String created_at;//
    @JsonIgnoreProperties(ignoreUnknown = true)
    private int utc_offset;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String time_zone;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private boolean geo_enabled;
    private String lang;//
    @JsonIgnoreProperties(ignoreUnknown = true)
    private boolean contributeors_enabled;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private boolean is_translator;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String profile_background_color;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String profile_background_image_url;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String profile_background_image_url_https;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private boolean profile_background_tile;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String profile_link_color;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String profile_sidebar_border_color;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String profile_sidebar_fill_color;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String profile_text_color;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private boolean profile_use_background_image;
    private String profile_image_url;//
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String profile_image_url_https;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String profile_banner_url;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private boolean default_profile;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private boolean default_profile_image;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private boolean following;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private boolean follow_request_sent;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private boolean notifications;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String withheld_in_countries;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String withheld_scope;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private boolean contributors_enabled;
    //

    public boolean isContributors_enabled() {
        return contributors_enabled;
    }

    public void setContributors_enabled(boolean contributors_enabled) {
        this.contributors_enabled = contributors_enabled;
    }

    public void setId_str(String id_str) {
        this.id_str = id_str;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getId_str() {
        return id_str;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCreated_at() {
        return created_at;
    }

    public boolean isProtected_() {
        return protected_;
    }

    public boolean isVerified() {
        return verified;
    }

    public int getFollowers_count() {
        return followers_count;
    }

    public int getFavourites_count() {
        return favourites_count;
    }

    public int getFriends_count() {
        return friends_count;
    }

    public int getListed_count() {
        return listed_count;
    }

    public Map<String, Object> getDerived() {
        return derived;
    }

    public int getStatuses_count() {
        return statuses_count;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public String getScreen_name() {
        return screen_name;
    }

    public String getUrl() {
        return url;
    }

    public void setDerived(Map<String, Object> derived) {
        this.derived = derived;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setFollowers_count(int followers_count) {
        this.followers_count = followers_count;
    }

    public void setScreen_name(String screen_name) {
        this.screen_name = screen_name;
    }

    public boolean isGeo_enabled() {
        return geo_enabled;
    }

    public void setFriends_count(int friends_count) {
        this.friends_count = friends_count;
    }

    public void setFavourites_count(int favourites_count) {
        this.favourites_count = favourites_count;
    }

    public int getUtc_offset() {
        return utc_offset;
    }

    public void setListed_count(int listed_count) {
        this.listed_count = listed_count;
    }

    public void setProtected_(boolean protected_) {
        this.protected_ = protected_;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public void setStatuses_count(int statuses_count) {
        this.statuses_count = statuses_count;
    }

    public void setUtc_offset(int utc_offset) {
        this.utc_offset = utc_offset;
    }

    @Override
    public String toString() {
        return getId()+":"+getName();
    }

    public void setWithheld_scope(String withheld_scope) {
        this.withheld_scope = withheld_scope;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getLang() {
        return lang;
    }

    public String getTime_zone() {
        return time_zone;
    }

    public boolean isContributeors_enabled() {
        return contributeors_enabled;
    }

    public void setWithheld_in_countries(String withheld_in_countries) {
        this.withheld_in_countries = withheld_in_countries;
    }

    public String getWithheld_scope() {
        return withheld_scope;
    }

    public void setTime_zone(String time_zone) {
        this.time_zone = time_zone;
    }

    public boolean isDefault_profile() {
        return default_profile;
    }

    public boolean isDefault_profile_image() {
        return default_profile_image;
    }

    public boolean isFollow_request_sent() {
        return follow_request_sent;
    }

    public boolean isFollowing() {
        return following;
    }

    public boolean isIs_translator() {
        return is_translator;
    }

    public boolean isNotifications() {
        return notifications;
    }

    public boolean isProfile_background_tile() {
        return profile_background_tile;
    }

    public boolean isProfile_use_background_image() {
        return profile_use_background_image;
    }

    public String getProfile_background_color() {
        return profile_background_color;
    }

    public String getProfile_background_image_url() {
        return profile_background_image_url;
    }

    public String getProfile_background_image_url_https() {
        return profile_background_image_url_https;
    }

    public String getProfile_image_url() {
        return profile_image_url;
    }

    public void setContributeors_enabled(boolean contributeors_enabled) {
        this.contributeors_enabled = contributeors_enabled;
    }

    public String getProfile_banner_url() {
        return profile_banner_url;
    }

    public String getProfile_image_url_https() {
        return profile_image_url_https;
    }

    public void setGeo_enabled(boolean geo_enabled) {
        this.geo_enabled = geo_enabled;
    }

    public String getProfile_link_color() {
        return profile_link_color;
    }

    public void setIs_translator(boolean is_translator) {
        this.is_translator = is_translator;
    }

    public String getProfile_sidebar_border_color() {
        return profile_sidebar_border_color;
    }

    public String getProfile_text_color() {
        return profile_text_color;
    }

    public String getProfile_sidebar_fill_color() {
        return profile_sidebar_fill_color;
    }

    public String getWithheld_in_countries() {
        return withheld_in_countries;
    }

    public void setDefault_profile(boolean default_profile) {
        this.default_profile = default_profile;
    }

    public void setDefault_profile_image(boolean default_profile_image) {
        this.default_profile_image = default_profile_image;
    }

    public void setFollow_request_sent(boolean follow_request_sent) {
        this.follow_request_sent = follow_request_sent;
    }

    public void setFollowing(boolean following) {
        this.following = following;
    }

    public void setNotifications(boolean notifications) {
        this.notifications = notifications;
    }

    public void setProfile_background_color(String profile_background_color) {
        this.profile_background_color = profile_background_color;
    }

    public void setProfile_background_image_url(String profile_background_image_url) {
        this.profile_background_image_url = profile_background_image_url;
    }

    public void setProfile_background_image_url_https(String profile_background_image_url_https) {
        this.profile_background_image_url_https = profile_background_image_url_https;
    }

    public void setProfile_background_tile(boolean profile_background_tile) {
        this.profile_background_tile = profile_background_tile;
    }

    public void setProfile_image_url(String profile_image_url) {
        this.profile_image_url = profile_image_url;
    }

    public void setProfile_banner_url(String profile_banner_url) {
        this.profile_banner_url = profile_banner_url;
    }

    public void setProfile_image_url_https(String profile_image_url_https) {
        this.profile_image_url_https = profile_image_url_https;
    }

    public void setProfile_sidebar_border_color(String profile_sidebar_border_color) {
        this.profile_sidebar_border_color = profile_sidebar_border_color;
    }

    public void setProfile_link_color(String profile_link_color) {
        this.profile_link_color = profile_link_color;
    }

    public void setProfile_sidebar_fill_color(String profile_sidebar_fill_color) {
        this.profile_sidebar_fill_color = profile_sidebar_fill_color;
    }

    public void setProfile_text_color(String profile_text_color) {
        this.profile_text_color = profile_text_color;
    }

    public void setProfile_use_background_image(boolean profile_use_background_image) {
        this.profile_use_background_image = profile_use_background_image;
    }
}