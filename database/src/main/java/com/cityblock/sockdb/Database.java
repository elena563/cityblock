package com.cityblock.sockdb;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.json.JsonObject;
import jakarta.json.Json;
import jakarta.json.JsonReader;

public class Database {
    private static Database INSTANCE;

    private String NOT_FOUND = "ERROR NOT_FOUND";
    private String COL_NOT_FOUND = "ERROR COLLECTION_NOT_FOUND";
    private String EXISTS = "ERROR ALREADY_EXISTS";
    private String MISSING_ARG = "ERROR MISSING_ARGUMENT";

    // collection name/uuid document/json
    private ConcurrentHashMap<String, ConcurrentHashMap<String, String>> data;

    private Database() {
        data = new ConcurrentHashMap<>();
    }

    public static Database getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Database();
        }
        return INSTANCE;
    }

    public String readDocument(String args){
        String[] argsList = args.split(" ");
        String[] path = argsList[0].split("/");

        if (data.containsKey(path[0])){
            var collection = data.get(path[0]);

            if (argsList.length > 1){
                String condition = argsList[1];
                collection = filterCollection(collection, condition);
            }

            String result = "";

            if (path.length == 1){
                ArrayList<String> entries = new ArrayList<>();
                for (Map.Entry<String, String> entry : collection.entrySet()) {
                        entries.add(entry.getValue());
                }
                result = "[" + String.join(",", entries) + "]";
                return "OK {" + result + "}";
            } else{
                if (collection.containsKey(path[1])){
                    result = collection.get(path[1]);
                    return "OK {" + result + "}";
                }  else {
                return NOT_FOUND;
                }
            }
        } else {
            return COL_NOT_FOUND;
        }
    }

    public ConcurrentHashMap<String, String> filterCollection(ConcurrentHashMap<String, String> collection, String rawCondition){
        String[] operators = {">=", "<=", "!=", ">", "<", "="};
        String operator = "";
        String[] condition = null;
        
        for (String op : operators){
            if (rawCondition.contains(op)){
                condition = rawCondition.split(op);
                operator = op;
                break;
            }
        }

        if (condition == null){
            return collection;
        }

        String attr = condition[0];
        String targetVal = condition[1];

        ConcurrentHashMap<String, String> filtered = new ConcurrentHashMap<>();

        for (Map.Entry<String, String> entry : collection.entrySet()) {
                try (JsonReader reader = Json.createReader(new StringReader(entry.getValue()))) {
                    JsonObject json = reader.readObject();
                    if (json.containsKey(attr)){
                        switch (operator) {
                            case "<":
                                if (Double.parseDouble(json.getString(attr)) < Double.parseDouble(targetVal)){
                                    filtered.put(entry.getKey(), entry.getValue());
                                }
                                break;
                            case ">":
                                if (Double.parseDouble(json.getString(attr)) > Double.parseDouble(targetVal)){
                                    filtered.put(entry.getKey(), entry.getValue());
                                }
                                break;
                            case "<=":
                                if (Double.parseDouble(json.getString(attr)) <= Double.parseDouble(targetVal)){
                                    filtered.put(entry.getKey(), entry.getValue());
                                }
                                break;
                            case ">=":
                                if (Double.parseDouble(json.getString(attr)) >= Double.parseDouble(targetVal)){
                                    filtered.put(entry.getKey(), entry.getValue());
                                }
                                break;
                            case "!=":
                                if (!json.getString(attr).equalsIgnoreCase(targetVal)){
                                    filtered.put(entry.getKey(), entry.getValue());
                                }
                                break;
                            case "=":
                                if (json.getString(attr).equalsIgnoreCase(targetVal)){
                                    filtered.put(entry.getKey(), entry.getValue());
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        return filtered;
    }

    // path potrbbe essere players/uuid-354
    public String writeDocument(){return "OK";}

    public String insertDocument(String args){
        String[] argsList = args.split(" ");
        String[] path = argsList[0].split("/");

        if (path.length == 1){
            return MISSING_ARG;
        }

        if (data.containsKey(path[0])){
            var collection = data.get(path[0]);
            
            var id = UUID.randomUUID().toString();
            collection.put(id, path[1]);
            return "OK " + id;
            
        } else {
            return COL_NOT_FOUND;
        } 
    }

    public String deleteDocument(String args){
        String[] argsList = args.split(" ");
        String[] path = argsList[0].split("/");

        if (path.length == 1){
            return MISSING_ARG;
        }

        if (data.containsKey(path[0])){
            var collection = data.get(path[0]);

            if (collection.containsKey(path[1])){
                collection.remove(path[1]);
                return "OK";
            }  else {
                return NOT_FOUND;
            }
        } else {
            return COL_NOT_FOUND;
        } 
    }

    public String createCollection(String args){
        String[] argsList = args.split(" ");
        String[] path = argsList[0].split("/");

        if (data.containsKey(path[0])){
            return EXISTS;
        } else {
            ConcurrentHashMap<String, String> collection = new ConcurrentHashMap<>();
            data.put(path[0], collection);
            return "OK";
        }
    }

    public String dropCollection(String args){
        String[] argsList = args.split(" ");
        String[] path = argsList[0].split("/");

        if (data.containsKey(path[0])){
            data.remove(path[0]);
            return "OK";
        } else {
            return NOT_FOUND;
        }
    }
}
