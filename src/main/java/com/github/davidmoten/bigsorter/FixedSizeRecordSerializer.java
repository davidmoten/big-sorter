package com.github.davidmoten.bigsorter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

final class FixedSizeRecordSerializer extends DataSerializer<byte[]> {

	private final int size;

	FixedSizeRecordSerializer(int size) {
		this.size = size;
	}

	@Override
	public byte[] read(DataInputStream dis) throws IOException {
		byte[] bytes = new byte[size];
		dis.readFully(bytes);
		return bytes;
	}

	@Override
	public void write(DataOutputStream dos, byte[] value) throws IOException {
		dos.write(value);
	}

}
