package Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtil {

    private static File configFile = new File("./conf/config.properties");

    public static Properties loadProperties(Properties configProps) throws IOException {
        Properties defaultProps = new Properties();
        // sets default properties
        defaultProps.setProperty("firstDate", "20180101");
        defaultProps.setProperty("lastDate", "20181231");
        defaultProps.setProperty("queryPeriod", "10");

        configProps = new Properties(defaultProps);

        // loads properties from file
        InputStream inputStream = new FileInputStream(configFile);
        configProps.load(inputStream);
        inputStream.close();
        return configProps;
    }
}
