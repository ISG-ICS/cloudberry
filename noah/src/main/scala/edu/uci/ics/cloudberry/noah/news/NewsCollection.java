package edu.uci.ics.cloudberry.noah.news;

/**
 * Collecting news articles from webhose.io and storing in JSON file.
 *
 * Created by Kaiyi Ma on 7/12/16.
 */

import com.buzzilla.webhose.client.WebhoseClient.WebhoseUrl;
import com.buzzilla.webhose.client.WebhoseQuery;
import com.buzzilla.webhose.client.WebhoseResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;


public class NewsCollection {

    private static String apiKey = "";
    private static String filePath = "";
    private static String ts = null;
    private static String propFileName = System.getProperty("user.dir") + "/noah/src/main/scala/edu/uci/ics/cloudberry/noah/news/news.properties";

    public static void main( String[] args ) throws IOException {

        loadPropValue();

        WebhoseQuery query = new WebhoseQuery();
        query.title = "Zika";
        query.siteTypes.add(WebhoseQuery.SiteType.news);

        WebhoseResponse response = search_ts(query.toString(), Long.parseLong(ts));
        String response_str = "";
        if(response.totalResults == 0) {
            System.out.println("No new result available.");
            return;
        }
        while (true){

            response_str += responseToString(response) + "\n";

            if(response.moreResultsAvailable != 0) {
                response = getMore(response);
            }
            else {
                //Update the timestamp
                List<NameValuePair> params =
                        URLEncodedUtils.parse(response.next, StandardCharsets.UTF_8);
                for (final NameValuePair param : params) {
                    if (param.getName().equals("ts")) {
                        ts = param.getValue();
                    }
                }

                File outFile = new File(filePath + "/response_" + ts + ".json");
                try {
                    FileUtils.writeStringToFile(outFile, response_str);
                    System.out.println("response saved to file.");
                    savePropValue();
                } catch (IOException io) {
                    io.printStackTrace();
                }
                break;
            }
        }
    }

    private static void loadPropValue() {
        Properties prop = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream(propFileName);
            prop.load(input);

            //set attribute values
            apiKey = prop.getProperty("apiKey");
            filePath = prop.getProperty("filePath");
            ts = prop.getProperty("ts");

        } catch (IOException io) {
            io.printStackTrace();
        }
        finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void savePropValue() {
        Properties prop = new Properties();
        OutputStream output = null;

        try {

            output = new FileOutputStream(propFileName);

            // set the new timestamp value
            prop.setProperty("apiKey", apiKey);
            prop.setProperty("filePath", filePath);
            prop.setProperty("ts", ts);

            // save properties to project root folder
            prop.store(output, null);

        } catch (IOException io) {
            io.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private static WebhoseResponse search_ts(String query, Long ts) throws IOException {


        WebhoseUrl url = new WebhoseUrl("https://webhose.io/search");
        url.token = apiKey;
        url.query = query;
        url.fromTimestamp = ts;

        HttpResponse jsonResponse = sendRequest(url);
        return jsonResponse.parseAs(WebhoseResponse.class);
    }

    private static WebhoseResponse getMore(WebhoseResponse response) throws IOException {
        WebhoseUrl url = new WebhoseUrl("https://webhose.io" + response.next);

        HttpResponse jsonResponse = sendRequest(url);
        return jsonResponse.parseAs(WebhoseResponse.class);
    }

    private static HttpResponse sendRequest(WebhoseUrl url) throws IOException{
        HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
        final JsonFactory JSON_FACTORY = new JacksonFactory();

        HttpRequestFactory requestFactory =
                HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
                    public void initialize(HttpRequest request) {
                        request.setParser(new JsonObjectParser(JSON_FACTORY));
                    }
                });
        HttpRequest request = requestFactory.buildGetRequest(url);
        return request.execute();
    }

    private static String responseToString(WebhoseResponse response) throws JsonProcessingException{
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(response.posts);
    }
}
