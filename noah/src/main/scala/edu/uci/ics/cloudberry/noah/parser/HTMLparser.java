package edu.uci.ics.cloudberry.noah.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;


import java.io.File;
import java.io.IOException;

public class HTMLparser {

    public static Elements parsePromedEmail(File file) throws IOException {
        File f = file;
        Elements wanted_info = null;
        if (f.isFile()) {
            try {
                Document parsedDocument = Jsoup.parse(f, "UTF-8");
                wanted_info = parsedDocument.getElementsByAttribute("onclick");
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
