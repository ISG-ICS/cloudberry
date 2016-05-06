package edu.uci.ics.cloudberry.noah.adm;

import edu.uci.ics.cloudberry.gnosis.NewYorkGnosis;
import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TaxiTrip {
    public static String VENDOR_ID = "vendor_id";
    public static String PICKUP_DATETIME = "pickup_datetime";
    public static String DROPOFF_DATETIME = "dropoff_datetime";
    public static String PASSENGER_COUNT = "passenger_count";
    public static String TRIP_DISTANCE = "trip_distance";
    public static String PICKUP_LOCATION = "pickup_location";
    public static String DROPOFF_LOCATION = "dropoff_location";
    public static String FARE_AMOUNT = "fare_amount";
    public static String TIP_AMOUNT = "tip_amount";
    public static String TOTAL_AMOUNT = "total_amount";
    public static String PICKUP_GEO_TAG = "pickup_geo_tag";
    public static String DROPOFF_GEO_TAG = "dropoff_geo_tag";

    public static Integer VENDOR_ID_INDEX = 0;
    public static Integer PICKUP_DATETIME_INDEX = 1;
    public static Integer DROPOFF_DATETIME_INDEX = 2;
    public static Integer PASSENGER_COUNT_INDEX = 3;
    public static Integer TRIP_DISTANCE_INDEX = 4;
    public static Integer PICKUP_LONGITUDE_INDEX = 5;
    public static Integer PICKUP_LATITUDE_INDEX = 6;
    public static Integer RATECODE_ID_INDEX = 7;
    public static Integer STORE_AND_FWD_FLAG_INDEX = 8;
    public static Integer DROPOFF_LONGITUDE_INDEX = 9;
    public static Integer DROPOFF_LATITUDE_INDEX = 10;
    public static Integer PAYMENT_TYPE_INDEX = 11;
    public static Integer FARE_AMOUNT_INDEX = 12;
    public static Integer EXTRA_INDEX = 13;
    public static Integer MTA_TAX_INDEX = 14;
    public static Integer TIP_AMOUNT_INDEX = 15;
    public static Integer TOLLS_AMOUNT_INDEX = 16;
    public static Integer IMPROVEMENT_SURCHARGE_INDEX = 17;
    public static Integer TOTAL_AMOUNT_INDEX = 18;

    public static String COMMA = ",";

    public static void toADMFile(String srcPath, NewYorkGnosis gnosis) {
        BufferedReader br;
        String line;

        try {
            FileOutputStream fos = new FileOutputStream(new File("taxi.adm"));
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            br = new BufferedReader(new FileReader(srcPath));
            br.readLine();      //skip first line

            while ((line = br.readLine()) != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("{");
                String[] cols = line.split(COMMA);
                ADM.keyValueToSbWithComma(sb, VENDOR_ID, ADM.mkInt8Constructor(cols[VENDOR_ID_INDEX]));
                DateFormat formatter = new SimpleDateFormat("y-M-d H:m:s");
                Date pickup_date = formatter.parse(cols[PICKUP_DATETIME_INDEX]);
                ADM.keyValueToSbWithComma(sb, PICKUP_DATETIME, ADM.mkDateTimeConstructor(pickup_date));
                Date dropoff_date = formatter.parse(cols[DROPOFF_DATETIME_INDEX]);
                ADM.keyValueToSbWithComma(sb, DROPOFF_DATETIME, ADM.mkDateTimeConstructor(dropoff_date));
                ADM.keyValueToSbWithComma(sb, PASSENGER_COUNT, ADM.mkInt32Constructor(cols[PASSENGER_COUNT_INDEX]));
                ADM.keyValueToSbWithComma(sb, TRIP_DISTANCE, ADM.mkFloatConstructor(cols[TRIP_DISTANCE_INDEX]));
                ADM.keyValueToSbWithComma(sb, PICKUP_LOCATION, ADM.mkPoint(cols[PICKUP_LONGITUDE_INDEX], cols[PICKUP_LATITUDE_INDEX]));
                ADM.keyValueToSbWithComma(sb, DROPOFF_LOCATION, ADM.mkPoint(cols[DROPOFF_LONGITUDE_INDEX], cols[DROPOFF_LATITUDE_INDEX]));
                ADM.keyValueToSbWithComma(sb, FARE_AMOUNT, ADM.mkFloatConstructor(cols[FARE_AMOUNT_INDEX]));
                ADM.keyValueToSbWithComma(sb, TIP_AMOUNT, ADM.mkFloatConstructor(cols[TIP_AMOUNT_INDEX]));
                ADM.keyValueToSbWithComma(sb, TOTAL_AMOUNT, ADM.mkFloatConstructor(cols[TOTAL_AMOUNT_INDEX]));
                String pickup_geoTags = geoTag(cols[PICKUP_LONGITUDE_INDEX], cols[PICKUP_LATITUDE_INDEX], gnosis);
                ADM.keyValueToSbWithComma(sb, PICKUP_GEO_TAG, pickup_geoTags);
                String dropoff_geoTags = geoTag(cols[DROPOFF_LONGITUDE_INDEX], cols[DROPOFF_LATITUDE_INDEX], gnosis);
                ADM.keyValueToSb(sb, DROPOFF_GEO_TAG, dropoff_geoTags);
                sb.append("}\n");
                osw.write(sb.toString());
            }
            br.close();
            osw.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }


    public static String geoTag(String lng, String lat, NewYorkGnosis gnosis) {
        StringBuilder sb = new StringBuilder();
        scala.Option<NewYorkGnosis.NYGeoTagInfo> info = gnosis.tagPoint(Double.parseDouble(lng), Double.parseDouble(lat));
        if (info.isEmpty()) {
            return "null";
        }
        sb.append(info.get().toString());
        return sb.toString();
    }
}
