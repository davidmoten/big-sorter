package com.github.davidmoten.bigsorter;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Collectors;

import org.junit.Test;

public class SorterTest {

	@Test
	public void test() throws IOException {
		assertEquals("1234", sort("1432"));
	}
	
	@Test
	public void testEmpty() throws IOException {
		assertEquals("", sort(""));
	}
	
	@Test
	public void testOne() throws IOException {
		assertEquals("1", sort("1"));
	}
	
	@Test
	public void testTwo() throws IOException {
		assertEquals("12", sort("21"));
	}

	@Test
	public void testThree() throws IOException {
		assertEquals("123", sort("231"));
	}
	
	@Test
	public void testFour() throws IOException {
		assertEquals("1234", sort("2431"));
	}
	
	private String sort(String s) throws IOException {
		File f = new File("target/temp.txt");
		writeStringToFile(s, f);
		Serializer<Character> serializer = createCharacterSerializer();
		File output = new File("target/out.txt");
		Sorter<Character> sorter = new Sorter<Character>(f, serializer, output, (x, y) -> Character.compare(x, y), 2,
				3);
		sorter.sort();
		return Files.readAllLines(output.toPath()).stream().collect(Collectors.joining("\n"));
	}

	private static void writeStringToFile(String s, File f) throws FileNotFoundException {
		try (PrintStream out = new PrintStream(f)) {
			out.print(s);
		}
	}

	private static Serializer<Character> createCharacterSerializer() {
		Serializer<Character> serializer = new Serializer<Character>() {

			@Override
			public Reader<Character> createReader(InputStream in) {
				return new Reader<Character>() {

					java.io.Reader r = new InputStreamReader(in, StandardCharsets.UTF_8);

					@Override
					public Character read() throws IOException {
						int c = r.read();
						if (c == -1) {
							return null;
						} else {
							return Character.valueOf((char) c);
						}
					}

					@Override
					public void close() throws IOException {
						r.close();
					}

				};
			}

			@Override
			public Writer<Character> createWriter(OutputStream out) {
				return new Writer<Character>() {

					java.io.Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8);

					@Override
					public void write(Character value) throws IOException {
						w.write((int) value.charValue());
					}

					@Override
					public void close() throws IOException {
						w.close();
					}
				};
			}

		};
		return serializer;
	}

}
