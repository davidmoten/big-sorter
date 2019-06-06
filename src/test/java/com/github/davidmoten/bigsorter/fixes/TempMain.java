package com.github.davidmoten.bigsorter.fixes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import org.davidmoten.hilbert.Ranges;

import com.github.davidmoten.bigsorter.Reader;
import com.github.davidmoten.bigsorter.Serializer;
import com.github.davidmoten.bigsorter.fixes.FixesSortMain2.Record;

public class TempMain {
    public static void main(String[] args) throws FileNotFoundException, IOException {
        File idx = new File("target/output-sorted.idx");
        Index ind = Index.read(new FileInputStream(idx));
        double[] mins = ind.mins();
        double[] maxes = ind.maxes();
        Serializer<byte[]> ser = Serializer.fixedSizeRecord(35);
        {
            long t = System.currentTimeMillis();
            // sydney box
            float lat1 = -33.806477f;
            float lon1 = 151.181767f;
            long t1 = Math.round(mins[2] + (maxes[2] - mins[2]) / 2);
            float lat2 = -33.882896f;
            float lon2 = 151.281330f;
            long t2 = t1 + TimeUnit.HOURS.toMillis(1);

            long[] o1 = ind.ordinates(new double[] { lat1, lon1, t1 });
            long[] o2 = ind.ordinates(new double[] { lat2, lon2, t2 });
            Ranges ranges = ind.hilbertCurve().query(o1, o2);
            List<PositionRange> positionRanges = ind.getPositionRanges(ranges);
            positionRanges.stream().forEach(System.out::println);
            PositionRange pr = positionRanges.get(0);
            
            // obfuscated urls for the brief period I'm using unauthenticated access
            String location = new String(
                    Base64.getDecoder().decode(
                            "aHR0cHM6Ly9tb3Rlbi1maXhlcy5zMy1hcC1zb3V0aGVhc3QtMi5hbWF6b25hd3MuY29tL291dHB1dC1zb3J0ZWQK"),
                    StandardCharsets.UTF_8);

            String locationIdx = new String(Base64.getDecoder().decode(
                    "aHR0cHM6Ly9tb3Rlbi1maXhlcy5zMy1hcC1zb3V0aGVhc3QtMi5hbWF6b25hd3MuY29tL291dHB1dC1zb3J0ZWQuaWR4Cg=="),
                    StandardCharsets.UTF_8);

            URL u = new URL(location);
            System.out.println("opening connection to " + u);
            HttpsURLConnection c = (HttpsURLConnection) u.openConnection();
            c.addRequestProperty("Range", "bytes=" + pr.floorPosition() + "-" + pr.ceilingPosition());
            int records = 0;
            System.out.println("getting inputstream");
            try (InputStream in = c.getInputStream()) {
                System.out.println("reading from url inputstream");
                Reader<byte[]> r = ser.createReader(in);
                byte[] b;
                while ((b = r.read()) != null) {
                    Record rec = FixesSortMain2.getRecord(b);
                    System.out.println(rec);
                    // check is in bounding box
                    if (rec.lat >= lat2 && rec.lat < lat1 && rec.lon >= lon1 && rec.lon < lon2 && rec.time >= t1
                            && rec.time < t2) {
                        records++;
                    } 
                    long[] p = ind.ordinates(rec.lat, rec.lon, rec.time);
                    long ix = ind.hilbertCurve().index(p);
                    System.out.println("compare "+ ix + " to "+ pr.highIndex());
//                    if (ix > pr.highIndex()) {
//                        System.out.println("hit max index");
//                        break;
//                    }
                }
            }
            System.out.println("read " + records + " in " + (System.currentTimeMillis() - t) + "ms");
        }
    }
}
