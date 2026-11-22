package com.cityblock.backend.service;

import java.io.IOException;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.cityblock.backend.entity.Board;
import com.cityblock.backend.entity.Ownable;
import com.cityblock.backend.entity.Piece;
import com.cityblock.backend.entity.Player;
import com.cityblock.backend.entity.Space;
import com.cityblock.backend.entity.TaxSpace;
import com.cityblock.backend.entity.enums.Color;
import com.cityblock.backend.entity.enums.SpaceType;
import com.cityblock.backend.factory.BoardFactory;
import com.cityblock.backend.repository.DBRepository;

@Service
public class BoardService {
    private static final Random RANDOM = new Random();
    private final BoardFactory boardFactory;
    private final DBRepository dbRepository;
    private final ObjectMapper objectMapper;
    private final BalanceService balanceService;
    private static final int PASS_GO_BONUS = 200;

    public record MoveResult(int position, boolean passGo) {}

    public BoardService(BoardFactory boardFactory, DBRepository dbRepository, ObjectMapper objectMapper, BalanceService balanceService) {
        this.boardFactory = boardFactory;
        this.dbRepository = dbRepository;
        this.objectMapper = objectMapper;
        this.balanceService = balanceService;
    }

    public Board initializeBoard(Board board, List<Player> players) throws IOException {
        boardFactory.initBoard(board);

        // pieces
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            Piece piece = new Piece();
            piece.setId(UUID.randomUUID().toString());
            piece.setPlayerId(player.getId());
            piece.setColor(Color.values()[i]); // !works only for 6 players
            board.getPieces().add(piece);
        }

        return board;
    }

    public int generateDiceRoll() {
        return RANDOM.nextInt(6) + 1;
    }

    private Board getBoardByRoomId(String roomId) {
        String response = dbRepository.findById("rooms", roomId);
        if (response == null || !response.startsWith("OK ")) {
            throw new IllegalStateException("Room not found: " + roomId);
        }
        try {
            JsonNode roomNode = objectMapper.readTree(response.substring(3));
            return objectMapper.convertValue(roomNode.get("board"), Board.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing board for room " + roomId, e);
        }
    }

    private Piece findPiece(Board board, String playerId) {
        return board.getPieces().stream()
            .filter(p -> playerId.equals(p.getPlayerId()))
            .findFirst()
            .orElse(null);
    }

    public MoveResult updatePosition(String playerId, String roomId, int diceRoll) {
        Board board = getBoardByRoomId(roomId);
        Piece piece = findPiece(board, playerId);
        if (piece == null) {
            return new MoveResult(-1, false);
        }
        int currentPosition = piece.getPosition();
        int newPosition = (currentPosition + diceRoll) % board.getSpaces().size();
        piece.setPosition(newPosition);
        ObjectNode patch = objectMapper.createObjectNode();
        patch.set("board", objectMapper.valueToTree(board));
        dbRepository.update("rooms", roomId, patch.toString());

        boolean passGo = this.checkPassGo(newPosition, currentPosition);
        if (passGo) {
            balanceService.addBalance(playerId, PASS_GO_BONUS);
        }

        checkSpaceType(board, newPosition, playerId);

        return new MoveResult(newPosition, passGo);
    }

    private boolean checkPassGo(int newPosition, int currentPosition) {
        return currentPosition >= newPosition;
    }

    private void checkSpaceType(Board board, int position, String playerId) {
        Space space = board.getSpaces().get(position);
        SpaceType spaceType = space.getType();
        if (SpaceType.PROPERTY.equals(spaceType) || SpaceType.UTILITY.equals(spaceType) || SpaceType.METRO_STATION.equals(spaceType)) {
            if (!(space instanceof Ownable ownable)) {
                return;
            }

            String ownerId = ownable.getOwnerPlayerId();
            if (ownerId == null) {
                // Handle unowned property logic (e.g., allow purchase)
                // You can implement the logic to allow the player to purchase the property here.
                return;
            }
            if (!ownerId.equals(playerId)) {
                balanceService.payRent(playerId, ownerId, ownable.getBaseRent());
            }
        } else if (SpaceType.TAX.equals(spaceType)) {
            if (!(space instanceof TaxSpace tax)) {
                return;
            }
            balanceService.subtractBalance(playerId, tax.getTaxAmount());
        } else if (SpaceType.CHANCE.equals(spaceType) || SpaceType.TROUBLE.equals(spaceType)) {
            // Handle chance space logic (e.g., draw a card)
            // You can implement the logic to draw a chance card here.
        }
    }
}
