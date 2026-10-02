package com.cityblock.sockdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.json.Json;
import jakarta.json.JsonReader;

class DatabaseTest {

    private Database db;

    @BeforeEach
    void setUp() {
        db = Database.getInstance();
        db.getData().clear();
    }

    private String id(String response) {
        assertTrue(response.startsWith("OK "), response);
        return response.substring(3);
    }

    private List<String> documents(String response) {
        assertTrue(response.startsWith("OK ["), response);
        try (JsonReader reader = Json.createReader(new StringReader(response.substring(3)))) {
            List<String> entries = new ArrayList<>();
            reader.readArray().forEach(document -> entries.add(document.toString()));
            entries.sort(null);
            return entries;
        }
    }

    @Test
    void createAndDropCollection() {
        assertEquals("OK", db.createCollection("players"));
        assertEquals("ERROR ALREADY_EXISTS", db.createCollection("players"));
        assertEquals("ERROR NOT_FOUND", db.dropCollection("games"));
        assertEquals("OK", db.dropCollection("players"));
        assertEquals("ERROR NOT_FOUND", db.dropCollection("players"));
    }

    @Test
    void rejectsInvalidNames() {
        assertEquals("ERROR INVALID_PATH", db.createCollection("Players"));
        assertEquals("ERROR INVALID_PATH", db.createCollection("../evil"));
        assertEquals("ERROR INVALID_PATH", db.createCollection("players/elenina"));
        assertEquals("ERROR INVALID_PATH", db.dropCollection("../evil"));
    }

    @Test
    void insertKeepsJsonWithSpaces() {
        db.createCollection("players");
        String response = db.insertDocument("players {\"username\": \"elenina rossi\", \"balance\": 100}");

        assertEquals("OK {\"username\":\"elenina rossi\",\"balance\":100}",
                db.readDocument("players/" + id(response)));
    }

    @Test
    void insertRejectsBadRequests() {
        db.createCollection("players");

        assertEquals("ERROR MISSING_ARGUMENT", db.insertDocument("players"));
        assertEquals("ERROR INVALID_PATH", db.insertDocument("players/elenina {\"a\": 1}"));
        assertEquals("ERROR INVALID_JSON", db.insertDocument("players {\"a\":"));
        assertEquals("ERROR INVALID_JSON", db.insertDocument("players [1, 2]"));
        assertEquals("ERROR INVALID_JSON", db.insertDocument("players \"ciao\""));
        assertEquals("ERROR COLLECTION_NOT_FOUND", db.insertDocument("games {\"a\": 1}"));
        assertEquals("OK []", db.readDocument("players"));
    }

    @Test
    void readReturnsDocumentsInAnyOrder() {
        db.createCollection("players");
        db.insertDocument("players {\"username\": \"elenina\"}");
        db.insertDocument("players {\"username\": \"bob\"}");

        assertEquals(List.of("{\"username\":\"bob\"}", "{\"username\":\"elenina\"}"),
                documents(db.readDocument("players")));
    }

    @Test
    void readReportsMissingTargets() {
        db.createCollection("players");

        assertEquals("ERROR MISSING_ARGUMENT", db.readDocument(""));
        assertEquals("ERROR COLLECTION_NOT_FOUND", db.readDocument("games"));
        assertEquals("ERROR NOT_FOUND", db.readDocument("players/nope"));
    }

    @Test
    void readFiltersWithKeywordAndSpaces() {
        db.createCollection("players");
        db.insertDocument("players {\"username\": \"elenina\", \"balance\": 100}");
        db.insertDocument("players {\"username\": \"bob\", \"balance\": 0}");

        assertEquals(List.of("{\"username\":\"elenina\",\"balance\":100}"),
                documents(db.readDocument("players filter balance > 50")));
        assertEquals(List.of("{\"username\":\"elenina\",\"balance\":100}"),
                documents(db.readDocument("players balance>50")));
        assertEquals(List.of("{\"username\":\"bob\",\"balance\":0}"),
                documents(db.readDocument("players filter username = bob")));
        assertEquals(List.of("{\"username\":\"elenina\",\"balance\":100}"),
                documents(db.readDocument("players filter balance >= 100")));
        assertEquals(2, documents(db.readDocument("players filter")).size());
        assertEquals(2, documents(db.readDocument("players")).size());
    }

