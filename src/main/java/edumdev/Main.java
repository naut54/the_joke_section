package edumdev;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final int BUFFER_SIZE = 8192 * 1024;
    private static final int BATCH_SIZE = 100000;
    private static final String STMT = "@@@@@@@@@";

    // Valores originales
    private static final int ROOF = 1_000;
    private static final int N_THREADS = 10;

    public static void main(String[] args) {
        long startTime = System.nanoTime();
        for (int i = 0; i <= 5; i++) {
            optimizedFileWriter();
        }

        long endTime = System.nanoTime() - startTime;
        System.out.printf("Tiempo de ejecución: %.2f segundos%n", endTime / 1_000_000_000.0);
    }

    public static void optimizedFileWriter() {
        ExecutorService executor = Executors.newFixedThreadPool(N_THREADS);

        Path outputDir = Paths.get("C:\\output");
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            System.err.println("Error creando directorio: " + e.getMessage());
            return;
        }

        StringBuilder batchContent = new StringBuilder(STMT.length() * BATCH_SIZE);
        for (int i = 0; i < BATCH_SIZE; i++) {
            batchContent.append(STMT);
        }
        String preComputedBatch = batchContent.toString();

        for (int i = 0; i < N_THREADS; i++) {
            int n = i;
            while (Files.exists(outputDir.resolve("file_" + n + ".txt"))) {
                n++;
            }
            final int threadId = n;
            executor.submit(() -> {
                try (BufferedWriter writer = new BufferedWriter(
                        new FileWriter(outputDir.resolve("file_" + threadId + ".txt").toString()),
                        BUFFER_SIZE)) {

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
                } catch (IOException e) {
                    System.err.printf("Error en thread %d: %s%n", threadId, e.getMessage());
                }
            });
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                System.err.println("Timeout esperando la finalización de las tareas");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupción durante la espera de finalización");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}