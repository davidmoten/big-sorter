package com.github.davidmoten.bigsorter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.apache.commons.csv.CSVFormat;

public class StackOverflowMain {

    public static void main(String[] args) throws FileNotFoundException {
        long t = System.currentTimeMillis();
        File input = new File("target/input.csv");
        try (PrintStream p = new PrintStream(
                new BufferedOutputStream(new FileOutputStream(input)))) {
            Random r = new Random();
            for (int i = 0; i < 12_000_000; i++) {
                for (int j = 0; j < 37; j++) {
                    if (j > 0) {
                        p.print(",");
                    }
                    p.print(r.nextInt() % 100000);
                }
                p.println();
            }
        }
        System.out.println((System.currentTimeMillis() - t) / 1000.0 + "s");
        Sorter.serializer(Serializer.csv(CSVFormat.DEFAULT.withRecordSeparator('\n'), //
                StandardCharsets.UTF_8)) //
                .comparator((x, y) -> Integer.compare(Integer.parseInt(x.get(10)),
                        Integer.parseInt(y.get(10)))) //
                .input(input) //
                .output(new File("target/output.txt"));
    }

}
