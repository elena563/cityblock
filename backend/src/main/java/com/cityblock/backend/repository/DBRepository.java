package com.cityblock.backend.repository;

import org.springframework.stereotype.Repository;
import com.cityblock.backend.db.DBClient;

@Repository
public class DBRepository {
    private DBClient dbClient;

    public DBRepository(DBClient dbClient) {
        this.dbClient = dbClient;
    }

    public String insert(String collection, String json) {
        String command = "insert " + collection + " " + json;
        return dbClient.send(command);
    }

    public void update(String collection, String id, String patch) {
        String command = "write " + collection + "/" + id + " " + patch;
        dbClient.send(command);
    }

    public void delete(String collection, String id) {
        String command = "delete " + collection + "/" + id;
        dbClient.send(command);
    }

    public String findAll(String collection) {
        return dbClient.send("read " + collection);
    }

    public String findAll(String collection, String filter) {
        return dbClient.send("read " + collection + " filter " + filter);
    }

    public String findById(String collection, String id) {
        return dbClient.send("read " + collection + "/" + id);
    }
}
