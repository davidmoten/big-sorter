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
import java.util.zip.GZIPInputStream;

import org.davidmoten.hilbert.HilbertCurve;
import org.davidmoten.hilbert.SmallHilbertCurve;

public class FixesSortMain {

    public static void main(String[] args) throws IOException {
        String filename = "2019-05-15.binary-fixes-with-mmsi.gz";
        File file = new File("/home/dave/Downloads/" + filename);
        float minLat = 90;
        float maxLat = -90;
        float minLon = 180;
        float maxLon = -180;
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
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
        try ( //
                OutputStream out = new BufferedOutputStream(new FileOutputStream("target/output"));
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
                long a = Math
                        .round((double) ((lat - minLat) / (maxLat - minLat)) * hc.maxOrdinate());
                long b = Math
                        .round((double) ((lon - minLon) / (maxLon - minLon)) * hc.maxOrdinate());
                long c = Math
                        .round((double) ((t - minTime) / (maxTime - minTime)) * hc.maxOrdinate());
                long index = hc.index(a, b, c);
                out.write(buffer);
                out.write(longToBytes(index));
            }
        }
    }

    public static byte[] longToBytes(long l) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (l & 0xFF);
            l >>= 8;
        }
        return result;
    }
}
