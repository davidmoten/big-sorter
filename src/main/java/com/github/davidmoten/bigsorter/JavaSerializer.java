package com.github.davidmoten.bigsorter;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;

final class JavaSerializer<T extends Serializable> implements Serializer<T> {

	private static final JavaSerializer<Serializable> INSTANCE = new JavaSerializer<>();
	
	@SuppressWarnings("unchecked")
	static <T extends Serializable> JavaSerializer<T> instance() {
		return (JavaSerializer<T>) INSTANCE;
	}

	@Override
	public Reader<T> createReader(InputStream in) {
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(in);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return new Reader<T>() {

			@SuppressWarnings("unchecked")
			@Override
			public T read() throws IOException {
				try {
					return (T) ois.readObject();
				} catch (EOFException e) {
					return null;
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public void close() throws IOException {
				ois.close();
			}
		};
	}

	@Override
	public Writer<T> createWriter(OutputStream out) {
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(out);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return new Writer<T>() {

			@Override
			public void write(T value) throws IOException {
				oos.writeObject(value);
			}

			@Override
			public void close() throws IOException {
				oos.close();
			}

            @Override
            public void flush() throws IOException {
                oos.flush();
            }
		};
	}

}
