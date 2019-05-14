package com.github.davidmoten.bigsorter;

import java.io.InputStream;
import java.io.OutputStream;

public interface Serializer<T> {

	T read(InputStream is);
	
	void write(OutputStream out, T value);
	
}
