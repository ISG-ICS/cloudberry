package edu.uci.ics.cloudberry.noah.parserTest;

import edu.uci.ics.cloudberry.noah.parser.HTMLparser;
import java.io.*;
import org.junit.Test;

public class HTMLparserTest {
    @Test
    public void testParser() throws IOException {
        HTMLparser hp = new HTMLparser();
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("state100.html").getFile());
        String result = hp.parsePromedEmail(file);
        System.out.println(result);
    }
}
