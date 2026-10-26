package com.cityblock.sockdb;

import java.io.StringReader;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.IntPredicate;

import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import jakarta.json.JsonValue;

/**
 * In-memory document store organised as collections of JSON documents.
 *
 * <p>Every public operation returns either an {@code OK} response or one of the
 * following error codes:
 * <ul>
 *   <li>{@code ERROR NOT_FOUND} - the requested document does not exist.</li>
 *   <li>{@code ERROR COLLECTION_NOT_FOUND} - the addressed collection does not exist.</li>
 *   <li>{@code ERROR ALREADY_EXISTS} - a collection with the same name already exists.</li>
 *   <li>{@code ERROR MISSING_ARGUMENT} - a required argument is missing.</li>
 *   <li>{@code ERROR INVALID_JSON} - the supplied document is not valid JSON.</li>
 *   <li>{@code ERROR INVALID_PATH} - the supplied path is malformed or not allowed.</li>
 * </ul>
 */
public class Database {
    private static Database INSTANCE;

    private static final String NOT_FOUND = "ERROR NOT_FOUND";
    private static final String COL_NOT_FOUND = "ERROR COLLECTION_NOT_FOUND";
    private static final String EXISTS = "ERROR ALREADY_EXISTS";
    private static final String MISSING_ARG = "ERROR MISSING_ARGUMENT";
    private static final String INVALID_JSON = "ERROR INVALID_JSON";
    private static final String INVALID_PATH = "ERROR INVALID_PATH";
    private static final String INVALID_FILTER = "ERROR INVALID_FILTER";
    private static final String OK = "OK";
    private static final String FILTER_PREFIX = "filter ";
    private static final Pattern FILTER_PATTERN =
        Pattern.compile("^\\s*([^\\s<>=!]+)\\s*(>=|<=|!=|=|>|<)\\s*(.+?)\\s*$");

    /**
     * A record representing a parsed filter.
     */
    private record ParsedFilter(String attribute, Operator operator, String targetValue) {
    }

    private enum Operator {
        GTE(">=", comparison -> comparison >= 0),
        LTE("<=", comparison -> comparison <= 0),
        NEQ("!=", comparison -> comparison != 0),
        GT(">", comparison -> comparison > 0),
        LT("<", comparison -> comparison < 0),
        EQ("=", comparison -> comparison == 0);

        private final String symbol;
        private final IntPredicate condition;

        Operator(String symbol, IntPredicate condition) {
            this.symbol = symbol;
            this.condition = condition;
        }

        private boolean test(int comparison) {
            return condition.test(comparison);
        }

        private static Operator fromSymbol(String symbol) {
            for (Operator operator : values()) {
                if (operator.symbol.equals(symbol)) {
                    return operator;
                }
            }

            return null;
        }
    }

    private static final JsonReaderFactory READER_FACTORY = Json.createReaderFactory(Map.of());

    // collection name/uuid document/json
    private final ConcurrentMap<String, ConcurrentMap<String, String>> data;

    private Database() {
        data = new ConcurrentHashMap<>();
    }

