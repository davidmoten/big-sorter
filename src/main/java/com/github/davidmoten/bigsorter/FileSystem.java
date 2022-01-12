package com.github.davidmoten.bigsorter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.github.davidmoten.bigsorter.internal.FileSystemDisk;
import com.github.davidmoten.bigsorter.internal.FileSystemS3;

public interface FileSystem {

	File nextTempFile(File directory) throws IOException;

	OutputStream outputStream(File file) throws IOException;

	InputStream inputStream(File file) throws IOException;

	void move(File a, File b) throws IOException;

	void delete(File file) throws IOException;

	void mkdirs(File directory);

	File defaultTempDirectory();

	/**
	 * Only called if tempDirectory was not specified explicitly by user.
	 * {@link FileSystemDisk} would probably do nothing because it default to using
	 * java.tmp.io directory and we don't want to delete that. {@link FileSystemS3}
	 * would probably attempt to delete a temporary bucket just used for a single
	 * sort though.
	 * 
	 * @param tempDirectory the temporary directory used for intermediate results of
	 *                      sorting (this method only called if temp directory was
	 *                      not specified explicitly by user)
	 */
	void finished(File tempDirectory);

	public static final FileSystem DISK = new FileSystemDisk();

}
