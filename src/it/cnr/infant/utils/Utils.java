package it.cnr.infant.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class Utils {

	public static void deleteDirectory(Path directory) throws IOException {

        if (!Files.exists(directory)) {
            return;
        }

        Files.walk(directory)
             .sorted(Comparator.reverseOrder()) // files before directories
             .forEach(path -> {
                 try {
                     Files.delete(path);
                 } catch (IOException e) {
                     throw new RuntimeException(
                             "Failed to delete: " + path, e);
                 }
             });
    }
	
}
