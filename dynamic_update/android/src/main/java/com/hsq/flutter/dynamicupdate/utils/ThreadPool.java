package com.hsq.flutter.dynamicupdate.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by heshiqi on 18/3/16.
 */
public class ThreadPool {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService THREAD_POOL;
    private static ThreadPool sThreadPool;

    private ThreadPool() {
    }

    public static ThreadPool getInstance() {
        return sThreadPool;
    }

    public void execute(Runnable r) {
        THREAD_POOL.execute(r);
    }

    static {
        THREAD_POOL = new ThreadPoolExecutor(CPU_COUNT, CPU_COUNT * 2, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue(CPU_COUNT * 4), new ThreadPoolExecutor.DiscardOldestPolicy());
        sThreadPool = new ThreadPool();
    }

}
