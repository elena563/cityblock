package com.cityblock.backend.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.cityblock.backend.entity.enums.GameStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Game {
    private String id;
    private String roomId;
    private GameStatus status;
}
