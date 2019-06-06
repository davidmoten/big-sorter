package com.github.davidmoten.bigsorter.fixes;

import static org.junit.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import org.davidmoten.hilbert.HilbertCurve;
import org.davidmoten.hilbert.Ranges;
import org.davidmoten.hilbert.SmallHilbertCurve;

import com.github.davidmoten.bigsorter.Reader;
import com.github.davidmoten.bigsorter.Serializer;
import com.github.davidmoten.bigsorter.Sorter;

public class FixesSortMain2 {

    static final class Extremes {
        final float minLat;
        final float maxLat;
        final float minLon;
        final float maxLon;
        final long minTime;
        final long maxTime;

        Extremes(float minLat, float maxLat, float minLon, float maxLon, long minTime, long maxTime) {
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLon = minLon;
            this.maxLon = maxLon;
            this.minTime = minTime;
            this.maxTime = maxTime;
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("starting");
        boolean sort = false;
        String filename = "2019-05-15.binary-fixes-with-mmsi.gz";
        int recordSize = 35;
        File input = new File(System.getProperty("user.home") + "/Downloads/" + filename);
        float minLat = 90;
        float maxLat = -90;
        float minLon = 180;
        float maxLon = -180;
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        int count = 0;
        try (DataInputStream dis = new DataInputStream(getInputStream(input))) {
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
                dis.skip(recordSize - 20);
                count++;
            }
            System.out.println("minLat=" + minLat);
            System.out.println("maxLat=" + maxLat);
            System.out.println("minLon=" + minLon);
            System.out.println("maxLon=" + maxLon);
            System.out.println("minTime=" + minTime);
            System.out.println("maxTime=" + maxTime);
        }
        Bounds extremes = new Bounds(new double[] { minLat, minLon, minTime },
                new double[] { maxLat, maxLon, maxTime });

        // sort the input file using the hilbert curve index
        int bits = 10;
        int dimensions = 3;
        SmallHilbertCurve hc = HilbertCurve.small().bits(bits).dimensions(dimensions);
        Serializer<byte[]> ser = Serializer.fixedSizeRecord(recordSize);
        Comparator<byte[]> comparator = (x, y) -> {
            int indexX = getIndex(x, extremes, hc);
            int indexY = getIndex(y, extremes, hc);
            return Integer.compare(indexX, indexY);
        };
        File sorted = new File("target/output-sorted");
        if (sort)
            try (InputStream in = getInputStream(input)) {
                Sorter //
                        .serializer(ser) //
                        .comparator(comparator) //
                        .input(in) //
                        .output(sorted) //
                        .loggerStdOut() //
                        .sort();
            }

        // sydney box
        Bounds sb = createSydneyBounds(minTime, maxTime);
        System.out.println(sb);
        System.out.println(sb.contains(new double[] { -33.82, 151.2, 2.148E9 }));
        long contains = 0;
        {
            System.out.println("checking sort worked");
            try (InputStream in = new BufferedInputStream(new FileInputStream(sorted));
                    Reader<byte[]> r = ser.createReader(in)) {
                byte[] b = r.read();
                int lastIndex = Integer.MIN_VALUE;
                while (b != null) {
                    Record rec = getRecord(b);
                    if (sb.contains(rec.toArray())) {
                        contains++;
                    }
                    if (sb.mins()[2] < rec.time && sb.maxes()[2] > rec.time) {
                        System.out.println(Arrays.toString(rec.toArray()));
                    }
                    int index = getIndex(b, extremes, hc);
                    if (index < lastIndex) {
                        throw new RuntimeException("sort did not work");
                    }
                    lastIndex = index;
                    b = r.read();
                }
            }
        }
        System.out.println("sort ok");
        System.out.println("number of records within sydney bounds = " + contains);

        printStartOfSortedIndexes(extremes, hc, ser, sorted);

        int approximateNumIndexEntries = 10000;

        // write idx
        File idx = new File("target/output-sorted.idx");
        writeIdx(recordSize, count, extremes, hc, ser, sorted, approximateNumIndexEntries, idx);

        // read idx file
        TreeMap<Integer, Long> tree = readAndPrintIndex(idx);

        tree.forEach((index, pos) -> System.out.println("index=" + index + "\tpos=" + pos));

        double[] mins = extremes.mins();
        double[] maxes = extremes.maxes();

