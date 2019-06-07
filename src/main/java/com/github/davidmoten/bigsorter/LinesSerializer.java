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

class LinesSerializer implements Serializer<String> {

	static final Serializer<String> LINES_UTF8 = new LinesSerializer(StandardCharsets.UTF_8);

	private final Charset charset;

	LinesSerializer(Charset charset) {
		this.charset = charset;
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
				bw.write("\n");
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
