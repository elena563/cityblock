package com.cityblock.sockdb;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class FileManager {

    static final String DATA_DIR_PROPERTY = "sockdb.data.dir";

    private static final String INITIALIZED_MARKER = ".initialized";

    private static Path dataDir(){
        String dir = System.getProperty(DATA_DIR_PROPERTY);
        if (dir == null){
            dir = System.getenv().getOrDefault("SOCKDB_DATA_DIR", System.getProperty("user.home") + "/.sockdb/data");
        }
        return Path.of(dir);
    }

    private static List<String> seedFiles(){
        try {
            URL url = FileManager.class.getClassLoader().getResource("");
            if (url == null || !url.getProtocol().equals("file")) {
                return List.of();
            }

            List<String> files = new ArrayList<>();
            File[] collections = Path.of(url.toURI()).toFile().listFiles(File::isDirectory);
            if (collections == null) {
                return files;
            }

            for (File collection : collections) {
                File[] documents = collection.listFiles(file ->
                        file.isFile() && file.getName().endsWith(".json"));

                if (documents != null) {
                    for (File document : documents) {
                        files.add(collection.getName() + "/" + document.getName());
                    }
                }
            }
            return files;
        } catch (URISyntaxException | IllegalArgumentException e) {
            return List.of();
        }
    }

    private static void loadSeed(Path dir) throws IOException {
        if (Files.exists(dir.resolve(INITIALIZED_MARKER))) {
            return;
        }

        List<String> files = seedFiles();
        if (files.isEmpty()) {
            return;
        }

        for (String file : files){
            try (InputStream seed = FileManager.class.getResourceAsStream("/" + file)) {
                if (seed == null) {
                    continue;
                }
                Path target = dir.resolve(file);
                Files.createDirectories(target.getParent());
                Files.writeString(target, new String(seed.readAllBytes(), StandardCharsets.UTF_8));
            }
        }

        Files.writeString(dir.resolve(INITIALIZED_MARKER), "");
    }

    public static void loadData() {
        ConcurrentHashMap<String, ConcurrentHashMap<String, String>> data = Database.getInstance().getData();

        try {
            Path path = dataDir();
            Files.createDirectories(path);
            loadSeed(path);

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
        } catch (IOException e) {
            throw new RuntimeException("Error loading data", e);
        }
    }

    public static void updateCollection(String collection) {
        ConcurrentHashMap<String, ConcurrentHashMap<String, String>> data = Database.getInstance().getData();

        try {
            Path path = dataDir().resolve(collection);

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
        } catch (IOException e) {
            throw new RuntimeException("Error updating collection " + collection, e);
        }
    }

    public static void updateDocument(String collection, String document) {
        ConcurrentHashMap<String, ConcurrentHashMap<String, String>> data = Database.getInstance().getData();
        String filename = collection + "/" + document + ".json";

        try {
            Path path = dataDir().resolve(filename);

            if (data.containsKey(collection) && data.get(collection).containsKey(document)) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, data.get(collection).get(document));
            } else {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error updating " + filename, e);
        }
    }
}
