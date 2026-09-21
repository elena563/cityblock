package com.cityblock.sockdb;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.json.JsonObject;
import jakarta.json.Json;

public class Database {
    private static Database INSTANCE;

    private String NOT_FOUND = "ERROR NOT_FOUND";
    private String COL_NOT_FOUND = "ERROR COLLECTION_NOT_FOUND";

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

    // path potrbbe essere players/uuid-354
    public String readDocument(String args){
        String[] argsList = args.split(" ");
        String[] path;
        path = argsList[0].split("/");

        boolean filter;
        String attr = null;
        String targetVal = null;

        if (argsList.length == 1){
            filter = false;
        } else{
            filter = true;

            String[] condition = argsList[2].split("=");
            attr = condition[0];
            targetVal = condition[1];
        }

        if (data.containsKey(path[0])){
            var collection = data.get(path[0]);
            String result = "";

            if (path.length == 1){
                
                ArrayList<String> entries = new ArrayList<>();
                for (Map.Entry<String, String> entry : collection.entrySet()) {
                    if (filter == true){
                        JsonObject json = Json.createReader(new StringReader(entry.getValue())).readObject();
                        if (json.containsKey(attr)){
                            if (json.getString(attr).equalsIgnoreCase(targetVal)){
                                entries.add(entry.getValue());
                            }
                        } 
                    } else {
                        entries.add(entry.getValue());
                    }
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
    // TODO: tidy up this mess and add helper method

    public String writeDocument(){return "OK";}

    public String insertDocument(){return "OK";}

    public String deleteDocument(){return "OK";}

    public String createCollection(){return "OK";}

    public String dropCollection(){return "OK";}
}
