package com.github.davidmoten.bigsorter;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.davidmoten.aws.lw.client.Client;

public class S3Main {

	public static void main(String[] args) {
		String accessKey = System.getProperty("accessKey");
		String secretKey = System.getProperty("secretKey");
		Client s3 = Client.s3().region("ap-southeast-2").accessKey(accessKey).secretKey(secretKey).build();
		Client iam = Client.iam().regionNone().accessKey(accessKey).secretKey(secretKey).build();
		String arn = iam //
				.query("Action", "GetUser") //
				.query("Version", "2010-05-08") //
				.responseAsXml() //
				.child("GetUserResult", "User", "Arn") //
				.content();
		int i = arn.indexOf("iam::") + 5;
		int j = arn.indexOf(":user/");
		String accountId = arn.substring(i, j);
		System.out.println(accountId);
		Stream<String> lines = Sorter //
				.linesUtf8() //
				.input("hello", "there", "about") //
				.outputAsStream() //
				.loggerStdOut() //
				.fileSystemS3(s3, "ap-southeast-2") //
//				.tempDirectory(new File("big-sorter-temp-" + accountId)) //
				.sort();
		System.out.println(lines.collect(Collectors.toList()));
		System.out.println("closing");
		lines.close();
		System.out.println("done");
	}

}
