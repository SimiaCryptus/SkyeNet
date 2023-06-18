package com.simiacryptus.skyenet;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class OutputInterceptor {

    private OutputInterceptor() {
        // Prevent instantiation of the utility class
    }

    private static final PrintStream originalOut = System.out;
    private static final PrintStream originalErr = System.err;
    private static final AtomicBoolean isSetup = new AtomicBoolean(false);

    public static void setupInterceptor() {
        if (isSetup.getAndSet(true)) return;
        System.setOut(createInterceptorStream(originalOut));
        System.setErr(createInterceptorStream(originalErr));
    }

    private static final ByteArrayOutputStream centralStream = new ByteArrayOutputStream();
    private static final ThreadLocal<ByteArrayOutputStream> threadLocalBuffer = new ThreadLocal<ByteArrayOutputStream>() {
        @Override
        protected ByteArrayOutputStream initialValue() {
            return new ByteArrayOutputStream();
            //return centralStream;
        }
    };

    public static void initThreadOutputStream() {
        setOutputStream(new ByteArrayOutputStream());
    }

    public static void resetThreadOutputStream() {
        setOutputStream(centralStream);
    }

    public static void setOutputStream(ByteArrayOutputStream stream) {
        threadLocalBuffer.set(stream);
    }

    public static ByteArrayOutputStream getOutputStream() {
        return threadLocalBuffer.get();
    }

    public static PrintStream createInterceptorStream(PrintStream originalStream) {
        return new PrintStream(new ByteArrayOutputStream() {
            @Override
            public void write(int b) {
                originalStream.write(b);
                ByteArrayOutputStream stream = getOutputStream();
                if(stream.size() > 1024 * 1024) {
                    stream.reset();
                }
                stream.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) {
                originalStream.write(b, off, len);
                ByteArrayOutputStream stream = getOutputStream();
                if(stream.size() > 1024 * 1024) {
                    stream.reset();
                }
                stream.write(b, off, len);
            }
        });
    }

    public static String getThreadOutput() {
        return getOutputStream().toString();
    }

    public static void clearThreadOutput() {
        getOutputStream().reset();
    }
}