        Index ind = new Index(tree, mins, maxes, bits);
        System.out.println("Index = " + ind);

        {
            long[] o1 = ind.ordinates(sb.mins());
            long[] o2 = ind.ordinates(sb.maxes());
            Ranges ranges = ind.hilbertCurve().query(o1, o2);
            List<PositionRange> positionRanges = ind.getPositionRanges(ranges);
            positionRanges.stream().forEach(System.out::println);
            PositionRange pr = positionRanges.get(0);
            // final InputStream in2;
            // try (DataInputStream d = new DataInputStream(new FileInputStream(sorted))) {
            // byte[] bytes2 = new byte[(int) (pr.ceilingPosition() - pr.floorPosition())];
            // d.skip(pr.floorPosition());
            // d.readFully(bytes2);
            // in2 = new ByteArrayInputStream(bytes2);
            // }
            // obfuscated urls for the brief period I'm using unauthenticated access
            String location = new String(
                    Base64.getDecoder().decode(
                            "aHR0cHM6Ly9tb3Rlbi1maXhlcy5zMy1hcC1zb3V0aGVhc3QtMi5hbWF6b25hd3MuY29tL291dHB1dC1zb3J0ZWQK"),
                    StandardCharsets.UTF_8);

            String locationIdx = new String(Base64.getDecoder().decode(
                    "aHR0cHM6Ly9tb3Rlbi1maXhlcy5zMy1hcC1zb3V0aGVhc3QtMi5hbWF6b25hd3MuY29tL291dHB1dC1zb3J0ZWQuaWR4Cg=="),
                    StandardCharsets.UTF_8);

            for (int i = 1; i < 10; i++) {
                long t = System.currentTimeMillis();
                InputStream in3 = getIndexInputStream(locationIdx);
                ind = Index.read(in3);
                System.out.println("read index in " + (System.currentTimeMillis() - t) + "ms");
                URL u = new URL(location);
                System.out.println("opening connection to " + u);
                HttpsURLConnection c = (HttpsURLConnection) u.openConnection();
                String bytesRange = "bytes=" + pr.floorPosition() + "-" + pr.ceilingPosition();
                c.addRequestProperty("Range", bytesRange);
                final int records;
                try (InputStream in = c.getInputStream()) {
                    records = read(ser, ind, sb, pr, in, hc);
                }
                System.out.println("found " + records + " in " + (System.currentTimeMillis() - t) + "ms");
                assertEquals(1130, records);
            }
        }
    }

    private static Bounds createSydneyBounds(long minTime, long maxTime) {
        float lat1 = -33.806477f;
        float lon1 = 151.181767f;
        long t1 = Math.round(minTime + (maxTime - minTime) / 2);
        float lat2 = -33.882896f;
        float lon2 = 151.281330f;
        long t2 = t1 + TimeUnit.HOURS.toMillis(1);
        return new Bounds(new double[] { lat1, lon1, t1 }, new double[] { lat2, lon2, t2 });
    }

    private static InputStream getIndexInputStream(String indexUrl) throws IOException {
        URL u = new URL(indexUrl);
        HttpsURLConnection c = (HttpsURLConnection) u.openConnection();
        return c.getInputStream();
    }

    private static int read(Serializer<byte[]> ser, Index ind, Bounds searchBounds, PositionRange pr, InputStream in,
            SmallHilbertCurve hc) throws IOException {
        System.out.println("reading from url inputstream");
        Reader<byte[]> r = ser.createReader(in);
        byte[] b;
        int records = 0;
        long total = 0;
        while ((b = r.read()) != null) {
            Record rec = FixesSortMain2.getRecord(b);
            // System.out.println(rec);
            // check is in bounding box
            if (searchBounds.contains(rec.toArray())) {
                records++;
            }
            total++;
            long[] p = ind.ordinates(rec.lat, rec.lon, rec.time);
            long ix = ind.hilbertCurve().index(p);
            if (ix > pr.highIndex()) {
                System.out.println("hit max index");
                break;
            }
        }
        System.out.println("read " + total + " records");
        return records;
    }

