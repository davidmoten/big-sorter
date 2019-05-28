package com.github.davidmoten.bigsorter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

public class ConcurrentBlockingBufferedInputStream extends InputStream implements Runnable {

    private final InputStream in;
    private final int bufferSize;
    private final BlockingQueue<Object> q;// handles ByteBuffer and Throwable
    private ExecutorService executor;
    private ByteBuffer buffer;
    private static final ByteBuffer EMPTY = ByteBuffer.wrap(new byte[1]);

    public ConcurrentBlockingBufferedInputStream(InputStream in, int bufferSize, ExecutorService executor) {
        this.in = in;
        this.bufferSize = bufferSize;
        this.executor = executor;
        this.q = new ArrayBlockingQueue<Object>(2);
        this.executor = executor;
    }

    @Override
    public int read() throws IOException {
        if (buffer == null) {
            executor.execute(this);
            executor.execute(this);
        }
        if (buffer == null || !buffer.hasRemaining()) {
            Object o;
            try {
                o = q.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (o == EMPTY) {
                return -1;
            } else if (o instanceof Throwable) {
                if (o instanceof RuntimeException) {
                    throw (RuntimeException) o;
                } else {
                    throw new RuntimeException((Throwable) o);
                }
            } else {
                // read some more and add to queue
                executor.execute(this);
                buffer = (ByteBuffer) o;
                return buffer.get() & 0xff;
            }
        } else {
            return  buffer.get() & 0xff;
        }
    }

    @Override
    public void run() {
        try {
            byte[] b = new byte[bufferSize];
            int length = in.read(b);
            if (length == -1) {
                q.put(EMPTY);
            } else {
                q.put(ByteBuffer.wrap(b, 0, length));
            }
        } catch (Throwable t) {
            try {
                q.put(t);
            } catch (InterruptedException e) {
                // do nothing
            }
        }
    }

}
