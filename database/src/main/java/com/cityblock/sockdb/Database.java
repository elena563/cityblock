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
    private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9_-]{1,64}");

    /**
     * A record representing a parsed filter.
     */
    private record ParsedFilter(String attribute, Operator operator, String targetValue) {}

    private record Target(String collection, String docId, String rest, int pathLength) {}

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
        Target target = parse(args);

        String pathError = validatePath(target, 2);
        if (pathError != null) {
            return pathError;
        }

        var collection = data.get(target.collection());

        if (collection == null) {
            return COL_NOT_FOUND;
        }

        if (target.docId().isEmpty()){
            String condition = target.rest();
            if (condition.startsWith(FILTER_PREFIX)){
                condition = condition.substring(FILTER_PREFIX.length()).strip();
            }

            if (!condition.isEmpty()) {
                ParsedFilter filter = parseFilter(condition);
                if (filter == null) {
                    return INVALID_FILTER;
                }

                collection = filterCollection(collection, filter);
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

        if (collection.containsKey(target.docId())){
            return OK + " " + collection.get(target.docId());
        } 
         
        return NOT_FOUND;
    }

    private static boolean isValidName(String value) {
        return value != null && VALID_NAME.matcher(value).matches();
    }

    private static String validatePath(Target target, int maxSegments) {
        if (target.collection().isEmpty()) {
            return MISSING_ARG;
        }

        if (target.pathLength() > maxSegments
                || !isValidName(target.collection())
                || (target.pathLength() == 2 && !isValidName(target.docId()))) {
            return INVALID_PATH;
        }

        return null;
    }

    private static Target parse(String args) {
        String input = args.strip();

        int separator = input.indexOf(' ');

        String path;
        String rest;

        if (separator < 0) {
            path = input;
            rest = "";
        } else {
            path = input.substring(0, separator);
            rest = input.substring(separator + 1).strip();
        }

        String[] pathParts = path.split("/", -1);

        if (pathParts.length == 1) {
            return new Target(pathParts[0], "", rest, 1);
        }

        if (pathParts.length == 2) {
            return new Target(pathParts[0], pathParts[1], rest, 2);
        }

        return new Target(pathParts[0], null, rest, pathParts.length);
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
        Target target = parse(args);

        String pathError = validatePath(target, 2);
        if (pathError != null) {
            return pathError;
        }

        if (target.pathLength() < 2 || target.rest().isEmpty()) {
            return MISSING_ARG;
        }
        var collection = data.get(target.collection());

        if (collection == null) {
            return COL_NOT_FOUND;
        }

        if (!collection.containsKey(target.docId())){
            return NOT_FOUND;
        }

        try (JsonReader docReader = READER_FACTORY.createReader(new StringReader(collection.get(target.docId())))) {
            JsonObject jsonDoc = docReader.readObject();
            JsonObjectBuilder builder = Json.createObjectBuilder(jsonDoc);

            try (JsonReader inputReader = READER_FACTORY.createReader(new StringReader(target.rest()))) {
                JsonObject jsonInput = inputReader.readObject();
                for (var input : jsonInput.entrySet()) {
                    String attr = input.getKey();
                    var newVal = input.getValue();
                    builder.add(attr, newVal);
                }
            }

            JsonObject updated = builder.build();
            collection.put(target.docId(), updated.toString());
            return OK;
        } catch (JsonException e) {
            return INVALID_JSON;
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
        Target target = parse(args);

        String pathError = validatePath(target, 1);
        if (pathError != null) {
            return pathError;
        }

        if (target.rest().isEmpty()) {
            return MISSING_ARG;
        }

        var collection = data.get(target.collection());

        if (collection == null) {
            return COL_NOT_FOUND;
        }

        try (JsonReader docReader = READER_FACTORY.createReader(new StringReader(target.rest()))) {
            JsonObject jsonDoc = docReader.readObject();

            var id = UUID.randomUUID().toString();
            collection.put(id, jsonDoc.toString());
            return OK + " " + id;
        } catch (JsonException e) {
            return INVALID_JSON;
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
        Target target = parse(args);

        String pathError = validatePath(target, 2);
        if (pathError != null) {
            return pathError;
        }

        if (target.pathLength() < 2) {
            return MISSING_ARG;
        }

        var collection = data.get(target.collection());

        if (collection == null) {
            return COL_NOT_FOUND;
        }

        if (collection.remove(target.docId()) != null) {
            return OK;
        }

        return NOT_FOUND;
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
        Target target = parse(args);

        String pathError = validatePath(target, 1);
        if (pathError != null) {
            return pathError;
        }

        if (!target.rest().isEmpty()) {
            return INVALID_PATH;
        }

        ConcurrentMap<String, String> collection = new ConcurrentHashMap<>();

        if (data.putIfAbsent(target.collection(), collection) != null) {
            return EXISTS;
        }

        return OK;
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
        Target target = parse(args);

        String pathError = validatePath(target, 1);
        if (pathError != null) {
            return pathError;
        }

        if (!target.rest().isEmpty()) {
            return INVALID_PATH;
        }

        if (data.remove(target.collection()) != null) {
            return OK;
        }

        return NOT_FOUND;
    }
}
