package com.github.davidmoten.bigsorter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.junit.Test;

public class SorterTest {

    @Test
    public void test() throws FileNotFoundException {
        String s = "1346749822";
        File f = new File("target/temp.txt");
        try (PrintStream out = new PrintStream(f)) {
            out.print(s);
        }
        Serializer<String> serializer = new Serializer<String>() {

            @Override
            public String read(InputStream is) {
                
            }

            @Override
            public void write(OutputStream out, String value) {
                // TODO Auto-generated method stub
                
            }};
    }

}
