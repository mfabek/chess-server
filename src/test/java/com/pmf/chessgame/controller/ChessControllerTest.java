package com.pmf.chessgame.controller;

import com.pmf.chessgame.storage.model.entity.ChessBoardEntity;
import com.pmf.chessgame.storage.model.entity.GameEntity;
import com.pmf.chessgame.storage.model.request.MovePieceRequest;
import com.pmf.chessgame.storage.model.response.MovePieceResponse;
import com.pmf.chessgame.storage.repository.GameEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class ChessControllerTest {

    @Autowired
    ChessController chessController;
    @Autowired
    GameEntityRepository gameEntityRepository;

//    Ovo se zapravo testira u jointGame()
//    @Test
//    void getPlayersCount() {
//
//    }

    @Test
    void getBoard() {
        Random random = new Random();
        String name = "TestnoIme" + Math.abs(random.nextInt(1000000));

        // getBoard zapravo ne vraca tablicu vec posljednji potez
        long l = chessController.joinGame(name);
        assertEquals(0L, l);
        //Stvorena je nova igra, mora imati prazan pocetni potez
        assertEquals("", chessController.getBoard(name));

        //Izbrisimo stvorenu igru
        Optional<GameEntity> game = gameEntityRepository.findByName(name);
        gameEntityRepository.delete(game.get());
    }

    @Test
    void reset() {
        //Stvorimo novu igru s nekim random podacima
        Random random = new Random();
        String name = "TestnoIme" + Math.abs(random.nextInt(1000000));
        List<ChessBoardEntity> boards = new ArrayList<>();
        ChessBoardEntity chessBoardEntity = new ChessBoardEntity();
        chessBoardEntity.setBoard("ovdje moze svasta pisati");
        chessBoardEntity.setMove("d2d4");
        boards.add(chessBoardEntity);
        GameEntity gameEntity = new GameEntity();
        gameEntity.setPlayerCount(1L);
        gameEntity.setName(name);
        gameEntity.setTurn("b");
        gameEntity.setFinished(true);
        gameEntity.setBoards(boards);
        gameEntityRepository.save(gameEntity);

        chessController.reset(name);

        Optional<GameEntity> game = gameEntityRepository.findByName(name);
        assertNotEquals(Optional.empty(), game);

        // If prolazi ako i samo ako prethodni assert prolazi
        if (game.isPresent()){
            assertEquals(2L, game.get().getPlayerCount());
            assertEquals("w", game.get().getTurn());
            assertEquals(1, game.get().getBoards().size());
            assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                    game.get().getBoards().get(0).getBoard());
            gameEntityRepository.delete(game.get());
        }

    }


    @Test
    void joinGame() {
        Random random = new Random();
        String name = "TestnoIme" + Math.abs(random.nextInt(1000000));

        //Provjerimo da takva igra ne postoji bazi
        assertEquals(Optional.empty(), gameEntityRepository.findByName(name));
        long l = chessController.joinGame(name);
        assertEquals(0L, l);
        //Sad provjerimo i da igra postoji u bazi
        assertNotEquals(Optional.empty(), gameEntityRepository.findByName(name));
        l = chessController.joinGame(name);
        assertEquals(1L, l);
        l = chessController.joinGame(name);
        assertEquals(2L, l);

        //Izbrisimo stvorenu igru
        Optional<GameEntity> game = gameEntityRepository.findByName(name);
        gameEntityRepository.delete(game.get());
    }

    @Test
    void boardChanged() {
        Random random = new Random();
        String name = "TestnoIme" + Math.abs(random.nextInt(1000000));
        //Stvorimo novu igru s join
        chessController.joinGame(name);

        //Stvorimo request koji odgovara pomicanju pijuna s c2 na c4
        // Taj request proslijeđujemo funkciji boardChanged
        MovePieceRequest request = new MovePieceRequest();
        String newBoard = "rnbqkbnr/pppppppp/8/8/2P5/8/PP1PPPPP/RNBQKBNR b KQkq c3 0 1";
        request.setBoard(newBoard);
        request.setMove("c2c4");
        request.setName(name);

        MovePieceResponse response = chessController.boardChanged(request);
        assertNotNull(response);
        assertEquals(false, response.getIsCheckmate());
        assertEquals("", response.getWhoWon());

        // Provjerimo da je u bazi spremljena nova tablica
        Optional<GameEntity> game = gameEntityRepository.findByName(name);
        assertEquals(newBoard, game.get().getBoards().get(game.get().getBoards().size() - 1).getBoard());

        //Izbrisimo ovu igru
        gameEntityRepository.delete(game.get());
    }

    @Test
    void getBoardLook() {
        //ChessController chessController = new ChessController(null);
        //Slijedeci string predstavlja stanje ploce na pocetku
        String pocetnoStanje = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        ChessController.FieldType[][] result = chessController.getBoardLook(pocetnoStanje);

        //Assert
        //Na (0,0) i (0,7) mora bit crna kula
        assertEquals(ChessController.FieldType.BROOK, result[0][0]);
        assertEquals(ChessController.FieldType.BROOK, result[0][7]);
        //Kralj je na (0,4)
        assertEquals(ChessController.FieldType.BKING, result[0][4]);
        //Pawn na svim (1,x)
        assertEquals(ChessController.FieldType.BPAWN, result[1][0]);
        assertEquals(ChessController.FieldType.BPAWN, result[1][6]);

        //Slicno za bijele figure
        assertEquals(ChessController.FieldType.WKNIGHT, result[7][1]);
        assertEquals(ChessController.FieldType.WKNIGHT, result[7][6]);

        assertEquals(ChessController.FieldType.WKING, result[7][4]);

        assertEquals(ChessController.FieldType.WPAWN, result[6][0]);
        assertEquals(ChessController.FieldType.WPAWN, result[6][6]);
    }

    @Test
    void isWhiteKingCheck() {
        // Stanje ploce mozemo vidjeti na https://grzegorz103.github.io/ngx-chess-board/chess-board/index.html
        // unosom u tekstualno polje FEN i klikom na Set FEN
        //ChessController chessController = new ChessController(null);
        String table = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

        //Assert
        //table je pocetno stanje, mora bit false
        assertFalse(chessController.isWhiteKingCheck(chessController.getBoardLook(table)));
        //Tri primjera kad nema saha
        table = "rnbq1bnr/ppp1kp1p/4p3/1B1p2p1/4P1P1/2N2Q2/PPPP1P1P/R1B1K1NR b KQ - 0 5";
        assertFalse(chessController.isWhiteKingCheck(chessController.getBoardLook(table)));
        table = "rnbq1bnr/p3k2p/p1p1p3/1P1p1pp1/4P1P1/B1N2Q2/P1PP1P1P/R3K1NR b KQ - 0 9"; //sah je tu na crnom
        assertFalse(chessController.isWhiteKingCheck(chessController.getBoardLook(table)));
        table = "rnbq1bnr/p4k1p/p1p5/1P3pp1/6P1/B3K2Q/P1PP1P1P/R5NR b - - 0 13";
        assertFalse(chessController.isWhiteKingCheck(chessController.getBoardLook(table)));

        //Tri primjera kad je sah
        table = "rnb1qbnr/p4k1p/p1p5/1P3pp1/6P1/B3K2Q/P1PP1P1P/R5NR w - - 0 14";
        assertTrue(chessController.isWhiteKingCheck(chessController.getBoardLook(table)));
        table = "rnb1kbnr/pppp1ppp/4p3/8/4PP1q/8/PPPP2PP/RNBQKBNR w KQkq - 0 3";
        assertTrue(chessController.isWhiteKingCheck(chessController.getBoardLook(table)));
        table = "rnb1kb1r/pp1p1ppp/4p3/2p5/2P2Pn1/8/PP1P1K1P/RNBQ1BNR w kq - 0 9";
        assertTrue(chessController.isWhiteKingCheck(chessController.getBoardLook(table)));
    }

    @Test
    void isBlackKingCheck() {
        // Stanje ploce mozemo vidjeti na https://grzegorz103.github.io/ngx-chess-board/chess-board/index.html
        // unosom u tekstualno polje FEN i klikom na Set FEN
        //ChessController chessController = new ChessController(null);
        String table;
        //Opet provjeravamo tri primjera kad nema saha i 3 kad ima
        //Tri primjera kad nema saha
        table = "rnbq1bnr/ppp1kp1p/4p3/1B1p2p1/4P1P1/2N2Q2/PPPP1P1P/R1B1K1NR b KQ - 0 5";
        assertFalse(chessController.isBlackKingCheck(chessController.getBoardLook(table)));
        table = "rnb1kb1r/pp1p1ppp/4p3/2p5/2P2Pn1/8/PP1P1K1P/RNBQ1BNR w kq - 0 9";
        assertFalse(chessController.isBlackKingCheck(chessController.getBoardLook(table)));
        table = "rnbq1bnr/p4k1p/p1p5/1P3pp1/6P1/B3K2Q/P1PP1P1P/R5NR b - - 0 13";
        assertFalse(chessController.isBlackKingCheck(chessController.getBoardLook(table)));

        //Tri primjera kad je sah
        table = "r1b2b1r/pp1pkppp/2n1p3/2p5/2P2PnQ/5K2/PP1P3P/RNB2BNR b - - 0 11";
        assertTrue(chessController.isBlackKingCheck(chessController.getBoardLook(table)));
        table = "rnbqkbnr/ppp1p2p/3p1p2/6p1/Q2P4/2P2P2/PP2P1PP/RNB1KBNR b KQkq - 0 4";
        assertTrue(chessController.isBlackKingCheck(chessController.getBoardLook(table)));
        table = "rnbq1b1r/ppp1pk1p/3p1p1n/7B/Q2P4/2P1p3/PP4PP/RNB1K1NR b KQ - 0 8";
        assertTrue(chessController.isBlackKingCheck(chessController.getBoardLook(table)));
    }

    @Test
    void isWhiteKingCheckmate() {
        // Stanje ploce mozemo vidjeti na https://grzegorz103.github.io/ngx-chess-board/chess-board/index.html
        // unosom u tekstualno polje FEN i klikom na Set FEN
        //ChessController chessController = new ChessController(null);
        String table;

        //Dva primjera kada nema ni saha (ista kao u testu isWhiteKingCheck())
        table = "rnbq1bnr/ppp1kp1p/4p3/1B1p2p1/4P1P1/2N2Q2/PPPP1P1P/R1B1K1NR b KQ - 0 5";
        assertFalse(chessController.isWhiteKingCheckmate(chessController.getBoardLook(table)));
        table = "rnbq1bnr/p3k2p/p1p1p3/1P1p1pp1/4P1P1/B1N2Q2/P1PP1P1P/R3K1NR b KQ - 0 9"; //sah je tu na crnom
        assertFalse(chessController.isWhiteKingCheckmate(chessController.getBoardLook(table)));

        //Dva primjera kada ima saha ali nema mata (ista kao u testu isWhiteKingCheck())
        table = "rnb1qbnr/p4k1p/p1p5/1P3pp1/6P1/B3K2Q/P1PP1P1P/R5NR w - - 0 14";
        assertFalse(chessController.isWhiteKingCheckmate(chessController.getBoardLook(table)));
        table = "rnb1kbnr/pppp1ppp/4p3/8/4PP1q/8/PPPP2PP/RNBQKBNR w KQkq - 0 3";
        assertFalse(chessController.isWhiteKingCheckmate(chessController.getBoardLook(table)));

        //Dva primjera sah-mata
        table = "rnb1kbnr/pppp1ppp/4p3/8/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 0 3";
        assertTrue(chessController.isWhiteKingCheckmate(chessController.getBoardLook(table)));
        table = "rnb1k1nr/ppp2ppp/8/8/8/2N3b1/PPPPP3/R1BQKBNR w KQkq - 0 7";
        assertTrue(chessController.isWhiteKingCheckmate(chessController.getBoardLook(table)));
    }

    @Test
    void isBlackKingCheckmate() {
        // Stanje ploce mozemo vidjeti na https://grzegorz103.github.io/ngx-chess-board/chess-board/index.html
        // unosom u tekstualno polje FEN i klikom na Set FEN
        //ChessController chessController = new ChessController(null);
        String table;

        //Dva primjera kad nema saha
        table = "rnbq1bnr/ppp1kp1p/4p3/1B1p2p1/4P1P1/2N2Q2/PPPP1P1P/R1B1K1NR b KQ - 0 5";
        assertFalse(chessController.isBlackKingCheckmate(chessController.getBoardLook(table)));
        table = "rnb1kb1r/pp1p1ppp/4p3/2p5/2P2Pn1/8/PP1P1K1P/RNBQ1BNR w kq - 0 9";
        assertFalse(chessController.isBlackKingCheckmate(chessController.getBoardLook(table)));


        //Tri primjera kad je sah a nije mat
        table = "r1b2b1r/pp1pkppp/2n1p3/2p5/2P2PnQ/5K2/PP1P3P/RNB2BNR b - - 0 11";
        assertFalse(chessController.isBlackKingCheckmate(chessController.getBoardLook(table)));
        table = "rnbqkbnr/ppp1p2p/3p1p2/6p1/Q2P4/2P2P2/PP2P1PP/RNB1KBNR b KQkq - 0 4";
        assertFalse(chessController.isBlackKingCheckmate(chessController.getBoardLook(table)));

        //Dva primjera sah-mata
        table = "rn1qkbnr/pppbp2p/3p1p2/6pQ/2P1P3/7N/PP1P1PPP/RNB1KB1R b KQkq - 0 5";
        assertTrue(chessController.isBlackKingCheckmate(chessController.getBoardLook(table)));
        table = "rnbqkbnr/ppppp3/7p/5p1Q/3PP2p/8/PPP2PPP/RN2KBNR b KQkq - 0 5";
        assertTrue(chessController.isBlackKingCheckmate(chessController.getBoardLook(table)));
    }
}