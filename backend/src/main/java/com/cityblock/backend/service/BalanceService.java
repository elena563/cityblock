package com.cityblock.backend.service;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.cityblock.backend.entity.Player;
import com.cityblock.backend.entity.enums.PlayerStatus;
import com.cityblock.backend.repository.DBRepository;

@Service
public class BalanceService {
    private final DBRepository dbRepository;
    private final ObjectMapper objectMapper;

    private static final String PLAYERS_COLLECTION = "players";

    public BalanceService(DBRepository dbRepository, ObjectMapper objectMapper) {
        this.dbRepository = dbRepository;
        this.objectMapper = objectMapper;
    }

    public Player getPlayer(String playerId) {
        String response = dbRepository.findById(PLAYERS_COLLECTION, playerId);
        if (response == null || !response.startsWith("OK ")) {
            return null;
        }
        try {
            return objectMapper.readValue(response.substring(3), Player.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing player " + playerId, e);
        }
    }

    public void addBalance(String playerId, int amount) {
        Player player = getPlayer(playerId);
        if (player != null) {
            player.setBalance(player.getBalance() + amount);
            JsonNode playerNode = objectMapper.valueToTree(player);
            dbRepository.update(PLAYERS_COLLECTION, playerId, playerNode.toString());
        }
    }

    public void subtractBalance(String playerId, int amount) {
        Player player = getPlayer(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Player with ID " + playerId + " does not exist.");
        }
        if (player.getBalance() >= amount) {
            player.setBalance(player.getBalance() - amount);
        } else {
            player.setStatus(PlayerStatus.ELIMINATED);
            player.setBalance(0);
        }
        JsonNode playerNode = objectMapper.valueToTree(player);
        dbRepository.update(PLAYERS_COLLECTION, playerId, playerNode.toString());
    }

    public void payRent(String payerId, String payeeId, int amount) {
        if (getPlayer(payerId) == null) {
            throw new IllegalArgumentException("Payer with ID " + payerId + " does not exist.");
        }
        if (getPlayer(payeeId) == null) {
            throw new IllegalArgumentException("Payee with ID " + payeeId + " does not exist.");
        }

        subtractBalance(payerId, amount);
        addBalance(payeeId, amount);
    }
}
