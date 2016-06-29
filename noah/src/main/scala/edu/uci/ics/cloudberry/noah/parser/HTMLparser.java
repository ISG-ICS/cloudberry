package edu.uci.ics.cloudberry.noah.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.*;

public class HTMLparser {

    public static String parse(String dirc) throws IOException {
        File f = new File(dirc);
        String info = null;
        if (f.isFile()) {
            try {
                Document doc = Jsoup.parse(f, "UTF-8");
                info = doc.getElementById("preview").text();
                if (info.intern() == "") {
                    throw new Exception("empty file");
                }
                return info;
            } catch (Exception e) {
                System.out.println("catch an exception");
            }
        } else {
            System.out.println("This is not A File");
        }
        return info;
    }
}
