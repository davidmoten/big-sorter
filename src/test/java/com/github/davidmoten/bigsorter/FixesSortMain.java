package com.github.davidmoten.bigsorter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import org.davidmoten.hilbert.HilbertCurve;
import org.davidmoten.hilbert.SmallHilbertCurve;

public class FixesSortMain {

    static final class Extremes {
        final float minLat;
        final float maxLat;
        final float minLon;
        final float maxLon;
        final long minTime;
        final long maxTime;

        Extremes(float minLat, float maxLat, float minLon, float maxLon, long minTime,
                long maxTime) {
            super();
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLon = minLon;
            this.maxLon = maxLon;
            this.minTime = minTime;
            this.maxTime = maxTime;
        }
    }

    public static void main(String[] args) throws IOException {
        String filename = "2019-05-15.binary-fixes-with-mmsi.gz";
        File file = new File(System.getProperty("user.home") + "/Downloads/" + filename);
        float minLat = 90;
        float maxLat = -90;
        float minLon = 180;
        float maxLon = -180;
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        int count = 0;
        try (DataInputStream dis = new DataInputStream(
                new GZIPInputStream(new FileInputStream(file)))) {
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
                count++;
            }
            System.out.println("minLat=" + minLat);
            System.out.println("maxLat=" + maxLat);
            System.out.println("minLon=" + minLon);
            System.out.println("maxLon=" + maxLon);
            System.out.println("minTime=" + minTime);
            System.out.println("maxTime=" + maxTime);
        }
        Extremes extremes = new Extremes(minLat, maxLat, minLon, maxLon, minTime, maxTime);
        int bits = 10;
        int dimensions = 3;
        SmallHilbertCurve hc = HilbertCurve.small().bits(bits).dimensions(dimensions);
        int recordSize = 35;
        Serializer<byte[]> ser = Serializer.fixedSizeRecord(recordSize);
        Comparator<byte[]> comparator = (x, y) -> {
            int indexX = getIndex(x, extremes, hc);
            int indexY = getIndex(y, extremes, hc);
            return Integer.compare(indexX, indexY);
        };
        File output2 = new File("target/output-sorted");
        Sorter //
                .serializer(ser) //
                .comparator(comparator) //
                .input(file) //
                .output(output2) //
                .loggerStdOut() //
                .sort();
        {
            System.out.println("first 100 hilbert curve indexes of output:");
            Reader<byte[]> r = ser.createReader(new FileInputStream(output2));
            for (int i = 0; i < 100; i++) {
                byte[] b = r.read();
                int index = getIndex(b, extremes, hc);
                System.out.println(index);
            }
        }

        int numIndexEntries = 10000;
        long chunk = count / numIndexEntries;

        // write idx
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
                    int index = getIndex(b, extremes, hc);
                    if (i % chunk == 0) {
                        dos.writeInt((int) position);
                        dos.writeInt(index);
                    }
                    i++;
                    position += recordSize;
                }
            }
        }

        // read idx file
        System.out.println("reading idx file");
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

    private static int getIndex(byte[] x, Extremes e, SmallHilbertCurve hc) {
        ByteBuffer bb = ByteBuffer.wrap(x);
        float lat = bb.getFloat();
        float lon = bb.getFloat();
        long t = bb.getLong();
        long a = Math.round((double) ((lat - e.minLat) / (e.maxLat - e.minLat)) * hc.maxOrdinate());
        long b = Math.round((double) ((lon - e.minLon) / (e.maxLon - e.minLon)) * hc.maxOrdinate());
        long c = Math
                .round((double) ((t - e.minTime) / (e.maxTime - e.minTime)) * hc.maxOrdinate());
        return (int) hc.index(a, b, c);
    }

    public static byte[] intToBytes(int value) {
        return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8),
                (byte) value };
    }

}
