package com.github.davidmoten.bigsorter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public final class Sorter<T> {

	private final File file;
	private final Serializer<T> serializer;
	private final File output;
	private final long maxFileSize;
	private final Comparator<T> comparator;
	private final int maxFilesPerMerge;
	private final int maxItemsPerPart;

	public Sorter(File file, Serializer<T> serializer, File output, long maxFileSize, Comparator<T> comparator,
			int maxFilesPerMerge, int maxItemsPerPart) {
		this.file = file;
		this.serializer = serializer;
		this.output = output;
		this.maxFileSize = maxFileSize;
		this.comparator = comparator;
		this.maxFilesPerMerge = maxFilesPerMerge;
		this.maxItemsPerPart = maxItemsPerPart;
	}

	private void sort() {
		try {
			Deque<File> stack = new LinkedList<>();
			stack.offer(file);
			List<File> list = new ArrayList<>(maxFilesPerMerge);
			while (!stack.isEmpty()) {
				File file = stack.poll();
				if (file.length() <= maxFileSize) {
					list.add(sortInMemory(file));
				} else {
					List<T> chunk = new ArrayList<>();
					try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
						{
							T t = null;
							while ((t = serializer.read(in)) != null) {
								chunk.add(t);
								if (chunk.size == maxItemsPerPart) {
									list.add
								}
							}
						}
					}
					
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private File sortAndWriteToFile(List<T> list) throws FileNotFoundException, IOException {
		Collections.sort(list, comparator);
		File file = nextTempFile();
		writeToFile(list, file);
		return file;
	}

	private void writeToFile(List<T> list, File f) throws FileNotFoundException, IOException {
		try (OutputStream out = new BufferedOutputStream(new FileOutputStream(f))) {
			for (T t : list) {
				serializer.write(out, t);
			}
		}
	}

	private File nextTempFile() throws IOException {
		return File.createTempFile("big-sorter", ".bin");
	}

	private File sortInMemory(File f) throws FileNotFoundException, IOException {
		List<T> list = new ArrayList<>();
		try (InputStream in = new BufferedInputStream(new FileInputStream(f));) {
			{
				T t = null;
				while ((t = serializer.read(in)) != null) {
					list.add(t);
				}
			}
			return sortAndWriteToFile(list);
		}
	}
}
