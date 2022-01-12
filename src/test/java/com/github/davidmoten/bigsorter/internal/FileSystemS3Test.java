package com.github.davidmoten.bigsorter.internal;

import java.io.File;

import org.junit.Test;
import org.mockito.Mockito;

import com.github.davidmoten.aws.lw.client.Client;
import com.github.davidmoten.aws.lw.client.HttpMethod;
import com.github.davidmoten.aws.lw.client.Request;
import com.github.davidmoten.aws.lw.client.Response;

public final class FileSystemS3Test {

	@Test
	@Ignore
	public void mkdirs() {
		Client client = Mockito.mock(Client.class);
		Request request1 = Mockito.mock(Request.class);
		Response response1 = Mockito.mock(Response.class);
		Request request2 = Mockito.mock(Request.class);
		Mockito.when(client.path("temp")).thenReturn(request1, request2);
		Mockito.when(request1.query("location")).thenReturn(request1);
		Mockito.when(request1.response()).thenReturn(response1);
		Mockito.when(response1.statusCode()).thenReturn(404);
		Mockito.when(request2.method(HttpMethod.PUT)).thenReturn(request2);
		Mockito.when(request2.requestBody(Mockito.anyString())).thenReturn(request2);
		Mockito.doNothing().when(request2).execute();

		FileSystemS3 fs = new FileSystemS3(client, "ap-southeast-2");
		fs.mkdirs(new File("temp"));
		Mockito.verifyNoMoreInteractions(request1, response1, request2);
	}

}
