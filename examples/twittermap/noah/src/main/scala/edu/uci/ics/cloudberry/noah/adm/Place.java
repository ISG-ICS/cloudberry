package edu.uci.ics.cloudberry.noah.adm;

import edu.uci.ics.cloudberry.util.Rectangle;
import twitter4j.GeoLocation;

import static edu.uci.ics.cloudberry.noah.adm.ADM.coordinates2Rectangle;

public class Place {

  public static String COUNTRY = "country";
  public static String COUNTRY_CODE = "country_code";
  public static String FULL_NAME = "full_name";
  public static String ID = "id";
  public static String NAME = "name";
  public static String PLACE_TYPE = "place_type";
  public static String BOUNDING_BOX = "bounding_box";

  public static String toADM(twitter4j.Place place) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    ADM.keyValueToSbWithComma(sb, COUNTRY, ADM.mkQuote(place.getCountry()));
    ADM.keyValueToSbWithComma(sb, COUNTRY_CODE, ADM.mkQuote(place.getCountry()));
    ADM.keyValueToSbWithComma(sb, FULL_NAME, ADM.mkQuote(place.getFullName()));
    ADM.keyValueToSbWithComma(sb, ID, ADM.mkQuote(String.valueOf(place.getId())));
    ADM.keyValueToSbWithComma(sb, NAME, ADM.mkQuote(place.getName()));
    ADM.keyValueToSbWithComma(sb, PLACE_TYPE, ADM.mkQuote(place.getPlaceType()));
    ADM.keyValueToSb(sb, BOUNDING_BOX, ADM.mkQuote(mkRectangleConstructor(place.getBoundingBoxCoordinates())));
    sb.append("}");
    return sb.toString();
  }

  private static String mkRectangleConstructor(GeoLocation[][] boundingBoxCoordinates)
      throws IllegalArgumentException {
    StringBuilder sb = new StringBuilder("LINESTRING");

    Rectangle rectangle = coordinates2Rectangle(boundingBoxCoordinates);
    sb.append("(").append(rectangle.swLog()).append(' ')
        .append(rectangle.swLat())
        .append(',')
        .append(rectangle.neLog()).append(' ')
        .append(rectangle.neLat())
        .append(")");
    return sb.toString();
  }
}