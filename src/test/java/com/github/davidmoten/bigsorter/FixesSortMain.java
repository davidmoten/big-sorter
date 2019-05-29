package com.github.davidmoten.bigsorter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import org.davidmoten.hilbert.HilbertCurve;
import org.davidmoten.hilbert.SmallHilbertCurve;

public class FixesSortMain {

	public static void main(String[] args) throws IOException {
		String filename = "2019-05-15.binary-fixes-with-mmsi.gz";
		File file = new File(System.getProperty("user.home") + "/Downloads/" + filename);
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

		int bits = 10;
		int dimensions = 3;
		SmallHilbertCurve hc = HilbertCurve.small().bits(bits).dimensions(dimensions);
		byte[] buffer = new byte[35];
		ByteBuffer bb = ByteBuffer.wrap(buffer);
		File output = new File("target/output");
		long count = 0;
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
				count++;
			}
		}

		int recordSize = 39;
		Serializer<byte[]> ser = Serializer.fixedSizeRecord(recordSize);
		Comparator<byte[]> comparator = (x, y) -> {
			int indexX = getInt(x, recordSize - 4);
			int indexY = getInt(y, recordSize - 4);
			return Integer.compare(indexX, indexY);
		};
		File output2 = new File("target/output-sorted");
		Sorter //
				.serializer(ser) //
				.comparator(comparator) //
				.input(output) //
				.output(output2) //
				.loggerStdOut() //
				.sort();
		System.out.println("done");
		{
			System.out.println("first 10 hilbert curve indexes of output:");
			Reader<byte[]> r = ser.createReader(new FileInputStream(output2));
			for (int i = 0; i < 10; i++) {
				byte[] b = r.read();
				int index = getInt(b, 35);
				System.out.println(index);
			}
		}

		int numIndexEntries = 10000;
		long chunk = count / numIndexEntries;

		File idx = new File("target/output-sorted.idx");
		try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(idx))) {
			dos.writeInt(bits);
			dos.writeInt(dimensions);
			dos.writeDouble(minLat);
			dos.writeDouble(maxLat);
			dos.writeDouble(minLon);
			dos.writeDouble(maxLon);
			dos.writeDouble(minTime);
			dos.writeDouble(maxTime);

			dos.writeInt(numIndexEntries);
			// write 0 for int position
			// write 1 for long position
			dos.writeInt(0);
			// write index entries
			{
				Reader<byte[]> r = ser.createReader(new FileInputStream(output2));
				long i = 0;
				long position = 0;
				for (;;) {
					byte[] b = r.read();
					if (b == null) {
						break;
					}
					int index = getInt(b, 35);
					if (i % chunk == 0) {
						dos.writeInt((int) position);
						dos.writeInt(index);
						System.out.println(position + ": " + index);
					}
					i++;
					position += recordSize;
				}
			}
		}
		try (DataInputStream dis = new DataInputStream(new FileInputStream(idx))) {
			dis.skip(4 + 4 + 6 * 8);
			dis.skip(4);
			dis.skip(4);
			TreeMap<Integer, Integer> tree = new TreeMap<>();
			for (int i = 0; i < numIndexEntries; i++) {
				int position = dis.readInt();
				int index = dis.readInt();
				tree.put(index, position);
			}
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
