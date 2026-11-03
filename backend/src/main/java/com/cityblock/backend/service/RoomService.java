package com.cityblock.backend.service;

import org.springframework.stereotype.Service;
import com.cityblock.backend.repository.DBRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.security.SecureRandom;

import com.cityblock.backend.entity.Room;

@Service
public class RoomService {
    private final DBRepository dbRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();


    public RoomService(DBRepository dbRepository) {
        this.dbRepository = dbRepository;
    }

    public String createPlayer(String username) {
        ObjectNode player = objectMapper.createObjectNode();
        player.put("username", username);
        return extractId(dbRepository.insert("players", player.toString()));
    }

    public Room createRoom(String creatorId) {
        ObjectNode room = objectMapper.createObjectNode();
        room.put("code", generateRoomCode());
        room.put("creatorId", creatorId);
        room.putArray("players").addObject().put("id", creatorId);

        String id = extractId(dbRepository.insert("rooms", room.toString()));
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
        String response = dbRepository.findAll("rooms", "code = " + code);
        return response != null && response.startsWith("OK ") && response.substring(3).equals("[]");
    }

    public void joinRoom(String roomCode, String playerId) {
        // Find the room by code
        String response = dbRepository.findAll("rooms", "code = " + roomCode);
        if (response == null || !response.startsWith("OK ")) {
            throw new RuntimeException("DB error: " + response);
        }

        try {
            JsonNode rooms = objectMapper.readTree(response.substring(3));
            if (!rooms.isArray() || rooms.size() == 0) {
                throw new RuntimeException("Room not found");
            }
            
            JsonNode room = rooms.get(0);
            String roomId = room.get("id").asText();
            ArrayNode players = (ArrayNode) room.get("players");

            for (JsonNode player : players) {
                if (player.get("id").asText().equals(playerId)) {
                    throw new RuntimeException("Player already in room");
                }
            }
            if (players.size() >= 6) {
                throw new RuntimeException("Room is full");
            }
            players.addObject().put("id", playerId);

            ObjectNode patch = objectMapper.createObjectNode();
            patch.set("players", players);
            dbRepository.update("rooms", roomId, patch.toString());
            
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing room data", e);
        }
    }

    public String startGame(String roomId) {
        if (roomId == null || roomId.isEmpty()) {
            throw new RuntimeException("Room not found");
        }

        String room = dbRepository.findById("rooms", roomId);
        if (room == null || !room.startsWith("OK ")) {
            throw new RuntimeException("Room not found");
        }

        ObjectNode game = objectMapper.createObjectNode();
        game.put("roomId", roomId);
        game.put("status", "ONGOING");
        return extractId(dbRepository.insert("games", game.toString()));
    }

    private String extractId(String response) {
       if (response == null || response.isEmpty() || !response.startsWith("OK ")) {
           throw new RuntimeException("DB error: " + response);
       }
       return response.substring(3);
    }
}
