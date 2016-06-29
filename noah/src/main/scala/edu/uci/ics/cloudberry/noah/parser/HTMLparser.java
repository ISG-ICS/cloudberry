package edu.uci.ics.cloudberry.noah.parser;

/**
 * Created by Zhenfeng Qi on 06/28/2016.
 */

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.*;
import java.io.IOException;

public class HTMLparser {

//    public static void main(String[] args)throws IOException{
//        System.out.println("hello");
//        String result = parse("C:\\Users\\dennis126\\Desktop\\Summer2017\\lab\\mycloudberry\\cloudberry\\noah\\src\\test\\resources\\state100.html");
//        System.out.println(result);
//    }


    public static String parse(String dirc)throws IOException{
        File f = new File(dirc);
        String info = null;
        if(f.isFile()){
            try{
                Document doc = Jsoup.parse(f, "UTF-8", "http://example.com/");
                info = doc.getElementById("preview").text();
                if(info.intern()==""){
                    throw new Exception("empty file");
                }
                return info;
            }catch (Exception e){

            }
        }else {
            System.out.println("This is not A File");
        }
        return info;
    }
}
