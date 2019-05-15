package com.github.davidmoten.bigsorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.junit.Test;

public class SorterTest {

	private static final File OUTPUT = new File("target/out.txt");

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

	@Test
	public void testDuplicatesPreserved() throws IOException {
		assertEquals("122234", sort("242312"));
	}

	@Test
	public void testLines() throws IOException {
		assertEquals("ab\nc\ndef", sortLines("c\ndef\nab"));
	}

	@Test
	public void testJavaSerializer() throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		Writer<Serializable> writer = Serializer.java().createWriter(bytes);
		writer.write(Long.valueOf(3));
		writer.write(Long.valueOf(1));
		writer.write(Long.valueOf(2));
		writer.close();

		InputStream in = new ByteArrayInputStream(bytes.toByteArray());
		Sorter //
				.serializer(Serializer.<Long>java()) //
				.comparator(Comparator.naturalOrder()) //
				.input(in) //
				.output(OUTPUT) //
				.sort();

		Reader<Long> reader = Serializer.<Long>java().createReader(new FileInputStream(OUTPUT));
		assertEquals(1, (long) reader.read());
		assertEquals(2, (long) reader.read());
		assertEquals(3, (long) reader.read());
		assertNull(reader.read());
	}

	private static String sortLines(String s) throws IOException {
		Sorter //
				.serializerTextUtf8() //
				.input(s) //
				.maxFilesPerMerge(3) //
				.maxItemsPerFile(2) //
				.output(OUTPUT) //
				.sort();

		return Files.readAllLines(OUTPUT.toPath()).stream().collect(Collectors.joining("\n"));
	}

	private static String sort(String s) throws IOException {
		File f = new File("target/temp.txt");
		writeStringToFile(s, f);
		Serializer<Character> serializer = createCharacterSerializer();
		Sorter //
				.serializer(serializer) //
				.comparator((x, y) -> Character.compare(x, y)) //
				.input(f) //
				.maxFilesPerMerge(3) //
				.maxItemsPerFile(2) //
				.output(OUTPUT) //
				.sort();

		return Files.readAllLines(OUTPUT.toPath()).stream().collect(Collectors.joining("\n"));
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
