package com.cityblock.backend.service;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.cityblock.backend.entity.Game;
import com.cityblock.backend.entity.Player;
import com.cityblock.backend.entity.Turn;
import com.cityblock.backend.entity.enums.GameStatus;
import com.cityblock.backend.entity.enums.PlayerStatus;
import com.cityblock.backend.entity.enums.TurnPhase;
import com.cityblock.backend.repository.DBRepository;
import com.cityblock.backend.service.BoardService.MoveResult;

@Service
public class GameService {
    private static final String ROOMS_COLLECTION = "rooms";
    private static final String TURNS_COLLECTION = "turns";
    private static final String GAME_PROPERTY = "game";

    private final BoardService boardService;
    private final BalanceService balanceService;
    private final DBRepository dbRepository;
    private final ObjectMapper objectMapper;

    public GameService(BoardService boardService, BalanceService balanceService, DBRepository dbRepository, ObjectMapper objectMapper) {
        this.boardService = boardService;
        this.balanceService = balanceService;
        this.dbRepository = dbRepository;
        this.objectMapper = objectMapper;
    }

    private Game getGameByRoomId(String roomId) {
        String response = dbRepository.findById(ROOMS_COLLECTION, roomId);
        if (response == null || !response.startsWith("OK ")) {
            throw new IllegalStateException("Room not found: " + roomId);
        }
        try {
            JsonNode roomNode = objectMapper.readTree(response.substring(3));
            JsonNode gameNode = roomNode.get(GAME_PROPERTY);
            if (gameNode == null || gameNode.isNull()) {
                throw new IllegalStateException("Game not started in room " + roomId);
            }
            return objectMapper.convertValue(gameNode, Game.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing game for room " + roomId, e);
        }
    }

    private Turn getTurn(String turnId) {
        if (turnId == null) {
            return null;
        }
        String response = dbRepository.findById(TURNS_COLLECTION, turnId);
        if (response == null || !response.startsWith("OK ")) {
            return null;
        }
        try {
            return objectMapper.readValue(response.substring(3), Turn.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing turn " + turnId, e);
        }
    }

    public MoveResult handleDiceRoll(String playerId, String token, String roomId) {
        if (playerId == null || token == null || roomId == null) {
            throw new IllegalArgumentException("Player ID, token, and room ID cannot be null");
        }

        Player player = balanceService.getPlayer(playerId);
        if (player == null || !token.equals(player.getToken())) {
            throw new IllegalArgumentException("Invalid player or token");
        }

        Game game = getGameByRoomId(roomId);
        if (game.getTurnOrder() == null || game.getTurnOrder().length == 0) {
            throw new IllegalStateException("Game is not active");
        }
        String currentPlayerId = game.getTurnOrder()[game.getCurrentTurnIndex()];
        if (!currentPlayerId.equals(playerId)) {
            throw new IllegalStateException("It is not your turn");
        }

        Turn turn = getTurn(game.getCurrentTurnId());
        if (turn == null || turn.getPhase() != TurnPhase.ROLL_DICE) {
            throw new IllegalStateException("Turn is not in the roll phase");
        }

        int diceRoll = boardService.generateDiceRoll();
        MoveResult moveResult = boardService.updatePosition(playerId, roomId, diceRoll);
        if (moveResult.position() < 0) {
            throw new IllegalStateException("Piece not found for player " + playerId);
        }

        endTurn(roomId, game, turn);

        return moveResult;
    }

    private void endTurn(String roomId, Game game, Turn currentTurn) {
        currentTurn.setPhase(TurnPhase.ENDED);
        dbRepository.update(TURNS_COLLECTION, currentTurn.getId(), objectMapper.valueToTree(currentTurn).toString());

        int nextIndex = nextActiveIndex(game);
        String nextPlayerId = game.getTurnOrder()[nextIndex];

        if (isGameFinished(roomId, game, currentTurn)) {
            return;
        }

        Turn nextTurn = new Turn();
        nextTurn.setGameId(roomId);
        nextTurn.setPlayerId(nextPlayerId);
        nextTurn.setNumber(currentTurn.getNumber() + 1);
        nextTurn.setPhase(TurnPhase.ROLL_DICE);
        String turnId = extractId(dbRepository.insert(TURNS_COLLECTION, objectMapper.valueToTree(nextTurn).toString()));
        nextTurn.setId(turnId);

        game.setCurrentTurnIndex(nextIndex);
        game.setCurrentTurnId(turnId);

        ObjectNode patch = objectMapper.createObjectNode();
        patch.set(GAME_PROPERTY, objectMapper.valueToTree(game));
        dbRepository.update(ROOMS_COLLECTION, roomId, patch.toString());
    }

    private boolean isGameFinished(String roomId, Game game, Turn currentTurn) {
        int active = 0;
        String winnerId = null;
        for (String id : game.getTurnOrder()) {
            Player p = balanceService.getPlayer(id);
            if (p == null || p.getStatus() != PlayerStatus.ELIMINATED) {
                active++;
                winnerId = id;
            }
        }
        if (active > 1) {
            return false;
        }

        currentTurn.setPhase(TurnPhase.ENDED);
        dbRepository.update(TURNS_COLLECTION, currentTurn.getId(),
            objectMapper.valueToTree(currentTurn).toString());

        game.setStatus(GameStatus.FINISHED);
        game.setWinnerId(active == 1 ? winnerId : null);
        ObjectNode patch = objectMapper.createObjectNode();
        patch.set(GAME_PROPERTY, objectMapper.valueToTree(game));
        dbRepository.update(ROOMS_COLLECTION, roomId, patch.toString());
        return true;
    }

    private int nextActiveIndex(Game game) {
        String[] order = game.getTurnOrder();
        int currentIndex = game.getCurrentTurnIndex();
        for (int step = 1; step <= order.length; step++) {
            int index = (currentIndex + step) % order.length;
            Player player = balanceService.getPlayer(order[index]);
            if (player == null || player.getStatus() != PlayerStatus.ELIMINATED) {
                return index;
            }
        }
        return currentIndex;
    }

    private String extractId(String response) {
        if (response == null || !response.startsWith("OK ")) {
            throw new IllegalStateException("DB error: " + response);
        }
        return response.substring(3);
    }
}
