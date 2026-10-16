package com.cityblock.backend.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.cityblock.backend.entity.enums.PlayerStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    private String id;
    private String username;
    private String token;
    private int balance;
    private PlayerStatus status;
    private String pieceId;
}
