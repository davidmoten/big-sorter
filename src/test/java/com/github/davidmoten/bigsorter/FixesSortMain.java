package com.github.davidmoten.bigsorter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.zip.GZIPInputStream;

import org.davidmoten.hilbert.HilbertCurve;
import org.davidmoten.hilbert.SmallHilbertCurve;

public class FixesSortMain {

	public static void main(String[] args) throws IOException {
		String filename = "2019-05-15.binary-fixes-with-mmsi.gz";
		File file = new File("/home/dxm/Downloads/" + filename);
		float minLat = 90;
		float maxLat = -90;
		float minLon = 180;
		float maxLon = -180;
		long minTime = Long.MAX_VALUE;
		long maxTime = Long.MIN_VALUE;
		try (DataInputStream dis = new DataInputStream(new GZIPInputStream(new FileInputStream(file)))) {
			for (;;) {
				float lat;
				try {
					dis.skip(4);
					lat = dis.readFloat();
				} catch (EOFException e) {
					break;
				}
				if (lat < minLat) {
					minLat = lat;
				}
				if (lat > maxLat) {
					maxLat = lat;
				}
				float lon = dis.readFloat();
				if (lon < minLon) {
					minLon = lon;
				}
				if (lon > maxLon) {
					maxLon = lon;
				}
				long t = dis.readLong();
				if (t < minTime) {
					minTime = t;
				}
				if (t > maxTime) {
					maxTime = t;
				}
				dis.skip(15);
			}
			System.out.println("minLat=" + minLat);
			System.out.println("maxLat=" + maxLat);
			System.out.println("minLon=" + minLon);
			System.out.println("maxLon=" + maxLon);
			System.out.println("minTime=" + minTime);
			System.out.println("maxTime=" + maxTime);
		}

		SmallHilbertCurve hc = HilbertCurve.small().bits(10).dimensions(3);
		byte[] buffer = new byte[35];
		ByteBuffer bb = ByteBuffer.wrap(buffer);
		File output = new File("target/output");
		try ( //
				OutputStream out = new BufferedOutputStream(new FileOutputStream(output));
				DataInputStream dis = new DataInputStream(
						new GZIPInputStream(new BufferedInputStream(new FileInputStream(file))))) {
			while (true) {
				try {
					dis.readFully(buffer);
				} catch (EOFException e) {
					break;
				}
				bb.rewind();
				float lat = bb.getFloat();
				float lon = bb.getFloat();
				long t = bb.getLong();
				long a = Math.round((double) ((lat - minLat) / (maxLat - minLat)) * hc.maxOrdinate());
				long b = Math.round((double) ((lon - minLon) / (maxLon - minLon)) * hc.maxOrdinate());
				long c = Math.round((double) ((t - minTime) / (maxTime - minTime)) * hc.maxOrdinate());
				int index = (int) hc.index(a, b, c);
				out.write(buffer);
				out.write(intToBytes(index));
			}
		}
		Serializer<byte[]> ser = Serializer.fixedSizeRecord(39);
		Comparator<byte[]> comparator = (x, y) -> {
			int indexX = getInt(x, 35);
			int indexY = getInt(y, 35);
			return Integer.compare(indexX, indexY);
		};
		File output2 = new File("target/output-sorted");
		Sorter //
				.serializer(ser) //
				.comparator(comparator) //
				.input(output) //
				.output(output2) //
				.sort();
		System.out.println("done");

		System.out.println("first 10 hilbert curve indexes of output:");
		Reader<byte[]> r = ser.createReader(new FileInputStream(output2));
		for (int i = 0; i < 10; i++) {
			byte[] b = r.read();
			int index = getInt(b, 35);
			System.out.println(index);
		}
	}

	public static byte[] intToBytes(int value) {
		return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value };
	}

	// TODO unit test
	public static int getInt(byte[] b, int start) {
		byte b1 = b[start];
		byte b2 = b[start + 1];
		byte b3 = b[start + 2];
		byte b4 = b[start + 3];
		return ((0xFF & b1) << 24) | ((0xFF & b2) << 16) | ((0xFF & b3) << 8) | (0xFF & b4);
	}
}
