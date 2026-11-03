package com.cityblock.backend.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    private String id;
    private String code;
    private Board board;
    private List<Player> players;
    private String creatorId; 
    private String createdAt;
}
