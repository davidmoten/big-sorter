package com.github.davidmoten.bigsorter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

final class LinesSerializer implements Serializer<String> {

    static final Serializer<String> LINES_UTF8_LF = new LinesSerializer(StandardCharsets.UTF_8,
            LineDelimiter.LINE_FEED);
    static final Serializer<String> LINES_UTF8_CR_LF = new LinesSerializer(StandardCharsets.UTF_8,
            LineDelimiter.CARRIAGE_RETURN_LINE_FEED);

    private final Charset charset;
    private final LineDelimiter delimiter;

    LinesSerializer(Charset charset, LineDelimiter delimiter) {
        this.charset = charset;
        this.delimiter = delimiter;
    }

    @Override
    public Reader<String> createReader(InputStream in) {
        return new Reader<String>() {
            BufferedReader br = new BufferedReader(new InputStreamReader(in, charset));

            @Override
            public String read() throws IOException {
                return br.readLine();
            }

            @Override
            public void close() throws IOException {
                br.close();
            }
        };
    }

    @Override
    public Writer<String> createWriter(OutputStream out) {
        return new Writer<String>() {

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out, charset));

            @Override
            public void write(String value) throws IOException {
                bw.write(value);
                bw.write(delimiter.value());
            }

            @Override
            public void close() throws IOException {
                bw.close();
            }

            @Override
            public void flush() throws IOException {
                bw.flush();
            }

        };
    }

}
