package com.github.davidmoten.bigsorter;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.zip.GZIPInputStream;

public class FixesSortMain {

	public static void main(String[] args) throws IOException {
		String filename = "2019-05-15.binary-fixes-with-mmsi.gz";
		File file = new File("/home/dxm/" + filename);
		DataInputStream dis = new DataInputStream(new GZIPInputStream(new FileInputStream(file)));
		for (int i = 0; i < 100; i++) {
			System.out.println(dis.readInt() + ",lat=" + dis.readFloat() + ",lon=" + dis.readFloat() + ", t="
					+ new Date(dis.readLong()));
			dis.skip(15);
		}

	}

}
