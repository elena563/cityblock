package com.cityblock.backend.service;

import org.springframework.stereotype.Service;
import com.cityblock.backend.repository.DBRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.cityblock.backend.entity.Board;
import com.cityblock.backend.entity.Player;
import com.cityblock.backend.entity.Room;
import com.cityblock.backend.entity.Turn;
import com.cityblock.backend.entity.enums.TurnPhase;

@Service
public class RoomService {
    private final DBRepository dbRepository;
    private final BoardService boardService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String PLAYERS_COLLECTION = "players";
    private static final String ROOMS_COLLECTION = "rooms";
    private static final String BOARD_PROPERTY = "board";
    private static final String GAME_PROPERTY = "game";
    private static final String TURNS_COLLECTION = "turns";

    public record PlayerCredentials(String id, String token) {}

    public RoomService(DBRepository dbRepository, BoardService boardService) {
        this.dbRepository = dbRepository;
        this.boardService = boardService;
    }

    public PlayerCredentials createPlayer(String username) {
        ObjectNode player = objectMapper.createObjectNode();
        player.put("username", username);

        String token = UUID.randomUUID().toString();
        player.put("token", token);
        player.put("balance", 1500);
        player.put("status", "ACTIVE");
        String id = extractId(dbRepository.insert(PLAYERS_COLLECTION, player.toString()));
        return new PlayerCredentials(id, token);
    }

    public Room createRoom(String creatorId) {
        ObjectNode room = objectMapper.createObjectNode();
        room.put("code", generateRoomCode());
        room.put("creatorId", creatorId);
        room.putArray(PLAYERS_COLLECTION).addObject().put("id", creatorId);

        ObjectNode board = objectMapper.createObjectNode();
        room.set(BOARD_PROPERTY, board);

        String id = extractId(dbRepository.insert(ROOMS_COLLECTION, room.toString()));
        room.put("id", id);
        return objectMapper.convertValue(room, Room.class);
    }

    public String generateRoomCode() {
        while (true) {
            StringBuilder code = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                code.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            String candidate = code.toString();
            if (isCodeAvailable(candidate)) {
                return candidate;
            }
        }
    }

    private boolean isCodeAvailable(String code) {
        String response = dbRepository.findByField(ROOMS_COLLECTION, "code", code);
        return response != null && response.startsWith("OK ") && response.substring(3).equals("[]");
    }

    public void joinRoom(String roomCode, String playerId) {
        // Find the room by code
        String response = dbRepository.findByField(ROOMS_COLLECTION, "code", roomCode);
        if (response == null || !response.startsWith("OK ")) {
            throw new IllegalStateException("DB error: " + response);
        }

        try {
            JsonNode rooms = objectMapper.readTree(response.substring(3));
            if (!rooms.isArray() || rooms.size() == 0) {
                throw new IllegalStateException("Room not found");
            }
            
            JsonNode room = rooms.get(0);
            String roomId = room.get("id").asText();
            ArrayNode players = (ArrayNode) room.get(PLAYERS_COLLECTION);

            for (JsonNode player : players) {
                if (player.get("id").asText().equals(playerId)) {
                    throw new IllegalStateException("Player already in room");
                }
            }
            if (players.size() >= 6) {
                throw new IllegalStateException("Room is full");
            }
            players.addObject().put("id", playerId);

            ObjectNode patch = objectMapper.createObjectNode();
            patch.set(PLAYERS_COLLECTION, players);
            dbRepository.update(ROOMS_COLLECTION, roomId, patch.toString());
            
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing room data", e);
        }
    }

    public String startGame(String roomId) {
        if (roomId == null || roomId.isEmpty()) {
            throw new IllegalStateException("Room not found");
        }

        String room = dbRepository.findById(ROOMS_COLLECTION, roomId);
        if (room == null || !room.startsWith("OK ")) {
            throw new IllegalStateException("Room not found");
        }

        List<Player> players;
        Board board;

        try {
            JsonNode roomNode = objectMapper.readTree(room.substring(3));
            if (roomNode.hasNonNull(GAME_PROPERTY)) {
                throw new IllegalStateException("Game already started");
            }
            Room roomData = objectMapper.treeToValue(roomNode, Room.class);
            JsonNode boardNode = roomNode.get(BOARD_PROPERTY);
            if (boardNode == null || boardNode.isNull()) {
                throw new IllegalStateException("Room has no board");
            }
            board = objectMapper.convertValue(boardNode, Board.class);
            players = roomData.getPlayers();
            if (players == null || players.isEmpty()) {
                throw new IllegalStateException("Room has no players");
            }
            boardService.initializeBoard(board, players);
        } catch (IOException e) {
            throw new RuntimeException("Error processing room data", e);
        }

        List<String> playerIds = new ArrayList<>();
        for (Player player : players) {
            playerIds.add(player.getId());
        }
        Collections.shuffle(playerIds);

        ensureTurnsCollection();

        ObjectNode game = objectMapper.createObjectNode();
        game.put("id", roomId);
        game.put("roomId", roomId);
        game.put("status", "ONGOING");

        ArrayNode turnOrder = game.putArray("turnOrder");
        for (String playerId : playerIds) {
            turnOrder.add(playerId);
        }
        game.put("currentTurnIndex", 0);

        Turn turn = new Turn();
        turn.setGameId(roomId);
        turn.setPlayerId(playerIds.get(0));
        turn.setNumber(1);
        turn.setPhase(TurnPhase.ROLL_DICE);
        String turnId = extractId(dbRepository.insert(TURNS_COLLECTION, objectMapper.valueToTree(turn).toString()));
        turn.setId(turnId);
        game.put("currentTurnId", turnId);

        ObjectNode patch = objectMapper.createObjectNode();
        patch.set(BOARD_PROPERTY, objectMapper.valueToTree(board));
        patch.set(GAME_PROPERTY, game);
        dbRepository.update(ROOMS_COLLECTION, roomId, patch.toString());

        return roomId;
    }

    private void ensureTurnsCollection() {
        dbRepository.createCollection(TURNS_COLLECTION);
    }

    private String extractId(String response) {
       if (response == null || response.isEmpty() || !response.startsWith("OK ")) {
           throw new IllegalStateException("DB error: " + response);
       }
       return response.substring(3);
    }
}
