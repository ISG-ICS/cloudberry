package algorithms;

import utils.PropertiesUtil;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;

/**
 * Implement the incremental database query
 */
public class IncrementalQuery {
    private String start;
    private String end;

    public String getStart() {
        return start;
    }

    public String getEnd() {
        return end;
    }

    /**
     * Reads the properties file.
     * @param endDate endDate parsed from query string
     */
    public void readProperties(String endDate) {
        try {
            PropertiesUtil.loadProperties();
            } catch (IOException ex) {
            System.out.println("The config.properties file does not exist, default properties loaded.");
        }
        calculateDate(endDate);
    }

    /**
     * Calculates the target query dates.
     * @param endDate endDate parsed from query string
     */
    private void calculateDate(String endDate) {
        try {
            end = (endDate == null) ? PropertiesUtil.firstDate : endDate;
            start = end;
            incrementCalendar(start);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Incremental the date by query period。
     * @param start base date
     */
    private void incrementCalendar(String start) {
        Calendar endCalendar = Calendar.getInstance();
        try {
            endCalendar.setTime(PropertiesUtil.dateFormat.parse(start));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        endCalendar.add(Calendar.HOUR, PropertiesUtil.queryPeriod);  // number of days to add
        end = PropertiesUtil.dateFormat.format(endCalendar.getTime());
    }
}
