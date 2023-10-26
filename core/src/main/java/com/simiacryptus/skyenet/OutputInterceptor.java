package com.simiacryptus.skyenet;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
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

    public static void initThreadOutputStream() {
        setOutputStream(new ByteArrayOutputStream());
    }

    public static void resetThreadOutputStream() {
        setOutputStream(centralStream);
    }

    private static final Map<Thread, ByteArrayOutputStream> threadLocalBuffer = new HashMap<>();

    public static void setOutputStream(ByteArrayOutputStream stream) {
        threadLocalBuffer.put(Thread.currentThread(), stream);
    }

    public static ByteArrayOutputStream getOutputStream() {
        return threadLocalBuffer.get(Thread.currentThread());
    }

    public static String getThreadOutput() {
        return getOutputStream().toString();
    }

    public static void clearThreadOutput() {
        getOutputStream().reset();
    }

    public static String getGlobalOutput() {
        return centralStream.toString();
    }

    public static void clearGlobalOutput() {
        centralStream.reset();
    }

    public static PrintStream createInterceptorStream(PrintStream originalStream) {
        int maxGlobalBuffer = 8 * 1024 * 1024;
        int maxThreadBuffer = 1024 * 1024;
        return new PrintStream(new ByteArrayOutputStream() {
            @Override
            public void write(int b) {
                originalStream.write(b);
                if(centralStream.size() > maxGlobalBuffer) {
                    centralStream.reset();
                }
                centralStream.write(b);
                ByteArrayOutputStream stream = getOutputStream();
                if(stream.size() > maxThreadBuffer) {
                    stream.reset();
                }
                stream.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) {
                originalStream.write(b, off, len);
                if(centralStream.size() > 1024 * 1024) {
                    centralStream.reset();
                }
                centralStream.write(b, off, len);
                ByteArrayOutputStream stream = getOutputStream();
                if(stream.size() > 1024 * 1024) {
                    stream.reset();
                }
                stream.write(b, off, len);
            }
        });
    }

}
