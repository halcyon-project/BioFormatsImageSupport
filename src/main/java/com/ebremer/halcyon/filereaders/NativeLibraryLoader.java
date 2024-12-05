package com.ebremer.halcyon.filereaders;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class NativeLibraryLoader {

    public static void loadLibraryFromJar(String resourcePath) {
        try {
            // Get the DLL as a resource stream
            InputStream inputStream = NativeLibraryLoader.class.getResourceAsStream(resourcePath);
            if (inputStream == null) {
                throw new IllegalArgumentException("Library " + resourcePath + " not found in JAR.");
            }

            // Create a temporary file to store the DLL
            File tempFile = File.createTempFile("nativeLib", ".dll");
            tempFile.deleteOnExit();

            // Write the DLL to the temporary file
            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            // Load the library
            System.load(tempFile.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load library", e);
        }
    }
}
