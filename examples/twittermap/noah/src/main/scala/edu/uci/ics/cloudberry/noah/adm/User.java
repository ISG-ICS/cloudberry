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

    public static StringBuilder userSB=new StringBuilder();
    //
    public static String toADM(twitter4j.User user) {

        userSB.delete(0,userSB.length());
        userSB.append("{");
        ADM.keyValueToSbWithComma(userSB, ID, ADM.mkADMConstructor("int64", String.valueOf(user.getId())));
        ADM.keyValueToSbWithComma(userSB, NAME, ADM.mkQuote(user.getName()));
        ADM.keyValueToSbWithComma(userSB, SCREEN_NAME, ADM.mkQuote(user.getScreenName()));
        ADM.keyValueToSbWithComma(userSB, PROFILE_IMAGE_URL, ADM.mkQuote(user.getProfileImageURL()));
        ADM.keyValueToSbWithComma(userSB, LANG, ADM.mkQuote(user.getLang()));
        ADM.keyValueToSbWithComma(userSB, LOCATION, ADM.mkQuote(user.getLocation()));
        ADM.keyValueToSbWithComma(userSB, CREATE_AT, ADM.mkDateConstructor(user.getCreatedAt()));
        ADM.keyValueToSbWithComma(userSB, DESCRIPTION, ADM.mkQuote(user.getDescription()));
        ADM.keyValueToSbWithComma(userSB, FOLLOWERS_COUNT, String.valueOf(user.getFollowersCount()));
        ADM.keyValueToSbWithComma(userSB, FRIENDS_COUNT, String.valueOf(user.getFriendsCount()));
        ADM.keyValueToSb(userSB, STATUS_COUNT, String.valueOf(user.getStatusesCount()));
        userSB.append("}");

        return userSB.toString();
    }
}
