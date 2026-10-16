package com.cityblock.backend.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.cityblock.backend.entity.enums.Color;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Piece {
    private String id;
    private Color color;
    private String spaceId;
}
