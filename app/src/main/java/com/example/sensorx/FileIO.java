package com.example.sensorx;

import android.os.Environment;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class FileIO {

    public static void writeToExternalStorage(String filename, String data) {
        // Check if external storage is available
        if (isExternalStorageWritable()) {
            // Get the directory for the user's public pictures directory.
            File externalDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), filename);

            try {
                FileUtils.writeStringToFile(externalDir, data, "UTF-8",false);
                // You can use the file path as needed
                System.out.println("File written to: " + externalDir.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}