package com.simiacryptus.skyenet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    private static final ThreadLocal<ByteArrayOutputStream> threadLocalBuffer = new ThreadLocal<ByteArrayOutputStream>() {
        @Override
        protected ByteArrayOutputStream initialValue() {
            return new ByteArrayOutputStream();
        }
    };

    private static PrintStream createInterceptorStream(PrintStream originalStream) {
        return new PrintStream(new ByteArrayOutputStream() {
            @Override
            public void write(int b) {
                originalStream.write(b);
                threadLocalBuffer.get().write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) {
                originalStream.write(b, off, len);
                threadLocalBuffer.get().write(b, off, len);
            }
        });
    }

    public static String getThreadOutput() {
        return threadLocalBuffer.get().toString();
    }

    public static void clearThreadOutput() {
        threadLocalBuffer.get().reset();
    }
}
