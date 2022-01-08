package com.github.davidmoten.bigsorter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface FileSystem {

	File nextTempFile(File directory) throws IOException;
	
	OutputStream outputStream(File file) throws IOException;
	
	InputStream inputStream(File file) throws IOException;
	
	void move(File a, File b) throws IOException;
	
	void delete(File file) throws IOException;
	
}
