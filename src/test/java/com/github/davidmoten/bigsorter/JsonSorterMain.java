package com.github.davidmoten.bigsorter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Iterator;

public class JsonSorterMain {

    private static final long N = 1000000;

    public static void main(String[] args) throws IOException {

        Iterator<String> it = new Iterator<String>() {

            long count = N;
            SecureRandom random = new SecureRandom();
            byte[] bytes = new byte[16];

            @Override
            public boolean hasNext() {
                return count > 0;
            }

            @Override
            public String next() {
                count--;
                StringBuilder s = new StringBuilder();
                if (count == N - 1) {
                    s.append("[");
                }
                random.nextBytes(bytes);
                s.append("{\"name\":\"" + Base64.getEncoder().encodeToString(bytes) + "\", \"age\":"
                        + Math.abs(random.nextInt()) % 100 + "}");
                if (count == 0) {
                    s.append("]");
                } else {
                    s.append(",");
                }
                return s.toString();
            }
        };
        InputStream in = new InputStream() {

            ByteArrayInputStream b = new ByteArrayInputStream(new byte[0]);

            @Override
            public int read() throws IOException {
                int x;
                while ((x = b.read()) == -1) {
                    if (it.hasNext()) {
                        String next = it.next();
                        b = new ByteArrayInputStream(next.getBytes(StandardCharsets.UTF_8));
                    } else {
                        return -1;
                    }
                }
                return x;
            }

        };

        Sorter.serializer(Serializer.jsonArray()) //
                .comparator((x, y) -> x.get("name").asText().compareTo(y.get("name").asText())) //
                .input(in) //
                .output(new File("target/output.json")) //
                .loggerStdOut() //
                .sort();
    }

}
