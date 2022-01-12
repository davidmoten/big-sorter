package com.github.davidmoten.bigsorter.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import com.github.davidmoten.aws.lw.client.Client;
import com.github.davidmoten.aws.lw.client.HttpMethod;
import com.github.davidmoten.aws.lw.client.Request;
import com.github.davidmoten.aws.lw.client.Response;

public final class FileSystemS3Test {

	@Test
	public void mkdirsWhenDoesNotExist() {
		Client s3 = Mockito.mock(Client.class);
		Request request1a = Mockito.mock(Request.class);
		Request request1b = Mockito.mock(Request.class);
		Response response = Mockito.mock(Response.class);
		Request request2a = Mockito.mock(Request.class);
		Request request2b = Mockito.mock(Request.class);
		Request request2c = Mockito.mock(Request.class);
		Mockito.when(s3.path("temp")).thenReturn(request1a, request2a);
		Mockito.when(request1a.query("location")).thenReturn(request1b);
		Mockito.when(request1b.response()).thenReturn(response);
		Mockito.when(response.statusCode()).thenReturn(404);
		Mockito.when(request2a.method(HttpMethod.PUT)).thenReturn(request2b);
		Mockito.when(request2b.requestBody(Mockito.anyString())).thenReturn(request2c);
		Mockito.doNothing().when(request2c).execute();

		FileSystemS3 fs = new FileSystemS3(s3, "ap-southeast-2");
		fs.mkdirs(new File("temp"));
		InOrder o = Mockito.inOrder(s3, request1a, request1b, response, request2a, request2b, request2c);
		o.verify(s3).path("temp");
		o.verify(request1a).query("location");
		o.verify(request1b).response();
		o.verify(response).statusCode();
		o.verify(request2a).method(HttpMethod.PUT);
		o.verify(request2b).requestBody(Mockito.anyString());
		o.verify(request2c).execute();
		o.verifyNoMoreInteractions();
	}

	@Test
	public void mkdirsWhenDoesExist() {
		Client s3 = Mockito.mock(Client.class);
		Request request1a = Mockito.mock(Request.class);
		Request request1b = Mockito.mock(Request.class);
		Response response = Mockito.mock(Response.class);
		Mockito.when(s3.path("temp")).thenReturn(request1a);
		Mockito.when(request1a.query("location")).thenReturn(request1b);
		Mockito.when(request1b.response()).thenReturn(response);
		Mockito.when(response.statusCode()).thenReturn(200);

		FileSystemS3 fs = new FileSystemS3(s3, "ap-southeast-2");
		fs.mkdirs(new File("temp"));
		InOrder o = Mockito.inOrder(s3, request1a, request1b, response);
		o.verify(s3).path("temp");
		o.verify(request1a).query("location");
		o.verify(request1b).response();
		o.verify(response).statusCode();
		o.verifyNoMoreInteractions();
	}

	@Test
	public void testNextTempFile() throws IOException {
		Client s3 = Mockito.mock(Client.class);
		FileSystemS3 fs = new FileSystemS3(s3, "ap-southeast-2");
		File f = fs.nextTempFile(new File("temp"));
		assertTrue(f.getName().startsWith("big-sorter"));
		assertEquals("temp", f.getParentFile().getName());
	}

}
