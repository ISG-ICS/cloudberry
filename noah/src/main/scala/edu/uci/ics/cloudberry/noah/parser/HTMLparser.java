package edu.uci.ics.cloudberry.noah.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.*;

public class HTMLparser {

    public static String parsePromedEmail(File file) throws IOException {
        File f = file;
        String wanted_info = null;
        if (f.isFile()) {
            try {
                Document parsedDocument = Jsoup.parse(f, "UTF-8");
                wanted_info = parsedDocument.getElementById("preview").text();
                return wanted_info;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("This is not A File");
        }
        return wanted_info;
    }
}
