package com.cityblock.backend.service;

import java.io.IOException;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import com.cityblock.backend.entity.Board;
import com.cityblock.backend.entity.Piece;
import com.cityblock.backend.entity.Player;
import com.cityblock.backend.entity.enums.Color;
import com.cityblock.backend.factory.BoardFactory;
import com.cityblock.backend.repository.DBRepository;

@Service
public class BoardService {
    private final BoardFactory boardFactory;
    private final DBRepository dbRepository;
    private final ObjectMapper objectMapper;

    protected record MoveResult(int position, boolean passGo) {}

    public BoardService(BoardFactory boardFactory, DBRepository dbRepository, ObjectMapper objectMapper) {
        this.boardFactory = boardFactory;
        this.dbRepository = dbRepository;
        this.objectMapper = objectMapper;
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
        return (int) (Math.random() * 6) + 1;
    }

    private Board getBoardByRoomId(String roomId) {
        String boardJson = dbRepository.findById("rooms", roomId);
        return objectMapper.convertValue(boardJson, Board.class);
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
        JsonNode boardJson = objectMapper.valueToTree(board);
        dbRepository.update("rooms", roomId, boardJson.toString());

        boolean passGo = this.checkPassGo(playerId, newPosition, currentPosition);

        return new MoveResult(newPosition, passGo);
    }

    private boolean checkPassGo(String playerId, int newPosition, int currentPosition) {
        if (currentPosition < newPosition) {
            return false;
        }
        return true;
    }//TODO: class turn + start of game
}
