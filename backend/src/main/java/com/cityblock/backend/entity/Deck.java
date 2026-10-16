package com.cityblock.backend.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.LinkedList;

import com.cityblock.backend.entity.enums.EventType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Deck {
    private String id;
    private LinkedList<Card> cards;
    private EventType type;
}
