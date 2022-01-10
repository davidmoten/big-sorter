package com.github.davidmoten.bigsorter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.github.davidmoten.bigsorter.internal.FileSystemDisk;

public interface FileSystem {

	File nextTempFile(File directory) throws IOException;
	
	OutputStream outputStream(File file) throws IOException;
	
	InputStream inputStream(File file) throws IOException;
	
	void move(File a, File b) throws IOException;
	
	void delete(File file) throws IOException;

	void mkdirs(File directory);
	
	public static final FileSystem DISK = new FileSystemDisk();
	
}
