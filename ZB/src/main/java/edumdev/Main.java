package edumdev;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    private static final int BUFFER_SIZE = 8192 * 1024;
    private static final int BATCH_SIZE = 100000;
    private static final String STMT = "@@@@@@@@@";
    private static final int ROOF = 40_000_000;
    private static final int FILES_TO_CREATE = 150000;
    private static final int QUEUE_SIZE = 100;
    private static final int BATCH_REPORT_SIZE = 100;

    private static final int THREADS = Math.max(2, Runtime.getRuntime().availableProcessors());
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    public static void main(String[] args) {
        System.out.printf("Iniciando creación de %d archivos usando %d hilos...%n", FILES_TO_CREATE, THREADS);
        long startTime = System.nanoTime();

        fileWriter();

        long endTime = System.nanoTime() - startTime;
        System.out.printf("%nTiempo total de ejecución: %.2f segundos%n", endTime / 1_000_000_000.0);
    }

    public static void fileWriter() {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(QUEUE_SIZE);
        AtomicInteger completedFiles = new AtomicInteger(0);
        AtomicInteger lastReported = new AtomicInteger(0);
        Path tempDir = Paths.get(TEMP_DIR);

        String preComputedBatch = preComputeBatchContent();

        CompletableFuture[] futures = new CompletableFuture[THREADS];
        for (int i = 0; i < THREADS; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Runnable task = workQueue.poll(1, TimeUnit.SECONDS);
                        if (task == null) {
                            if (completedFiles.get() >= FILES_TO_CREATE) {
                                break;
                            }
                            continue;
                        }
                        task.run();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, executor);
        }

        for (int i = 0; i < FILES_TO_CREATE; i++) {
            final int fileNumber = i + 1;
            try {
                workQueue.put(() -> {
                    String fileName = UUID.randomUUID().toString() + ".txt";
                    try (BufferedWriter writer = new BufferedWriter(
                            new FileWriter(tempDir.resolve(fileName).toString()),
                            BUFFER_SIZE)) {
                        writeContentToFile(writer, preComputedBatch);

                        int completed = completedFiles.incrementAndGet();
                        reportProgress(completed, lastReported);

                    } catch (IOException e) {
                        System.err.printf("Error creando archivo %d: %s%n", fileNumber, e.getMessage());
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        CompletableFuture.allOf(futures).join();

        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    private static String preComputeBatchContent() {
        StringBuilder batchContent = new StringBuilder(STMT.length() * BATCH_SIZE);
        for (int i = 0; i < BATCH_SIZE; i++) {
            batchContent.append(STMT);
        }
        return batchContent.toString();
    }

    private static void writeContentToFile(BufferedWriter writer, String preComputedBatch) throws IOException {
        int remainingItems = ROOF;
        while (remainingItems > 0) {
            int batchesToWrite = Math.min(remainingItems / STMT.length(), BATCH_SIZE);
            if (batchesToWrite > 0) {
                writer.write(preComputedBatch, 0, batchesToWrite * STMT.length());
                remainingItems -= batchesToWrite * STMT.length();
            } else {
                writer.write(STMT);
                remainingItems -= STMT.length();
            }
        }
    }

    private static void reportProgress(int completed, AtomicInteger lastReported) {
        int lastReportedValue = lastReported.get();
        if (completed % BATCH_REPORT_SIZE == 0 &&
                completed > lastReportedValue &&
                lastReported.compareAndSet(lastReportedValue, completed)) {
            System.out.printf("Progreso: %d/%d archivos creados (%.1f%%)%n",
                    completed, FILES_TO_CREATE, (completed * 100.0) / FILES_TO_CREATE);
        }
    }
}