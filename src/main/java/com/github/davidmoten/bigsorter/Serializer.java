package com.github.davidmoten.bigsorter;

import java.io.InputStream;
import java.io.OutputStream;

public interface Serializer<T> {

	Reader<T> createReader(InputStream in);
	
	Writer<T> createWriter(OutputStream out);
	
}
