package com.github.davidmoten.bigsorter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Random;

public class SortIntegersMain {

    public static void main(String[] args) throws FileNotFoundException {
        File textInts = new File("target/ints.txt");
        Random r = new Random();
        try (PrintWriter out = new PrintWriter(textInts)) {
            for (int i = 0; i < 100000000; i++) {
                out.println(r.nextInt());
                if (i % 1000000 == 0) {
                    System.out.println("written " + i / 1000000.0 + "m");
                }
            }
        }

        System.out.println("file written");

        Serializer<Integer> intSerializer = Serializer.dataSerializer( //
                dis -> (Integer) dis.readInt(), //
                (dos, v) -> dos.writeInt(v));

        File output = new File("target/out");

        Sorter //
                .serializer(intSerializer) //
                .inputMapper(Serializer.linesUtf8(), line -> Integer.parseInt(line)) //
                .naturalOrder() //
                .input(textInts) //
                .output(output) //
                .outputMapper(Serializer.linesUtf8(), x -> Integer.toString(x)) //
                .loggerStdOut() //
                .initialSortInParallel() //
                .maxItemsPerFile(5_000_000) //
                .sort();

        Sorter //
                .serializerLinesUtf8() //
                .comparator((a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b))) //
                .input(textInts) //
                .filter(line -> !line.isEmpty()) //
                .output(output) //
                .loggerStdOut() //
                .initialSortInParallel() //
                .maxItemsPerFile(5_000_000) //
                .sort();

    }

}
