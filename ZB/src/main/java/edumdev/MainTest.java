package edumdev;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;

public class MainTest {

    @TempDir
    Path tempDir;
    private String outputDir;
    private String[] testFiles;

    @BeforeEach
    void setUp() {
        outputDir = "TestOutput";
        testFiles = new String[]{"test1", "test2", "test3"};
        // Configurar el directorio de trabajo
        System.setProperty("user.dir", tempDir.toString());
    }

    @Test
    void testFileWriterCreatesCorrectNumberOfFiles() throws IOException {
        // Ejecutar el método a probar
        Main.fileWriter(outputDir, testFiles);

        // Verificar que se creó el directorio de salida
        Path outputPath = tempDir.resolve(outputDir);
        assertTrue(Files.exists(outputPath), "El directorio de salida debería existir");

        // Verificar el número correcto de archivos
        long fileCount = Files.list(outputPath).count();
        assertEquals(Main.N_THREADS, fileCount,
                "El número de archivos creados debería ser igual a N_THREADS");
    }

    @Test
    void testFileWriterWithEmptyFileList() throws IOException {
        String[] emptyFiles = {};

        // Verificar que se lanza una excepción apropiada
        assertThrows(IllegalArgumentException.class, () -> {
            Main.fileWriter(outputDir, emptyFiles);
        }, "Debería lanzar una excepción cuando la lista de archivos está vacía");
    }

    @Test
    void testFileWriterWithNullParameters() {
        // Verificar null en el nombre del directorio
        assertThrows(NullPointerException.class, () -> {
            Main.fileWriter(null, testFiles);
        }, "Debería lanzar una excepción cuando el nombre del directorio es null");

        // Verificar null en la lista de archivos
        assertThrows(NullPointerException.class, () -> {
            Main.fileWriter(outputDir, null);
        }, "Debería lanzar una excepción cuando la lista de archivos es null");
    }

    @AfterEach
    void tearDown() throws IOException {
        // Restaurar la propiedad del sistema
        System.clearProperty("user.dir");
    }
}