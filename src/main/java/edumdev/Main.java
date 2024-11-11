package edumdev;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        fileWriter();
    }

    public static void fileWriter() throws IOException {
        int roof = 1;
        try (FileWriter fileWriter = new FileWriter("C:\\output.txt")) {
            for (int i = 0; i < roof; i++) {
                fileWriter.write(i + "\n");
            }
        }
    }
}