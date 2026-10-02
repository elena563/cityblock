package com.cityblock.sockdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileManagerTest {

    private Database db;
    private Path dataDir;
    private String previousDir;

    @BeforeEach
    void setUp(@TempDir Path temp) {
        db = Database.getInstance();
        db.getData().clear();

        previousDir = System.getProperty(FileManager.DATA_DIR_PROPERTY);
        dataDir = temp.resolve("data");
        System.setProperty(FileManager.DATA_DIR_PROPERTY, dataDir.toString());
    }

    @AfterEach
    void tearDown() {
        if (previousDir == null) {
            System.clearProperty(FileManager.DATA_DIR_PROPERTY);
        } else {
            System.setProperty(FileManager.DATA_DIR_PROPERTY, previousDir);
        }
        db.getData().clear();
    }

    private String insert(String collection, String json) {
        db.createCollection(collection);
        String response = db.insertDocument(collection + " " + json);
        assertTrue(response.startsWith("OK "), response);

        String document = response.substring(3);
        FileManager.updateDocument(collection, document);
        return document;
    }

    private List<String> documents(String collection) {
        return new ArrayList<>(db.getData().get(collection).keySet()).stream().sorted().toList();
    }

    @Test
    void writesOneFilePerDocument() throws Exception {
        String document = insert("matches", "{\"winner\": \"elenina\"}");

        assertTrue(Files.exists(dataDir.resolve("matches").resolve(document + ".json")));
        assertEquals("{\"winner\":\"elenina\"}",
                Files.readString(dataDir.resolve("matches").resolve(document + ".json")));
    }

    @Test
    void createAndDropKeepTheFolderInSync() {
        db.createCollection("matches");
        FileManager.updateCollection("matches");
        assertTrue(Files.isDirectory(dataDir.resolve("matches")));

        db.dropCollection("matches");
        FileManager.updateCollection("matches");
        assertFalse(Files.exists(dataDir.resolve("matches")));
    }

    @Test
    void deleteRemovesTheFile() {
        String document = insert("matches", "{\"winner\": \"elenina\"}");

        db.deleteDocument("matches/" + document);
        FileManager.updateDocument("matches", document);

        assertFalse(Files.exists(dataDir.resolve("matches").resolve(document + ".json")));
    }

    @Test
    void loadDataRestoresEverythingFromDisk() {
        insert("matches", "{\"winner\": \"elenina\"}");
        insert("matches", "{\"winner\": \"bob\"}");

        db.getData().clear();
        FileManager.loadData();

        assertEquals(2, db.getData().get("matches").size());
    }

    @Test
    void firstBootCopiesTheResourcesOnlyOnce() {
        FileManager.loadData();

        assertEquals(List.of("1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
                "2b3c4d5e-6f7a-4b8c-9d0e-1f2a3b4c5d6e",
                "3c4d5e6f-7a8b-4c9d-8e0f-2a3b4c5d6e7f"), documents("players"));
        assertEquals(List.of("9e8d7c6b-5a4f-4392-8180-7f6e5d4c3b2a"), documents("games"));
        assertEquals(List.of("7f3c2d1a-84b5-4e29-a3f1-0c9d2e5b6f78"), documents("rooms"));
        assertEquals("OK {\"username\":\"elenina\",\"balance\":100,\"playerStatus\":\"winner\"}",
                db.readDocument("players/1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d"));

        db.dropCollection("games");
        FileManager.updateCollection("games");
        db.getData().clear();
        FileManager.loadData();

        assertFalse(db.getData().containsKey("games"));
    }
}