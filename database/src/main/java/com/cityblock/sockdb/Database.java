package com.cityblock.sockdb;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonReader;

public class Database {
    private static Database INSTANCE;

    private String NOT_FOUND = "ERROR NOT_FOUND";
    private String COL_NOT_FOUND = "ERROR COLLECTION_NOT_FOUND";
    private String EXISTS = "ERROR ALREADY_EXISTS";
    private String MISSING_ARG = "ERROR MISSING_ARGUMENT";
    private String INVALID_JSON = "ERROR INVALID_JSON";

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

        if (condition == null || condition.length == 1){
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
                            case ">":
                            case "<=":
                            case ">=":
                                if (satisfies(json.getString(attr), targetVal, operator)){
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

    private boolean satisfies(String value, String target, String op){
        try {
            double a = Double.parseDouble(value);
            double b = Double.parseDouble(target);
            switch (op) {
                case "<": return a < b;
                case ">": return a > b;
                case "<=": return a <= b;
                case ">=": return a >= b;
                default: return false;
            }
        } catch (NumberFormatException e) {
            int cmp = value.compareTo(target);
            switch (op) {
                case "<": return cmp < 0;
                case ">": return cmp > 0;
                case "<=": return cmp <= 0;
                case ">=": return cmp >= 0;
                default: return false;
            }
        }
    }

    public String writeDocument(String args){
        String[] argsList = args.split(" ", 2);
        String[] path = argsList[0].split("/");

        if (argsList.length == 1 || path.length == 1){
            return MISSING_ARG;
        }
        if (data.containsKey(path[0])){
            var collection = data.get(path[0]);

            if (!collection.containsKey(path[1])){
                return NOT_FOUND;
            }

            try (JsonReader docReader = Json.createReader(new StringReader(collection.get(path[1])))) {
                JsonObject jsonDoc = docReader.readObject();
                JsonObjectBuilder builder = Json.createObjectBuilder(jsonDoc);

                try (JsonReader inputReader = Json.createReader(new StringReader(argsList[1]))) {
                    JsonObject jsonInput = inputReader.readObject();
                    for (var input : jsonInput.entrySet()) {
                        String attr = input.getKey();
                        var newVal = input.getValue();
                        builder.add(attr, newVal);
                    }
                }

                JsonObject updated = builder.build();
                collection.put(path[1], updated.toString());
                return "OK";
            } catch (JsonException e) {
                return INVALID_JSON;
            }
        } else {
            return COL_NOT_FOUND;
        } 
    }

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