    /**
     * Returns the process-wide singleton instance.
     *
     * @return the shared {@code Database} instance
     */
    public static Database getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Database();
        }
        return INSTANCE;
    }

    /**
     * Returns the raw backing store, mapping collection names to their documents.
     *
     * @return the mutable collection map
     */
    public ConcurrentMap<String, ConcurrentMap<String, String>> getData(){
        return data;
    }

    /**
     * Reads a document or lists a collection.
     *
     * <p>Expected argument format: {@code <collection>[/<documentId>] [filter <attribute> <operator> <value>]}.
     * When the filter prefix is absent no filtering is applied.
     *
     * @param args the raw request
     * @return {@code OK <json>} on success, otherwise an error code
     */
    public String readDocument(String args){
        String[] argsArr = args.split(" ", 2);
        String[] path = argsArr[0].split("/", -1);

        if (path[0].isEmpty()){
            return MISSING_ARG;
        }

        if (path.length > 2) {
            return INVALID_PATH;
        }

        var collection = data.get(path[0]);

        if (collection == null) {
            return COL_NOT_FOUND;
        }

        if (path.length == 1){
            if (argsArr.length > 1){
                String condition = argsArr[1].strip();
                if (condition.startsWith(FILTER_PREFIX)){
                    condition = condition.substring(FILTER_PREFIX.length()).strip();
                }
                if (!condition.isEmpty()){
                    ParsedFilter filter = parseFilter(condition);

                    if (filter == null) {
                        return INVALID_FILTER;
                    }
                    collection = filterCollection(collection, filter);
                }
            }

            JsonArrayBuilder array = Json.createArrayBuilder();
            for (Map.Entry<String, String> entry : collection.entrySet()) {
                try (JsonReader reader = READER_FACTORY.createReader(new StringReader(entry.getValue()))) {
                    array.add(reader.readObject());
                } catch (JsonException e) {
                    continue;
                }
            }
            return OK + " " + array.build().toString();
        }   

        if (collection.containsKey(path[1])){
            return OK + " " + collection.get(path[1]);
        } 
         
        return NOT_FOUND;
    }

    private static ParsedFilter parseFilter(String rawCondition) {
        Matcher matcher = FILTER_PATTERN.matcher(rawCondition);

        if (!matcher.matches()) {
            return null;
        }

        Operator operator = Operator.fromSymbol(matcher.group(2));

        if (operator == null) {
            return null;
        }

        return new ParsedFilter(
                matcher.group(1).strip(),
                operator,
                matcher.group(3).strip());
    }


    private static ConcurrentMap<String, String> filterCollection(ConcurrentMap<String, String> collection, ParsedFilter filter){
        String attr = filter.attribute();
        Operator operator = filter.operator();
        String targetVal = filter.targetValue();

        ConcurrentMap<String, String> filtered = new ConcurrentHashMap<>();

        for (Map.Entry<String, String> entry : collection.entrySet()) {
                try (JsonReader reader = READER_FACTORY.createReader(new StringReader(entry.getValue()))) {
                    JsonObject json = reader.readObject();
                    if (json.containsKey(attr)){
                        JsonValue value = json.get(attr);
                        String attrValue = value.getValueType() == JsonValue.ValueType.STRING
                                ? json.getString(attr)
                                : value.toString();

                        if (operator.test(compareValues(attrValue, targetVal))) {
                            filtered.put(entry.getKey(), entry.getValue());
                        }
                    }
                } catch (JsonException e) {
                    continue;
                }
            }
        return filtered;
    }

    private static int compareValues(String value, String target) {
        try {
            double left = Double.parseDouble(value);
            double right = Double.parseDouble(target);
            return Double.compare(left, right);
        } catch (NumberFormatException e) {
            return value.compareToIgnoreCase(target);
        }
    }

    /**
     * Patches an existing document, adding or replacing the given attributes.
     *
     * <p>Expected argument format: {@code <collection>/<documentId> <json>}.
     *
     * @param args the raw request
     * @return {@code OK} on success, otherwise an error code
     */
    public String writeDocument(String args){
        String[] argsArr = args.split(" ", 2);
        String[] path = argsArr[0].split("/", -1);

        if (argsArr.length == 1) {
            return MISSING_ARG;
        }

        if (path.length != 2) {
            return path.length == 1 ? MISSING_ARG : INVALID_PATH;
        }
        if (!path[1].matches("^[a-z0-9_-]{1,64}$")){
            return INVALID_PATH;
        }
        if (data.containsKey(path[0])){
            var collection = data.get(path[0]);

            if (!collection.containsKey(path[1])){
                return NOT_FOUND;
            }

            try (JsonReader docReader = READER_FACTORY.createReader(new StringReader(collection.get(path[1])))) {
                JsonObject jsonDoc = docReader.readObject();
                JsonObjectBuilder builder = Json.createObjectBuilder(jsonDoc);

                try (JsonReader inputReader = READER_FACTORY.createReader(new StringReader(argsArr[1]))) {
                    JsonObject jsonInput = inputReader.readObject();
                    for (var input : jsonInput.entrySet()) {
                        String attr = input.getKey();
                        var newVal = input.getValue();
                        builder.add(attr, newVal);
                    }
                }

                JsonObject updated = builder.build();
                collection.put(path[1], updated.toString());
                return OK;
            } catch (JsonException e) {
                return INVALID_JSON;
            }
        } else {
            return COL_NOT_FOUND;
        } 
    }

    /**
     * Inserts a new document into a collection, assigning it a generated id.
     *
     * <p>Expected argument format: {@code <collection> <json>}.
     *
     * @param args the raw request
     * @return {@code OK <id>} on success, otherwise an error code
     */
    public String insertDocument(String args){
        String[] argsArr = args.split(" ", 2);
        String[] path = argsArr[0].split("/", -1);

        if (argsArr.length == 1){
            return MISSING_ARG;
        }
        if (path.length > 1){
            return INVALID_PATH;
        }

        if (data.containsKey(path[0])){
            var collection = data.get(path[0]);

            try (JsonReader docReader = READER_FACTORY.createReader(new StringReader(argsArr[1]))) {
                JsonObject jsonDoc = docReader.readObject();

                var id = UUID.randomUUID().toString();
                collection.put(id, jsonDoc.toString());
                return OK + " " + id;
            } catch (JsonException e) {
                return INVALID_JSON;
            }
        } else {
            return COL_NOT_FOUND;
        } 
    }

    /**
     * Deletes a document.
     *
     * <p>Expected argument format: {@code <collection>/<documentId>}.
     *
     * @param args the raw request
     * @return {@code OK} on success, otherwise an error code
     */
    public String deleteDocument(String args){
        String[] argsArr = args.split(" ");
        String[] path = argsArr[0].split("/", -1);

        if (path.length != 2) {
            return path.length == 1 ? MISSING_ARG : INVALID_PATH;
        }
        if (!path[1].matches("^[a-z0-9_-]{1,64}$")){
            return INVALID_PATH;
        }

        if (data.containsKey(path[0])){
            var collection = data.get(path[0]);

            if (collection.containsKey(path[1])){
                collection.remove(path[1]);
                return OK;
            }  else {
                return NOT_FOUND;
            }
        } else {
            return COL_NOT_FOUND;
        } 
    }

    /**
     * Creates an empty collection.
     *
     * <p>Expected argument format: {@code <collection>}.
     *
     * @param args the raw request
     * @return {@code OK} on success, otherwise an error code
     */
    public String createCollection(String args){
        String[] argsArr = args.split(" ");
        String[] path = argsArr[0].split("/", -1);

        if (path.length > 1 || !path[0].matches("^[a-z0-9_-]{1,64}$")){
            return INVALID_PATH;
        }

        if (data.containsKey(path[0])){
            return EXISTS;
        } else {
            ConcurrentMap<String, String> collection = new ConcurrentHashMap<>();
            data.put(path[0], collection);
            return OK;
        }
    }

    /**
     * Deletes a collection and all its documents.
     *
     * <p>Expected argument format: {@code <collection>}.
     *
     * @param args the raw request
     * @return {@code OK} on success, otherwise an error code
     */
    public String dropCollection(String args){
        String[] argsArr = args.split(" ");
        String[] path = argsArr[0].split("/", -1);

        if (path.length > 1 || !path[0].matches("^[a-z0-9_-]{1,64}$")){
            return INVALID_PATH;
        }

        if (data.containsKey(path[0])){
            data.remove(path[0]);
            return OK;
        } else {
            return NOT_FOUND;
        }
    }
}
