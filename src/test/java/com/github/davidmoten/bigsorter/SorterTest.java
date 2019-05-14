package com.github.davidmoten.bigsorter;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class SorterTest {

    @Test
    public void test() throws IOException {
        String s = "1346749822";
        File f = new File("target/temp.txt");
        try (PrintStream out = new PrintStream(f)) {
            out.print(s);
        }
        Serializer<Character> serializer = new Serializer<Character>() {

            @Override
            public Reader<Character> createReader(InputStream in) {
                return new Reader<Character>() {

                    java.io.Reader r = new InputStreamReader(in, StandardCharsets.UTF_8);

                    @Override
                    public Character read() throws IOException {
                        return Character.valueOf((char) r.read());
                    }

                };
            }

            @Override
            public Writer<Character> createWriter(OutputStream out) {
                return new Writer<Character>() {

                    java.io.Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8);

                    @Override
                    public void write(Character value) throws IOException {
                        w.write((int) value.charValue());
                    }
                };
            }

        };
        Reader<Character> reader = serializer.createReader(new FileInputStream(f));
        assertEquals(Character.valueOf('1'), reader.read());
        assertEquals(Character.valueOf('3'), reader.read());
        assertEquals(Character.valueOf('4'), reader.read());

        File output = new File("target/out.txt");
        Sorter<Character> sorter = new Sorter<Character>(f, serializer, output,
                (x, y) -> Character.compare(x, y), 20, 3);
        sorter.sort();
    }

}
