package com.github.davidmoten.bigsorter;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.davidmoten.aws.lw.client.Client;

public class S3Main {

	public static void main(String[] args) {
		String accessKey = System.getProperty("accessKey");
		String secretKey = System.getProperty("secretKey");
		Client s3 = Client.s3().region("ap-southeast-2").accessKey(accessKey).secretKey(secretKey).build();
		try (Stream<String> lines = Sorter.linesUtf8() //
				.input("hello", "there", "about") //
				.outputAsStream() //
				.loggerStdOut() //
				.fileSystemS3(s3, "ap-southeast-2") //
				.sort()) {
			System.out.println(lines.collect(Collectors.toList()));
		}
	}

}
