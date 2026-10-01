package com.cityblock.sockdb;

import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;


public class FileManager {

    public static void loadData() {
        ConcurrentHashMap<String, ConcurrentHashMap<String, String>> data = Database.getInstance().getData();

        try {
            URL url = FileManager.class.getClassLoader().getResource("");
            if (url == null) {
                throw new IOException("Storage not found");
            }
            Path path = Path.of(url.toURI());
            File[] collections = path.toFile().listFiles(File::isDirectory);
            if (collections == null) {
                return;
            }

            for (File collection : collections) {
                ConcurrentHashMap<String, String> collectionData = new ConcurrentHashMap<>();
                File[] documents = collection.listFiles(file ->
                        file.isFile() && file.getName().endsWith(".json"));

                if (documents != null) {
                    for (File document : documents) {
                        String documentName = document.getName().replaceFirst("[.][^.]+$", "");
                        collectionData.put(documentName, Files.readString(document.toPath()));
                    }
                }

                data.put(collection.getName(), collectionData);
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Error loading data", e);
        }
    }

    public static void updateCollection(String collection) {
        ConcurrentHashMap<String, ConcurrentHashMap<String, String>> data = Database.getInstance().getData();

        try {
            URL url = FileManager.class.getClassLoader().getResource("");
            if (url == null) {
                throw new IOException("Storage not found");
            }

            Path path = Path.of(url.toURI()).resolve(collection);

            if (data.containsKey(collection)) {
                Files.createDirectories(path);
            } else {
                if (Files.exists(path)) {
                    try (var files = Files.walk(path)) {
                        files.sorted(Comparator.reverseOrder()).forEach(file -> {
                            try {
                                Files.delete(file);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Error updating collection " + collection, e);
        }
    }

    public static void updateDocument(String collection, String document) {
        ConcurrentHashMap<String, ConcurrentHashMap<String, String>> data = Database.getInstance().getData();
        String filename = collection + "/" + document + ".json";

        try {
            URL url = FileManager.class.getClassLoader().getResource("");
            if (url == null) {
                throw new IOException("Storage not found");
            }

            Path path = Path.of(url.toURI()).resolve(filename);

            if (data.containsKey(collection) && data.get(collection).containsKey(document)) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, data.get(collection).get(document));
            } else {
                Files.deleteIfExists(path);
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Error updating " + filename, e);
        }
    }
}
