package com.pmf.chessgame.storage.model.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "game")
public class GameEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "player_count")
    private Long playerCount;

    private Boolean finished;

    private String turn;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "game_id", referencedColumnName = "id")
    private List<ChessBoardEntity> boards = new ArrayList<>();
}
