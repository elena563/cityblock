# SockDB - TCP Protocol

SockDB is a lightweight, schema-free document database exposed over a raw TCP socket using a custom text-based protocol. Each document is stored as a JSON object inside a named collection, and identified by a UUID v4 generated at insert time.

## Connection

Connect to the server over TCP. The protocol is line-oriented: every request and every response is terminated by a newline character (`\n`). Commands are case-sensitive and must be lowercase.


## Request format

```
COMMAND /path [payload]
```

- `COMMAND` - one of the commands listed below
- `/path` - collection or collection/document identifier
- `payload` - optional JSON object, on the same line, separated by a single space


## Response format

Every response is a single line terminated by `\n`.

| Response | Meaning |
|---|---|
| `OK` | Command succeeded, no data to return |
| `OK <uuid>` | Document created; UUID of the new document |
| `OK {...}` | Single document returned as JSON |
| `OK [...]` | List of documents returned as JSON array |
| `ERROR NOT_FOUND` | Document or collection does not exist |
| `ERROR ALREADY_EXISTS` | Collection already exists |
| `ERROR COLLECTION_NOT_FOUND` | Target collection does not exist |
| `ERROR INVALID_QUERY` | Malformed filter expression |
| `ERROR INTERNAL` | Unexpected server error |


## Commands

### `create` - create a collection

```
create /collection
```

Creates a new empty collection. Returns `ERROR ALREADY_EXISTS` if a collection with that name is already present.

**Example:**
```
create /players
OK
```


### `drop` - delete a collection

```
drop /collection
```

Deletes the collection and all its documents. Returns `ERROR NOT_FOUND` if the collection does not exist.

**Example:**
```
drop /players
OK
```


### `insert` - insert a document

```
insert /collection {"key": "value", ...}
```

Inserts a new document into the collection. The server generates a UUID v4 as the document ID and returns it.

**Example:**
```
insert /players {"username": "alice", "score": 0}
OK 7f3c2d1a-84b5-4e29-a3f1-0c9d2e5b6f78
```


### `read` - read documents

**Read all documents in a collection:**
```
read /collection
```

**Read a single document by ID:**
```
read /collection/id
```

**Read with a filter:**
```
read /collection filter EXPRESSION
```

Returns `OK [...]` for collection reads and `OK {...}` for single-document reads. Returns `ERROR NOT_FOUND` if the document or collection does not exist.

**Examples:**
```
read /players
OK [{"username": "alice", "score": 0}, ...]

read /players/7f3c2d1a-84b5-4e29-a3f1-0c9d2e5b6f78
OK {"username": "alice", "score": 0}

read /players filter score > 10
OK [{"username": "bob", "score": 42}]
```


### `write` - partially update a document

```
write /collection/id {"key": "new_value"}
```

Performs a partial update (patch) on the specified document. Only the properties included in the payload are updated; all other properties are left unchanged. This is not a full replace.

**Example:**
```
write /players/7f3c2d1a-84b5-4e29-a3f1-0c9d2e5b6f78 {"score": 15}
OK
```

The document after the operation:
```json
{"username": "alice", "score": 15}
```


### `delete` - delete documents

**Delete a single document by ID:**
```
delete /collection/id
```

**Delete with a filter:**
```
delete /collection filter EXPRESSION
```

Returns `ERROR NOT_FOUND` if the document or collection does not exist.

**Examples:**
```
delete /players/7f3c2d1a-84b5-4e29-a3f1-0c9d2e5b6f78
OK

delete /players filter score < 1
OK
```


## Filter expressions

Filters apply to the top-level properties of documents only. Nested properties are not supported in filter expressions.

### Comparison operators

| Operator | Meaning |
|---|---|
| `=` | equal |
| `!=` | not equal |
| `>` | greater than |
| `<` | less than |
| `>=` | greater than or equal |
| `<=` | less than or equal |

### Logical connectives

Conditions can be combined with `AND` and `OR`.

**Examples:**
```
read /players filter score > 10
read /players filter score >= 5 AND score <= 100
read /players filter username = alice OR username = bob
```