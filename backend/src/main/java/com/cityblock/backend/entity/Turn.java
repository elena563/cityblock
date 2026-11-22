package com.cityblock.backend.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.cityblock.backend.entity.enums.TurnPhase;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Turn {
    private String id;
    private int number;
    private TurnPhase phase;
    private String gameId;
    private String playerId;
}
