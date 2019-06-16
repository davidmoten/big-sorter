package com.github.davidmoten.bigsorter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class DataSerializer<T> implements Serializer<T> {

	public abstract T read(DataInputStream dis) throws IOException;

	public abstract void write(DataOutputStream dos, T value) throws IOException;
	
	@Override
	public Reader<T> createReader(InputStream in) {
		return new Reader<T>() {

			final DataInputStream dis = new DataInputStream(in);

			@Override
			public T read() throws IOException {
				try {
					return DataSerializer.this.read(dis);
				} catch (EOFException e) {
					return null;
				}
			}
			
			@Override
            public void close() throws IOException {
                dis.close();
            }
		};
	}

	@Override
	public Writer<T> createWriter(OutputStream out) {
		return new Writer<T>() {

			final DataOutputStream dos = new DataOutputStream(out);

			@Override
			public void write(T value) throws IOException {
				DataSerializer.this.write(dos, value);
			}
			
			@Override
            public void close() throws IOException {
                dos.close();
            }

            @Override
            public void flush() throws IOException {
                dos.flush();
            }
		};
	}

}