    static TreeMap<Integer, Long> readAndPrintIndex(File idx) throws IOException, FileNotFoundException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(idx))) {
            dis.skip(4 + 4 + 6 * 8);
            int numEntries = dis.readInt();
            dis.skip(4);
            TreeMap<Integer, Long> tree = new TreeMap<>();
            for (int i = 0; i < numEntries; i++) {
                int position = dis.readInt();
                int index = dis.readInt();
                tree.put(index, (long) position);
            }
            return tree;
        }
    }

    private static void writeIdx(int recordSize, int count, Bounds extremes, SmallHilbertCurve hc,
            Serializer<byte[]> ser, File output, int numIndexEntries, File idx)
            throws IOException, FileNotFoundException {
        long chunk = count / numIndexEntries;
        int numIndexes = 0;
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(idx)))) {
            dos.writeInt(hc.bits());
            dos.writeInt(hc.dimensions());
            dos.writeDouble(extremes.mins()[0]);
            dos.writeDouble(extremes.maxes()[0]);
            dos.writeDouble(extremes.mins()[1]);
            dos.writeDouble(extremes.maxes()[1]);
            dos.writeDouble(extremes.mins()[2]);
            dos.writeDouble(extremes.maxes()[2]);

            // num index entries
            dos.writeInt(0);

            // write 0 for int position
            // write 1 for long position
            dos.writeInt(0);

            // write index entries
            try (InputStream sorted = new BufferedInputStream(new FileInputStream(output))) {
                Reader<byte[]> r = ser.createReader(sorted);
                long i = 0;
                long position = 0;
                int index = 0;
                for (;;) {
                    byte[] b = r.read();
                    if (b == null) {
                        if (i != 0 && position > 0) {
                            // write the last position if not already written
                            dos.writeInt((int) position - recordSize);
                            dos.writeInt(index);
                            numIndexes += 1;
                        }
                        break;
                    }
                    index = getIndex(b, extremes, hc);
                    if (i % chunk == 0) {
                        dos.writeInt((int) position);
                        dos.writeInt(index);
                        numIndexes += 1;
                        i = 0;
                    }
                    i++;
                    position += recordSize;
                }
            }
        }

        // overwrite numIndexes with actual value
        try (RandomAccessFile rf = new RandomAccessFile(idx, "rw")) {
            rf.seek(4 + 4 + 8 * 6);
            rf.writeInt(numIndexes);
        }
    }

    private static GZIPInputStream getInputStream(File input) throws IOException, FileNotFoundException {
        return new GZIPInputStream(new FileInputStream(input));
    }

    private static void printStartOfSortedIndexes(Bounds extremes, SmallHilbertCurve hc, Serializer<byte[]> ser,
            File output) throws FileNotFoundException, IOException {
        {
            System.out.println("first 10 hilbert curve indexes of output:");
            Reader<byte[]> r = ser.createReader(new FileInputStream(output));
            for (int i = 0; i < 10; i++) {
                byte[] b = r.read();
                int index = getIndex(b, extremes, hc);
                System.out.println(index);
            }
        }
    }

    static final class Record {
        final float lat;
        final float lon;
        final long time;

        Record(float lat, float lon, long time) {
            this.lat = lat;
            this.lon = lon;
            this.time = time;
        }

        double[] toArray() {
            return new double[] { lat, lon, time };
        }

        @Override
        public String toString() {
            return "Record [lat=" + lat + ", lon=" + lon + ", time=" + time + "]";
        }
    }

    static Record getRecord(byte[] x) {
        ByteBuffer bb = ByteBuffer.wrap(x);
        // skip mmsi
        bb.position(4);
        float lat = bb.getFloat();
        float lon = bb.getFloat();
        long t = bb.getLong();
        return new Record(lat, lon, t);
    }

    private static int getIndex(byte[] x, Bounds e, SmallHilbertCurve hc) {
        ByteBuffer bb = ByteBuffer.wrap(x);
        // skip mmsi
        bb.position(4);
        float lat = bb.getFloat();
        float lon = bb.getFloat();
        long t = bb.getLong();
        long a = Math.round(((lat - e.mins()[0]) / (e.maxes()[0] - e.mins()[0])) * hc.maxOrdinate());
        long b = Math.round(((lon - e.mins()[1]) / (e.maxes()[1] - e.mins()[1])) * hc.maxOrdinate());
        long c = Math
                .round((((double) (t - e.mins()[2])) / ((double) (e.maxes()[2] - e.mins()[2]))) * hc.maxOrdinate());
        return (int) hc.index(a, b, c);
    }

    public static byte[] intToBytes(int value) {
        return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value };
    }

}
