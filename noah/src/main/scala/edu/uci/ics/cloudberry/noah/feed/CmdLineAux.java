package edu.uci.ics.cloudberry.noah.feed;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

public class CmdLineAux {

    public static Config parseCmdLine(String[] args) {
        Config config = new Config();
        CmdLineParser parser = new CmdLineParser(config);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e);
            parser.printUsage(System.err);
        }
        return config;
    }

    public static BufferedWriter createWriter(String fileName) throws IOException {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Date now = new Date();
        String strDate = sdfDate.format(now);
        fileName += strDate + ".gz";
        GZIPOutputStream zip = new GZIPOutputStream(
                new FileOutputStream(new File(fileName)));
        BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(zip, "UTF-8"));
        return bw;
    }
}
