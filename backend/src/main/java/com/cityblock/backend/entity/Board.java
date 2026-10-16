package com.cityblock.backend.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Board {
    private String id;
    private List<Piece> pieces;
    private List<Space> spaces;
}
