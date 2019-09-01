package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

/**
 * Util class to read properties file.
 */
public class PropertiesUtil {

    public static String firstDate;
    public static String lastDate;
    public static int queryPeriod;
    public static Calendar firstDateCalendar;
    public static Calendar lastDateCalender;
    public static SimpleDateFormat dateFormat;

    private static File configFile = new File("./conf/config.properties");

    public static void loadProperties() throws IOException {
        Properties defaultProps = new Properties();
        // sets default properties
        defaultProps.setProperty("firstDate", "20180101");
        defaultProps.setProperty("lastDate", "20181231");
        defaultProps.setProperty("queryPeriod", "10");

        Properties configProps = new Properties(defaultProps);

        // loads properties from file
        InputStream inputStream = new FileInputStream(configFile);
        configProps.load(inputStream);
        firstDate = configProps.getProperty("firstDate");
        lastDate = configProps.getProperty("lastDate");
        queryPeriod = Integer.parseInt(configProps.getProperty("queryPeriod"));
        dateFormat = new SimpleDateFormat(configProps.getProperty("dateFormat"));
        firstDateCalendar = Calendar.getInstance();
        try {
            firstDateCalendar.setTime(dateFormat.parse(firstDate));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        lastDateCalender = Calendar.getInstance();
        try {
            lastDateCalender.setTime(dateFormat.parse(lastDate));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        inputStream.close();
    }
}
