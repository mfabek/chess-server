package com.pmf.chessgame.storage.model.entity;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "board")
public class ChessBoardEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String board;

    private String move;
}
