package com.cityblock.backend.service;

import java.io.IOException;

import org.springframework.stereotype.Service;

import com.cityblock.backend.entity.Board;
import com.cityblock.backend.factory.BoardFactory;

@Service
public class BoardService {
    private final BoardFactory boardFactory;

    public BoardService(BoardFactory boardFactory) {
        this.boardFactory = boardFactory;
    }

    public Board initializeBoard(Board board) throws IOException {
        boardFactory.initBoard(board);
        return board;
    }
}
