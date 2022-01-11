package com.github.davidmoten.bigsorter.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import com.github.davidmoten.aws.lw.client.Client;
import com.github.davidmoten.aws.lw.client.HttpMethod;
import com.github.davidmoten.aws.lw.client.Multipart;
import com.github.davidmoten.aws.lw.client.ResponseInputStream;
import com.github.davidmoten.aws.lw.client.xml.builder.Xml;
import com.github.davidmoten.bigsorter.FileSystem;
import com.github.davidmoten.guavamini.Preconditions;

public final class FileSystemS3 implements FileSystem {

	private final Client s3;
	private final String region;

	public FileSystemS3(Client s3, String region) {
		this.s3 = s3;
		this.region = region;
	}
	
	@Override
	public File nextTempFile(File directory) throws IOException {
		Preconditions.checkNotNull(directory);
		return new File(directory, "big-sorter-" + UUID.randomUUID().toString().replace("-", ""));
	}

	@Override
	public OutputStream outputStream(File file) throws IOException {
		System.out.println(file + " " + file.getParentFile());
		return Multipart //
				.s3(s3)//
				.bucket(file.getParentFile().getName()) //
				.key(file.getName()) //
				.outputStream();
	}

	@Override
	public InputStream inputStream(File file) throws IOException {
		ResponseInputStream r = s3 //
				.path(file.getParentFile().getName(), file.getName()) //
				.responseInputStream();
		if (r.statusCode() / 100 == 2) {
			return r;
		} else {
			throw new IOException("statusCode=" + r.statusCode());
		}
	}

	@Override
	public void move(File a, File b) throws IOException {
		try (InputStream in = inputStream(a); OutputStream out = outputStream(b)) {
			copy(in, out);
		}
	}

	private static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[8192];
		int n;
		while ((n = in.read(buffer)) != -1) {
			out.write(buffer, 0, n);
		}
	}

	@Override
	public void delete(File file) throws IOException {
		s3.path(file.getParentFile().getName(), file.getName()) //
				.method(HttpMethod.DELETE) //
				.execute();
	}

	@Override
	public void mkdirs(File directory) {
		String xml = Xml //
				.create("CreateBucketConfiguration") //
				.a("xmlns", "http://s3.amazonaws.com/doc/2006-03-01/") //
				.e("LocationConstraint") //
				.content(region) //
				.toString();
		s3 //
				.path(directory.getName()) //
				.method(HttpMethod.PUT) //
				.requestBody(xml) //
				.execute();
	}

	@Override
	public File defaultTempDirectory() {
		return new File("big-sorter-temp-"+ UUID.randomUUID().toString().replace("-",""));
	}

}
