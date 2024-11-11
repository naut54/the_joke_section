package edumdev;

import java.io.IOException;
import java.nio.file.*;

public class Main {
    public static void main(String[] args) throws IOException {
        fileWriter();
    }

    public static void fileWriter() throws IOException {
        int roof = 2;
        String stmt = "@@@@@@@@@@";
        Path path = Paths.get("joke.txt");
        for (int i = 0; i < roof; i++) {
            Files.write(path, stmt.getBytes());
        }
    }
}