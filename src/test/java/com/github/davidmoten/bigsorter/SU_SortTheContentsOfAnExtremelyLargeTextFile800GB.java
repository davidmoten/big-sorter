package com.github.davidmoten.bigsorter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * <a href=
 * "https://superuser.com/questions/1300361/sort-the-contents-of-an-extremely-large-800gb-text-file-on-windows/1441113#1441113">question</a>
 */
public class SU_SortTheContentsOfAnExtremelyLargeTextFile800GB {

    public static void main(String[] args) throws IOException {
        byte[] newLine = "\n".getBytes(StandardCharsets.UTF_8);
        File input = new File("target/input");
        long n = 10_000_000;
        try (OutputStream p = new BufferedOutputStream(new FileOutputStream(input))) {
            for (long i = 0; i < n; i++) {
                p.write(UUID.randomUUID().toString().substring(0, 16).getBytes(StandardCharsets.UTF_8));
                p.write(newLine);
            }
        }
        System.out.println("input file size = " + input.length());
        long t = System.currentTimeMillis();
        File output = new File("target/output");
        Sorter.linesUtf8() //
                .input(input) //
                .output(output) //
                .loggerStdOut() //
                .sort();
        t = System.currentTimeMillis() - t;
        // each line is 17 bytes
        // so numlines = 800GB / 17
        long numLines = 800000000000L / 17;
        // calculate time to sort numLines
        double k = ((double) t) / n / Math.log(n);
        long t2 = Math.round(k * numLines * Math.log(numLines));
        System.out.println("to sort 800GB would be " + t2 / 1000 / 3600 + " hours");

        input.delete();
        output.delete();

    }
}
