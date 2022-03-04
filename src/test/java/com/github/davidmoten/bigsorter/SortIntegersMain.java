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
                    System.out.println("written " + i/1000000.0 + "m");
                }
            }
        }
        
        System.out.println("file written");
        
        long t = System.currentTimeMillis();
        
        Serializer<Integer> intSerializer = Serializer.dataSerializer( //
                dis -> (Integer) dis.readInt(), //
                (dos, v) -> dos.writeInt(v));

        // convert input from text integers to 4 byte binary integers
        File ints = new File("target/numbers-integers");
        Util.convert(textInts, Serializer.linesUtf8(), ints, intSerializer, line -> Integer.parseInt(line));

        System.out.println("converted in " + (System.currentTimeMillis() - t)/1000.0 + "s");
        
        Sorter //
                .serializer(intSerializer) //
                .naturalOrder() //
                .input(ints) //
                .outputAsStream() //
                .loggerStdOut() //
                .sort() //
                .count();

        Sorter //
                .serializerLinesUtf8() //
                .comparator((a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b))) //
                .input(textInts) //
                .filter(line -> !line.isEmpty()) //
                .outputAsStream() //
                .loggerStdOut().sort() //
                .count();
    }

}