    @Test
    void readFiltersWithoutMatchingNothing() {
        db.createCollection("players");
        db.insertDocument("players {\"username\": \"elenina\", \"balance\": 100}");

        assertEquals(List.of(), documents(db.readDocument("players filter balance < 50")));
        assertEquals(List.of(), documents(db.readDocument("players filter username = carol")));
        assertEquals(List.of(), documents(db.readDocument("players filter missing = 1")));
    }

    @Test
    void filterSkipsNonStringAttributes() {
        db.createCollection("games");
        db.insertDocument("games {\"playerIds\": [\"elenina\", \"bob\"], \"ended\": true}");
        db.insertDocument("games {\"playerIds\": [\"carol\"], \"ended\": false}");

        assertEquals(List.of(), documents(db.readDocument("games playerIds=elenina")));
        assertEquals(1, documents(db.readDocument("games ended=true")).size());
    }

    @Test
    void filterSkipsCorruptedDocuments() {
        db.createCollection("players");
        db.getData().get("players").put("broken", "{\"balance\":");
        db.insertDocument("players {\"balance\": 100}");

        assertEquals(List.of("{\"balance\":100}"), documents(db.readDocument("players balance>50")));
    }

    @Test
    void writePatchesOnlyGivenAttributes() {
        db.createCollection("players");
        String documentId = id(db.insertDocument("players {\"username\": \"elenina\", \"balance\": 100}"));

        assertEquals("OK", db.writeDocument("players/" + documentId + " {\"balance\": 50}"));
        assertEquals("OK {\"username\":\"elenina\",\"balance\":50}", db.readDocument("players/" + documentId));
    }

    @Test
    void writeAcceptsNestedValues() {
        db.createCollection("players");
        String documentId = id(db.insertDocument("players {\"username\": \"elenina\"}"));

        assertEquals("OK", db.writeDocument("players/" + documentId + " {\"tags\": [\"a\", \"b\"], \"active\": true}"));
        assertEquals("OK {\"username\":\"elenina\",\"tags\":[\"a\",\"b\"],\"active\":true}",
                db.readDocument("players/" + documentId));
    }

    @Test
    void writeRejectsBadRequests() {
        db.createCollection("players");
        String documentId = id(db.insertDocument("players {\"balance\": 100}"));

        assertEquals("ERROR MISSING_ARGUMENT", db.writeDocument("players"));
        assertEquals("ERROR MISSING_ARGUMENT", db.writeDocument("players/" + documentId));
        assertEquals("ERROR INVALID_PATH", db.writeDocument("players/../evil {\"a\": 1}"));
        assertEquals("ERROR INVALID_JSON", db.writeDocument("players/" + documentId + " {\"a\":"));
        assertEquals("ERROR NOT_FOUND", db.writeDocument("players/nope {\"a\": 1}"));
        assertEquals("ERROR COLLECTION_NOT_FOUND", db.writeDocument("games/" + documentId + " {\"a\": 1}"));
        assertEquals("OK {\"balance\":100}", db.readDocument("players/" + documentId));
    }

    @Test
    void deleteRemovesDocument() {
        db.createCollection("players");
        String documentId = id(db.insertDocument("players {\"username\": \"elenina\"}"));

        assertEquals("OK", db.deleteDocument("players/" + documentId));
        assertEquals("ERROR NOT_FOUND", db.deleteDocument("players/" + documentId));
        assertEquals("OK []", db.readDocument("players"));
    }

    @Test
    void deleteRejectsBadRequests() {
        assertEquals("ERROR MISSING_ARGUMENT", db.deleteDocument("players"));
        assertEquals("ERROR INVALID_PATH", db.deleteDocument("players/../evil"));
        assertEquals("ERROR COLLECTION_NOT_FOUND", db.deleteDocument("games/nope"));
    }
}