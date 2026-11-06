package com.cityblock.backend.service;

import java.io.IOException;

import java.util.List;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.cityblock.backend.entity.Board;
import com.cityblock.backend.entity.Piece;
import com.cityblock.backend.entity.Player;
import com.cityblock.backend.entity.enums.Color;
import com.cityblock.backend.factory.BoardFactory;

@Service
public class BoardService {
    private final BoardFactory boardFactory;
    private final ObjectMapper objectMapper;

    public BoardService(BoardFactory boardFactory, ObjectMapper objectMapper) {
        this.boardFactory = boardFactory;
        this.objectMapper = objectMapper;
    }

    public Board initializeBoard(Board board, List<Player> players) throws IOException {
        boardFactory.initBoard(board);

        // pieces
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            Piece piece = new Piece();
            piece.setPlayerId(player.getId());
            piece.setColor(Color.values()[i]); // !works only for 6 players
            board.getPieces().add(piece);
        }

        return board;
    }
}
