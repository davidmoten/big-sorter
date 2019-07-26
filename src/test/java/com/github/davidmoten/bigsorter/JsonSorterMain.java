package com.github.davidmoten.bigsorter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Iterator;

public class JsonSorterMain {

    private static final long N = 1000;

    public static void main(String[] args) throws IOException {

        Iterator<String> it = new Iterator<String>() {

            long remaining = N;
            SecureRandom random = new SecureRandom();
            byte[] bytes = new byte[16];

            @Override
            public boolean hasNext() {
                return remaining > 0;
            }

            @Override
            public String next() {
                remaining--;
                StringBuilder s = new StringBuilder();
                if (remaining == N - 1) {
                    s.append("[");
                }
                random.nextBytes(bytes);
                s.append("{\"name\":\"" + Base64.getEncoder().encodeToString(bytes) + "\", \"age\":"
                        + Math.abs(random.nextInt()) % 100 + "}");
                if (remaining == 0) {
                    s.append("]");
                } else {
                    s.append(",");
                }
                return s.toString();
            }
        };
        // InputStream in = new InputStream() {
        //
        // ByteArrayInputStream b = new ByteArrayInputStream(new byte[0]);
        //
        // @Override
        // public int read() throws IOException {
        // int x;
        // while ((x = b.read()) == -1) {
        // if (it.hasNext()) {
        // String next = it.next();
        // b = new ByteArrayInputStream(next.getBytes(StandardCharsets.UTF_8));
        // } else {
        // return -1;
        // }
        // }
        // return x;
        // }
        //
        // };
        InputStream in = new SequenceInputStream(new Enumeration<InputStream>() {

            @Override
            public boolean hasMoreElements() {
                return it.hasNext();
            }

            @Override
            public InputStream nextElement() {
                return new ByteArrayInputStream(it.next().getBytes(StandardCharsets.UTF_8));
            }
        });

        Sorter.serializer(Serializer.jsonArray()) //
                .comparator((x, y) -> x.get("name").asText().compareTo(y.get("name").asText())) //
                .input(in) //
                .output(new File("target/output.json")) //
                .loggerStdOut() //
                .sort();
    }

}
