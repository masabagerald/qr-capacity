package com.dissertation.qrcapacity.services;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.util.function.Supplier;

public final class QRProfiler {

    private static final OperatingSystemMXBean osBean =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    private QRProfiler() { /* util class */ }

    /** Simple container for measured stats. */
    public static class QRStats {
        public final double timeMs;               // wall time (ms)
        public final double cpuPercent;           // CPU% across all cores
        public final double cpuPercentPerCore;    // normalized per-core %
        public final double memDeltaMB;           // memory delta (MB)
        public final double cpuTimeMs;            // CPU time used (ms)

        public QRStats(double timeMs, double cpuPercent, double cpuPercentPerCore,
                       double memDeltaMB, double cpuTimeMs) {
            this.timeMs = timeMs;
            this.cpuPercent = cpuPercent;
            this.cpuPercentPerCore = cpuPercentPerCore;
            this.memDeltaMB = memDeltaMB;
            this.cpuTimeMs = cpuTimeMs;
        }

        @Override
        public String toString() {
            return String.format("Time: %.3f ms | CPU: %.2f%% (%.2f%%/core) | CPU time: %.3f ms | Mem Δ: %.3f MB",
                    timeMs, cpuPercent, cpuPercentPerCore, cpuTimeMs, memDeltaMB);
        }
    }

    /** Generic container: result value + stats. */
    public static class ResultWithStats<T> {
        public final T result;
        public final QRStats stats;
        public ResultWithStats(T result, QRStats stats) {
            this.result = result;
            this.stats = stats;
        }
    }

    // Returns used heap memory in MB
    private static double usedMemoryMB() {
        long usedBytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        return usedBytes / (1024.0 * 1024.0);
    }

    // Returns process CPU time in nanoseconds (may be 0 on unsupported platforms)
    private static long processCpuTimeNs() {
        try {
            return osBean.getProcessCpuTime();
        } catch (UnsupportedOperationException e) {
            return 0L;
        }
    }

    /**
     * Profiles a Supplier<T> that returns a result (e.g., a decode method).
     * Captures wall-clock time, process CPU time and heap memory delta.
     *
     * Important:
     *  - For short operations, use multiple repeated trials and average results.
     *  - cpuPercent = (cpuTimeNs / wallTimeNs) * 100 (may exceed 100 if multiple cores used)
     *  - cpuPercentPerCore = cpuPercent / availableProcessors (approx. 0..100)
     *
     * @param supplier operation that returns T (may throw runtime exceptions)
     * @param <T> return type
     * @return ResultWithStats containing supplier result and QRStats
     */
    public static <T> ResultWithStats<T> profileSupplier(Supplier<T> supplier) {
        // optional small sleep to avoid -1 or unstable first CPU readings on some JVMs
        try { Thread.sleep(5); } catch (InterruptedException ignored) {}

        // Warm up memory reading (optional)
        System.gc();
        try { Thread.sleep(5); } catch (InterruptedException ignored) {}

        double memBefore = usedMemoryMB();
        long cpuStartNs = processCpuTimeNs();
        long wallStartNs = System.nanoTime();

        // Execute operation
        T result = supplier.get();

        long wallEndNs = System.nanoTime();
        long cpuEndNs = processCpuTimeNs();
        double memAfter = usedMemoryMB();

        long wallElapsedNs = Math.max(1L, wallEndNs - wallStartNs); // avoid division by zero
        long cpuTimeNs = Math.max(0L, cpuEndNs - cpuStartNs);       // if unsupported, cpuTimeNs==0

        // Wall-clock time in ms (important — you were missing this)
        double timeMs = wallElapsedNs / 1_000_000.0;

        // CPU time in ms (useful and never zero for short ops)
        double cpuTimeMs = cpuTimeNs / 1_000_000.0;

        double cpuPercent = 0.0;
        double cpuPercentPerCore = 0.0;
        int cores = Runtime.getRuntime().availableProcessors();

        if (cpuTimeNs > 0) {
            // CPU% across all cores (may exceed 100%)
            cpuPercent = (cpuTimeNs / (double) wallElapsedNs) * 100.0;
            // Normalized per-core percentage (approx. 0..100)
            cpuPercentPerCore = cpuPercent / (double) cores;
        }

        double memDeltaMB = memAfter - memBefore;

        // Note: QRStats constructor updated to include cpuTimeMs (see below)
        QRStats stats = new QRStats(timeMs, cpuPercent, cpuPercentPerCore, memDeltaMB, cpuTimeMs);
        return new ResultWithStats<>(result, stats);
    }


    /**
     * Convenience profiler for Runnable operations (no return value).
     * Returns QRStats only.
     */
    public static QRStats profileRunnable(Runnable runnable) {
        ResultWithStats<Void> r = profileSupplier(() -> {
            runnable.run();
            return null;
        });
        return r.stats;
    }
}
