package com.github.davidmoten.bigsorter;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;

public interface Serializer<T> {

	Reader<T> createReader(InputStream in);

	Writer<T> createWriter(OutputStream out);

	public static Serializer<String> linesUtf8() {
		return LinesSerializer.LINES_UTF8;
	}

	public static Serializer<String> lines(Charset charset) {
		return new LinesSerializer(charset);
	}
	
	public static <T extends Serializable> Serializer<T> java() {
		return JavaSerializer.instance();
	}
}
