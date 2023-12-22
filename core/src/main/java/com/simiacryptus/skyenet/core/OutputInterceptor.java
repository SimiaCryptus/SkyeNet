package com.simiacryptus.skyenet.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class OutputInterceptor {

    private OutputInterceptor() {
        // Prevent instantiation of the utility class
    }

    private static final PrintStream originalOut = System.out;
    private static final PrintStream originalErr = System.err;
    private static final AtomicBoolean isSetup = new AtomicBoolean(false);
    private static final Object globalStreamLock = new Object();

    public static void setupInterceptor() {
        if (isSetup.getAndSet(true)) return;
        System.setOut(new PrintStream(new OutputStreamRouter(originalOut)));
        System.setErr(new PrintStream(new OutputStreamRouter(originalErr)));
    }

    private static final ByteArrayOutputStream globalStream = new ByteArrayOutputStream();

    private static final Map<Thread, ByteArrayOutputStream> threadLocalBuffer = new WeakHashMap<>();

    private static ByteArrayOutputStream getThreadOutputStream() {
        Thread currentThread = Thread.currentThread();
        ByteArrayOutputStream outputStream;
        synchronized (threadLocalBuffer) {
            if ((outputStream = threadLocalBuffer.get(currentThread)) != null) return outputStream;
            outputStream = new ByteArrayOutputStream();
            threadLocalBuffer.put(currentThread, outputStream);
        }
        return outputStream;
    }

    public static String getThreadOutput() {
        ByteArrayOutputStream outputStream = getThreadOutputStream();
        try {
            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outputStream.toString();
    }

    public static void clearThreadOutput() {
        getThreadOutputStream().reset();
    }

    public static String getGlobalOutput() {
        synchronized (globalStreamLock) {
            return globalStream.toString();
        }
    }

    public static void clearGlobalOutput() {
        synchronized (globalStreamLock) {
            globalStream.reset();
        }
    }

    private static class OutputStreamRouter extends ByteArrayOutputStream {
        private final PrintStream originalStream;
        int maxGlobalBuffer = 8 * 1024 * 1024;
        int maxThreadBuffer = 1024 * 1024;

        public OutputStreamRouter(PrintStream originalStream) {
            this.originalStream = originalStream;
        }

        @Override
        public void write(int b) {
            originalStream.write(b);
            synchronized (globalStreamLock) {
                if (globalStream.size() > maxGlobalBuffer) {
                    globalStream.reset();
                }
                globalStream.write(b);
            }
            ByteArrayOutputStream threadOutputStream = getThreadOutputStream();
            if (threadOutputStream.size() > maxThreadBuffer) {
                threadOutputStream.reset();
            }
            threadOutputStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            originalStream.write(b, off, len);
            synchronized (globalStreamLock) {
                if (globalStream.size() > maxGlobalBuffer) {
                    globalStream.reset();
                }
                globalStream.write(b, off, len);
            }
            ByteArrayOutputStream threadOutputStream = getThreadOutputStream();
            if (threadOutputStream.size() > maxThreadBuffer) {
                threadOutputStream.reset();
            }
            threadOutputStream.write(b, off, len);
        }
    }
}


