package com.cityblock.backend.service;

import org.springframework.stereotype.Service;

import com.cityblock.backend.service.BoardService.MoveResult;

@Service
public class GameService {
    private final BoardService boardService;

    public GameService(BoardService boardService) {
        this.boardService = boardService;
    }

    public MoveResult handleDiceRoll(String playerId, String token, String roomId) {
        if (playerId == null || token == null || roomId == null) {
            throw new IllegalArgumentException("Player ID, token, and room ID cannot be null");
        }
        int diceRoll = boardService.generateDiceRoll();
        // Handle the dice roll result (e.g., update game state, notify players, etc.)
        // For example, you might want to update the game state or notify other players of the roll.
        MoveResult moveResult = boardService.updatePosition(token, roomId, diceRoll);
        //boolean passGo = moveResult.passGo();

        //if passgo: balanceService.addBalance(playerId, 200);
        return moveResult;
    }
}
