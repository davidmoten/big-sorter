package com.github.davidmoten.bigsorter;

import java.io.Closeable;
import java.io.IOException;

public interface Reader<T> extends Closeable {

	/**
	 * Returns the next read value. If no more values returns null.
	 * 
	 * @return the next read value or null if no more values
	 * @throws IOException on IO problem
	 */
	T read() throws IOException;

	/**
	 * Returns the next read value. If no more values close() is called then null
	 * returned.
	 * 
	 * @return the next read value or null if no more values
	 * @throws IOException on IO problem
	 */
	default T readAutoClosing() throws IOException {
		T v = read();
		if (v == null) {
			close();
		}
		return v;
	}

}
