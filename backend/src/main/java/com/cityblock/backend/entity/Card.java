package com.cityblock.backend.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.cityblock.backend.entity.enums.CardEffect;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Card {
    private String id;
    private String name;
    private String description;
    private CardEffect effect;
}
