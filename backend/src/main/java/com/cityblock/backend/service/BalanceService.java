package com.cityblock.backend.service;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import com.cityblock.backend.entity.Player;
import com.cityblock.backend.repository.DBRepository;

@Service
public class BalanceService {
    private final DBRepository dbRepository;
    private final ObjectMapper objectMapper;

    public BalanceService(DBRepository dbRepository, ObjectMapper objectMapper) {
        this.dbRepository = dbRepository;
        this.objectMapper = objectMapper;
    }

    public void addBalance(String playerId, int amount) {
        String playerJson = dbRepository.findById("players", playerId);
        Player player = objectMapper.convertValue(playerJson, Player.class);
        if (player != null) {
            player.setBalance(player.getBalance() + amount);
            JsonNode playerNode = objectMapper.valueToTree(player);
            dbRepository.update("players", playerId, playerNode.toString());
        }
    }
}
