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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class FileSystemDisk implements FileSystem {
	
	private final int bufferSize;

	public FileSystemDisk(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	@Override
	public File nextTempFile(File directory) throws IOException {
		return File.createTempFile("big-sorter", "", directory);
	}

	@Override
	public OutputStream outputStream(File file) throws FileNotFoundException {
		return new BufferedOutputStream(new FileOutputStream(file), bufferSize);
	}

	@Override
	public void move(File a, File b) throws IOException {
		Files.move( //
                a.toPath(), //
                b.toPath(), //
                StandardCopyOption.ATOMIC_MOVE, //
                StandardCopyOption.REPLACE_EXISTING);		
	}

	@Override
	public InputStream inputStream(File file) throws IOException {
		return new BufferedInputStream(new FileInputStream(file), bufferSize);
	}

}
