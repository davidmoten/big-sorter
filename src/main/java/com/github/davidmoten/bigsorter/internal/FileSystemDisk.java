package com.github.davidmoten.bigsorter.internal;

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

import com.github.davidmoten.bigsorter.FileSystem;

public final class FileSystemDisk implements FileSystem {
	
	private static final int BUFFER_SIZE = 8192;
	
	@Override
	public File nextTempFile(File directory) throws IOException {
		return File.createTempFile("big-sorter", "", directory);
	}

	@Override
	public OutputStream outputStream(File file) throws FileNotFoundException {
		return new BufferedOutputStream(new FileOutputStream(file), BUFFER_SIZE);
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
		return new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE);
	}

	@Override
	public void delete(File file) throws IOException {
		file.delete();
	}

	@Override
	public void mkdirs(File directory) {
		directory.mkdirs();
	}

	@Override
	public File defaultTempDirectory() {
		return new File(System.getProperty("java.io.tmpdir"));
	}

}
