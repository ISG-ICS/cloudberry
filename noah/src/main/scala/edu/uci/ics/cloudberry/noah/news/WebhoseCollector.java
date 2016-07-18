package edu.uci.ics.cloudberry.noah.news;

/**
 * Collecting news articles from webhose.io and storing in JSON file.
 *
 * Created by Kaiyi Ma on 7/12/16.
 */

import com.buzzilla.webhose.client.WebhoseClient.WebhoseUrl;
import com.buzzilla.webhose.client.WebhosePost;
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
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


public class WebhoseCollector {
    @Option(required = true,
            name = "-a",
            aliases = "--api",
            usage = "Webhose API")
    private String apiKey;

    @Option(required = true,
            name = "-o",
            aliases = "--output",
            usage = "output file directory")
    private String filePath;

    @Option(required = true,
            name = "-t",
            aliases = "--timestamp",
            usage="timestamp for search")
    private String ts;

    public static void main( String[] args ) throws IOException {
        new WebhoseCollector().doMain(args);
    }

    public void doMain(String[] args) throws IOException {
        CmdLineParser parser = new CmdLineParser(this);
        try{
            parser.parseArgument(args);
        } catch( CmdLineException e ) {
            System.err.println(e.getMessage());
            return;
        }

        WebhoseQuery query = new WebhoseQuery();
        query.title = "Zika";
        query.siteTypes.add(WebhoseQuery.SiteType.news);

        WebhoseResponse response = searchTimestamp(apiKey, query.toString(), Long.parseLong(ts));

        if(response.totalResults == 0) {
            System.err.println("No new result available.");
            return;
        }

        String fileName = "response_" + ts + "_";
        List<WebhosePost> postList = new ArrayList<>();
        while (true) {
            postList.addAll(response.posts);

            if(response.moreResultsAvailable != 0) {
                response = getMore(response);
            } else {
                //Update the timestamp
                List<NameValuePair> params =
                        URLEncodedUtils.parse(response.next, StandardCharsets.UTF_8);
                for (final NameValuePair param : params) {
                    if (param.getName().equals("ts")) {
                        ts = param.getValue();
                    }
                }

                // File Name Format: response_from_to.json
                File outFile = new File(filePath + "/" + fileName + ts + ".json");
                try {
                    FileUtils.writeStringToFile(outFile, postListToString(postList));
                    System.err.println("response saved to file.");
                    System.out.println(ts);
                } catch (IOException io) {
                    io.printStackTrace();
                }
                break;
            }
        }
    }


    private static WebhoseResponse searchTimestamp(String apiKey, String query, Long ts) throws IOException {
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


    private static String postListToString(List<WebhosePost> postList) throws JsonProcessingException{
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(postList);
    }
}
