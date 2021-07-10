package com.pmf.chessgame.controller;

import com.pmf.chessgame.storage.model.entity.ChessBoardEntity;
import com.pmf.chessgame.storage.model.entity.GameEntity;
import com.pmf.chessgame.storage.model.request.MovePieceRequest;
import com.pmf.chessgame.storage.repository.GameEntityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@CrossOrigin("*")
@RestController
@RequestMapping(value = "chess")
public class ChessController {

    private final GameEntityRepository gameEntityRepository;

    @Autowired
    public ChessController(GameEntityRepository gameEntityRepository) {
        this.gameEntityRepository = gameEntityRepository;
    }

    @PostMapping("getCount")
    public Long getPlayersCount(@RequestBody String name) {
        Optional<GameEntity> game = gameEntityRepository.findByName(name);
        if (game.isPresent()) {
            return game.get().getPlayerCount();
        }
        return 0L;
    }

    @PostMapping("getBoard")
    public String getBoard(@RequestBody String name) {
        Optional<GameEntity> game = gameEntityRepository.findByName(name);
        return game.map(gameEntity -> gameEntity.getBoards().get(gameEntity.getBoards().size() - 1).getBoard()).orElse(null);
    }

    @PostMapping("join")
    public Long joinGame(@RequestBody String name) {
        Optional<GameEntity> game = gameEntityRepository.findByName(name);

        if (game.isPresent()) {
            GameEntity gameEntity = game.get();
            if (gameEntity.getPlayerCount() == 2) {
                return 2L;
            } else {
                gameEntity.setPlayerCount(gameEntity.getPlayerCount() + 1);
                gameEntityRepository.save(gameEntity);
                return 1L;
            }
        } else {
            List<ChessBoardEntity> boards = new ArrayList<>();
            ChessBoardEntity chessBoardEntity = new ChessBoardEntity();
            boards.add(chessBoardEntity);
            chessBoardEntity.setBoard("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
            GameEntity gameEntity = new GameEntity();
            gameEntity.setPlayerCount(1L);
            gameEntity.setName(name);
            gameEntity.setTurn("w");
            gameEntity.setFinished(false);
            gameEntity.setBoards(boards);
            gameEntityRepository.save(gameEntity);
            return 0L;
        }
    }

    @PostMapping("boardChanged")
    public void boardChanged(@RequestBody MovePieceRequest request) {
        Optional<GameEntity> game = gameEntityRepository.findByName(request.getName());
        if (game.isPresent()) {
            GameEntity gameEntity = game.get();
            ChessBoardEntity chessBoardEntity = new ChessBoardEntity();
            chessBoardEntity.setBoard(request.getBoard());
            List<ChessBoardEntity> list = gameEntity.getBoards();
            list.add(chessBoardEntity);
            gameEntity.setBoards(list);
            gameEntityRepository.save(gameEntity);
        }
    }
}
