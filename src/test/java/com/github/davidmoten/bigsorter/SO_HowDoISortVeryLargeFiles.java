package com.github.davidmoten.bigsorter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * <a href=
 * "https://stackoverflow.com/questions/7918060/how-do-i-sort-very-large-files">question</a>
 */
public class SO_HowDoISortVeryLargeFiles {

    public static void main(String[] args) throws IOException {

        String s = "0052304 0000004000000000000000000000000000000041   John Teddy   000023\n"
                + "0022024 0000004000000000000000000000000000000041   George Clan 00013";
        File input = new File("target/input");
        Files.write(input.toPath(), s.getBytes(StandardCharsets.UTF_8));
        File output = new File("target/output");

        Sorter.serializerLinesUtf8() //
                .comparator((a, b) -> {
                    String ida = a.substring(0, a.indexOf(' '));
                    String idb = b.substring(0, b.indexOf(' '));
                    return ida.compareTo(idb);
                }) //
                .input(input) //
                .output(output) //
                .sort();

        Files.readAllLines(output.toPath()).forEach(System.out::println);
    }

}
