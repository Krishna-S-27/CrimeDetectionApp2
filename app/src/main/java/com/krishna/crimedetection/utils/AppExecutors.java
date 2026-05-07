package com.krishna.crimedetection.utils;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class AppExecutors {
    private static final Executor IO = Executors.newSingleThreadExecutor();

    private AppExecutors() {}

    public static Executor io() {
        return IO;
    }
}