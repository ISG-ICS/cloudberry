package edu.uci.ics.cloudberry.noah.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.*;

public class HTMLparser {

    public static String parsePromedEmail(String directory) throws IOException {
        File f = new File(directory);
        String wanted_info = null;
        if (f.isFile()) {
            try {
                Document parsedDocument = Jsoup.parse(f, "UTF-8");
                wanted_info = parsedDocument.getElementById("preview").text();
                if (wanted_info.intern() == "") { //if there is not useful information found
                    throw new Exception("empty file");
                }
                return wanted_info;
            } catch (Exception e) {
                System.out.println("catch an exception");
            }
        } else {
            System.out.println("This is not A File");
        }
        return wanted_info;
    }
}
