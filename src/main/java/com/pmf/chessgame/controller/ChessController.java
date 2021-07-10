package com.pmf.chessgame.controller;

import com.pmf.chessgame.storage.model.request.CreateGameRequest;
import com.pmf.chessgame.storage.model.request.GetChessBoardRequest;
import com.pmf.chessgame.storage.model.request.JoinGameRequest;
import com.pmf.chessgame.storage.model.request.MovePieceRequest;
import com.pmf.chessgame.storage.model.response.CreateGameResponse;
import com.pmf.chessgame.storage.model.response.GetChessBoardResponse;
import com.pmf.chessgame.storage.model.response.JoinGameResponse;
import com.pmf.chessgame.storage.model.response.MovePieceResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin("*")
@RestController
@RequestMapping(value = "chess")
public class ChessController {

    @PostMapping("create")
    public ResponseEntity<CreateGameResponse> createGame(@RequestBody CreateGameRequest request) {

        return ResponseEntity.ok(new CreateGameResponse());
    }

    @PostMapping("join")
    public void joinGame(@RequestBody String request) {

//        return ResponseEntity.ok(new JoinGameResponse());
    }

    @PostMapping("move")
    public ResponseEntity<MovePieceResponse> movePiece(@RequestBody MovePieceRequest request) {

        return ResponseEntity.ok(new MovePieceResponse());
    }

    @PostMapping("chessBoard")
    public ResponseEntity<GetChessBoardResponse> getChessBoard(@RequestBody GetChessBoardRequest request) {

        return ResponseEntity.ok(new GetChessBoardResponse());
    }
}
