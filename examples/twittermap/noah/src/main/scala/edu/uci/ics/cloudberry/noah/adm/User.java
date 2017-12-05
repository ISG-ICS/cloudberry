package edu.uci.ics.cloudberry.noah.adm;

public class User {

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String SCREEN_NAME = "screen_name";
    public static final String PROFILE_IMAGE_URL = "profile_image_url";
    public static final String LANG = "lang";
    public static final String LOCATION = "location";
    public static final String CREATE_AT = "create_at";
    public static final String DESCRIPTION = "description";
    public static final String FOLLOWERS_COUNT = "followers_count";
    public static final String FRIENDS_COUNT = "friends_count";
    public static final String STATUS_COUNT = "statues_count";

    public static String toADM(twitter4j.User user) {

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        ADM.keyValueToSbWithComma(sb, ID, ADM.mkADMConstructor("int64", String.valueOf(user.getId())));
        ADM.keyValueToSbWithComma(sb, NAME, ADM.mkQuote(user.getName()));
        ADM.keyValueToSbWithComma(sb, SCREEN_NAME, ADM.mkQuote(user.getScreenName()));
        ADM.keyValueToSbWithComma(sb, PROFILE_IMAGE_URL, ADM.mkQuote(user.getProfileImageURL()));
        ADM.keyValueToSbWithComma(sb, LANG, ADM.mkQuote(user.getLang()));
        ADM.keyValueToSbWithComma(sb, LOCATION, ADM.mkQuote(user.getLocation()));
        ADM.keyValueToSbWithComma(sb, CREATE_AT, ADM.mkDateConstructor(user.getCreatedAt()));
        ADM.keyValueToSbWithComma(sb, DESCRIPTION, ADM.mkQuote(user.getDescription()));
        ADM.keyValueToSbWithComma(sb, FOLLOWERS_COUNT, String.valueOf(user.getFollowersCount()));
        ADM.keyValueToSbWithComma(sb, FRIENDS_COUNT, String.valueOf(user.getFriendsCount()));
        ADM.keyValueToSb(sb, STATUS_COUNT, String.valueOf(user.getStatusesCount()));
        sb.append("}");

        return sb.toString();
    }
}
