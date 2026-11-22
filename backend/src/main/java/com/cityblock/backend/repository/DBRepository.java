package com.cityblock.backend.repository;

import org.springframework.stereotype.Repository;
import com.cityblock.backend.db.DBClient;

@Repository
public class DBRepository {
    private final DBClient dbClient;

    private static final String READ_COMMAND = "read";
    private static final String WRITE_COMMAND = "write";
    private static final String DELETE_COMMAND = "delete";
    private static final String INSERT_COMMAND = "insert";
    private static final String CREATE_COMMAND = "create";

    public DBRepository(DBClient dbClient) {
        this.dbClient = dbClient;
    }

    public String createCollection(String collection) {
        return dbClient.send(CREATE_COMMAND + " " + collection);
    }

    public String insert(String collection, String json) {
        String command = INSERT_COMMAND + " " + collection + " " + json;
        return dbClient.send(command);
    }

    public void update(String collection, String id, String patch) {
        String command = WRITE_COMMAND + " " + collection + "/" + id + " " + patch;
        dbClient.send(command);
    }

    public void delete(String collection, String id) {
        String command = DELETE_COMMAND + " " + collection + "/" + id;
        dbClient.send(command);
    }

    public String findAll(String collection) {
        return dbClient.send(READ_COMMAND + " " + collection);
    }

    public String findAll(String collection, String filter) {
        return dbClient.send(READ_COMMAND + " " + collection + " filter " + filter);
    }

    public String findByField(String collection, String field, String value) {
        return findAll(collection, field + " = " + value);
    }

    public String findById(String collection, String id) {
        return dbClient.send(READ_COMMAND + " " + collection + "/" + id);
    }
}
