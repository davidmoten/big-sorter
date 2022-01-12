package com.github.davidmoten.bigsorter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.github.davidmoten.bigsorter.internal.FileSystemDisk;
import com.github.davidmoten.bigsorter.internal.FileSystemS3;

/**
 * An abstraction of a file system that may be implemented over a standard disk
 * file system or for example over a cloud storage service like AWS S3.
 */
public interface FileSystem {

	/**
	 * Returns a new unique file (for use as a temporary file for the sorting
	 * process).
	 * 
	 * @param directory directory to create the file in (might correspond to a
	 *                  bucket for an S3 file system)
	 * @return new File object
	 * @throws IOException
	 */
	File nextTempFile(File directory) throws IOException;

	/**
	 * Returns an {@link OutputStream} to overwrite the contents of {@code file}.
	 * 
	 * @param file file to overwrite
	 * @return output stream
	 * @throws IOException
	 */
	OutputStream outputStream(File file) throws IOException;

	/**
	 * Returns an {@link InputStream} to read the contents of {@code file}.
	 * 
	 * @param file file to be read
	 * @return input stream
	 * @throws IOException
	 */
	InputStream inputStream(File file) throws IOException;

	/**
	 * Moves file a to file b. For S3 this might correspond to writing the contents
	 * of a to b then deleting a. For {@link FileSystemDisk} may be an atomic move.
	 * 
	 * @param a source file
	 * @param b destination file
	 * @throws IOException
	 */
	void move(File a, File b) throws IOException;

	/**
	 * Deletes a file (not a directory/bucket) if it exists.
	 * 
	 * @param file file to delete
	 * @throws IOException
	 */
	void delete(File file) throws IOException;

	/**
	 * Creates a directory and its parent directories if required. For
	 * {@link FileSystemS3} a parent-less file would be passed and the filename
	 * would correspond to the bucket to be created.
	 * 
	 * @param directory directory to be created
	 */
	void mkdirs(File directory);

	/**
	 * Returns the temp directory to be used if not explicitly specified by the user
	 * when building the sorter. For {@link FileSystemDisk} would likely be the
	 * directory given by the system property {@code java.io.tmp}. For
	 * {@link FileSystemS3} would likely be a newly created S3 bucket (using a UUID
	 * in the name to ensure uniqueness).
	 * 
	 * @return temp directory
	 */
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

	public static final FileSystem DISK = FileSystemDisk.INSTANCE;

}
