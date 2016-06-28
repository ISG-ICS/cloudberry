package edu.uci.ics.cloudberry.noah.parserTest;

import edu.uci.ics.cloudberry.noah.parser.HTMLparser;

import java.io.IOException;

/**
 * Created by dennis126 on 06/28/2016.
 */

import org.junit.Test;

public class HTMLparserTest {
    @Test
    public void test() throws IOException {
        HTMLparser hp = new HTMLparser();
        String result = hp.parse("C:\\Users\\dennis126\\Desktop\\Summer2017\\lab\\mycloudberry\\cloudberry\\noah\\src\\test\\resources\\state100.html");
        System.out.println(result);
    }
}
