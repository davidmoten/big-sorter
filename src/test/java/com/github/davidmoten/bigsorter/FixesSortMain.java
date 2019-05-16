package com.github.davidmoten.bigsorter;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
        try (DataInputStream dis = new DataInputStream(
                new GZIPInputStream(new FileInputStream(file)))) {
            while (true) {
                dis.skip(4);
                float lat;
                try {
                    lat = dis.readFloat();
                } catch (EOFException e) {
                    break;
                }
                float lon = dis.readFloat();
                long t = dis.readLong();
                long a = Math
                        .round((double) ((lat - minLat) / (maxLat - minLat)) * hc.maxOrdinate());
                long b = Math
                        .round((double) ((lon - minLon) / (maxLon - minLon)) * hc.maxOrdinate());
                long c = Math
                        .round((double) ((t - minTime) / (maxTime - minTime)) * hc.maxOrdinate());
                long index = hc.index(a, b, c);
                System.out.println(index);
                dis.skip(15);
            }
        }
    }

}
