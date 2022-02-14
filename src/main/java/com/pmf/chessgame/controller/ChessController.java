package com.pmf.chessgame.controller;

import com.pmf.chessgame.storage.model.entity.ChessBoardEntity;
import com.pmf.chessgame.storage.model.entity.GameEntity;
import com.pmf.chessgame.storage.model.request.MovePieceRequest;
import com.pmf.chessgame.storage.model.response.MovePieceResponse;
import com.pmf.chessgame.storage.repository.GameEntityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
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

    @MessageMapping("getCount")
    @SendTo("/topic")
    public Long getPlayersCount(@RequestBody String name) {
        Optional<GameEntity> game = gameEntityRepository.findByName(name);
        if (game.isPresent()) {
            return game.get().getPlayerCount();
        }
        return 0L;
    }

    @PostMapping("getAllMoves")
    public List<String> getAllMoves(@RequestBody String name) {
        Optional<GameEntity> game = gameEntityRepository.findByName(name);
        List<String> list = new ArrayList<>();
        if (game.isPresent()) {
            List<ChessBoardEntity> chessBoardEntityList = game.get().getBoards();
            chessBoardEntityList.sort((a, b) -> a.getId() < b.getId() ? -1 : 1);
            for (ChessBoardEntity chessBoardEntity : chessBoardEntityList) {
                list.add(chessBoardEntity.getBoard());
            }
            return list;
        }
        return list;
    }

    @PostMapping("getBoard")
    public String getBoard(@RequestBody String name) {
        Optional<GameEntity> game = gameEntityRepository.findByName(name);
        String move = "";
        if (game.isPresent()) {
            Long id = 0L;
            for (ChessBoardEntity chessBoardEntity : game.get().getBoards()) {
                if (chessBoardEntity.getId() > id) {
                    id = chessBoardEntity.getId();
                    move = chessBoardEntity.getMove();
                }
            }
        }
        return move;
    }

    @PostMapping("reset")
    public void reset(@RequestBody String name) {
        Optional<GameEntity> game = gameEntityRepository.findByName(name);
        if (game.isPresent()) {
            GameEntity gameEntity = game.get();
            gameEntityRepository.delete(gameEntity);
            GameEntity gameEntity1 = new GameEntity();
            gameEntity1.setPlayerCount(2L);
            gameEntity1.setFinished(false);
            gameEntity1.setTurn("w");
            gameEntity1.setName(game.get().getName());
            List<ChessBoardEntity> boards = new ArrayList<>();
            ChessBoardEntity chessBoardEntity = new ChessBoardEntity();
            chessBoardEntity.setBoard("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
            chessBoardEntity.setMove("");
            boards.add(chessBoardEntity);
            gameEntity1.setBoards(boards);
            gameEntityRepository.save(gameEntity1);
        }
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
            chessBoardEntity.setBoard("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
            chessBoardEntity.setMove("");
            boards.add(chessBoardEntity);
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
    public MovePieceResponse boardChanged(@RequestBody MovePieceRequest request) {
        Optional<GameEntity> game = gameEntityRepository.findByName(request.getName());
        if (game.isPresent()) {
            Boolean isCheckmate = false;
            String whoWon = "";
            GameEntity gameEntity = game.get();
            ChessBoardEntity chessBoardEntity = new ChessBoardEntity();

            //Test
            FieldType[][] temp = getBoardLook(request.getBoard());
            if (isWhiteKingCheck(temp)) {
                System.out.println("SAH na bijelom");
                if (isWhiteKingCheckmate(temp)) {
                    System.out.println("sah mat na bijelom");
                    isCheckmate = true;
                    whoWon = "b";
                    gameEntity.setFinished(true);
                }
            }
            if (isBlackKingCheck(temp)) {
                System.out.println("SAH na crnom");
                if (isBlackKingCheckmate(temp)) {
                    System.out.println("sah mat na crnom");
                    isCheckmate = true;
                    whoWon = "w";
                    gameEntity.setFinished(true);
                }
            }

            MovePieceResponse movePieceResponse = new MovePieceResponse(isCheckmate, whoWon);

            String boardInLastMove = "";
            Long id = 0L;
            for (ChessBoardEntity board : gameEntity.getBoards()) {
                if (board.getId() > id) {
                    id = board.getId();
                    boardInLastMove = board.getBoard();
                }
            }

            if (boardInLastMove.split(" ")[0].equals(request.getBoard().split(" ")[0])) {
                //Drugo spremanje iste tablice
                //Zanemari
                return movePieceResponse;
            }

            chessBoardEntity.setBoard(request.getBoard());
            chessBoardEntity.setMove(request.getMove());

            List<ChessBoardEntity> list = gameEntity.getBoards();
            list.add(chessBoardEntity);
            gameEntity.setBoards(list);
            gameEntityRepository.save(gameEntity);
            return movePieceResponse;
        }
        return null;
    }

    public enum FieldType {
        EMPTY, BPAWN, BROOK, BKNIGHT, BBISHOP, BQUEEN, BKING, WPAWN, WROOK, WKNIGHT, WBISHOP, WQUEEN, WKING;
    }

    private List<FieldType> blacks = new ArrayList<>() {{
        add(FieldType.BBISHOP);
        add(FieldType.BKING);
        add(FieldType.BKNIGHT);
        add(FieldType.BPAWN);
        add(FieldType.BQUEEN);
        add(FieldType.BROOK);
    }};
    private List<FieldType> whites = new ArrayList<>() {{
        add(FieldType.WBISHOP);
        add(FieldType.WKING);
        add(FieldType.WKNIGHT);
        add(FieldType.WPAWN);
        add(FieldType.WROOK);
        add(FieldType.WQUEEN);
    }};

    //Vraca matricu koja opisuje stanje ploce
    public FieldType[][] getBoardLook(String s) {
        FieldType[][] result = new FieldType[8][8];
        int i = 0;
        int j = 0;
        int l = 0;

        String board = s.split(" ")[0];
        while (l < board.length()) {
            switch (board.charAt(l)) {
                case '/':
                    i++;
                    j = 0;
                    break;
                case 'r':
                    result[i][j] = FieldType.BROOK;
                    j++;
                    break;
                case 'n':
                    result[i][j] = FieldType.BKNIGHT;
                    j++;
                    break;
                case 'b':
                    result[i][j] = FieldType.BBISHOP;
                    j++;
                    break;
                case 'q':
                    result[i][j] = FieldType.BQUEEN;
                    j++;
                    break;
                case 'k':
                    result[i][j] = FieldType.BKING;
                    j++;
                    break;
                case 'p':
                    result[i][j] = FieldType.BPAWN;
                    j++;
                    break;
                case 'R':
                    result[i][j] = FieldType.WROOK;
                    j++;
                    break;
                case 'N':
                    result[i][j] = FieldType.WKNIGHT;
                    j++;
                    break;
                case 'B':
                    result[i][j] = FieldType.WBISHOP;
                    j++;
                    break;
                case 'Q':
                    result[i][j] = FieldType.WQUEEN;
                    j++;
                    break;
                case 'K':
                    result[i][j] = FieldType.WKING;
                    j++;
                    break;
                case 'P':
                    result[i][j] = FieldType.WPAWN;
                    j++;
                    break;
                default:
                    //Mora biti broj
                    int num = Integer.parseInt(String.valueOf(board.charAt(l)));
                    for (int k = 0; k < num; k++) {
                        result[i][j] = FieldType.EMPTY;
                        j++;
                    }
            }
            l++;
        }
        return result;
    }

    //Provjera je li bijeli kralj u šahu
    public boolean isWhiteKingCheck(FieldType[][] table) {
        //Bijeli kralj se nalazi na (x,y)
        int x = 0, y = 0;
        boolean found = false;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (table[i][j] == FieldType.WKING) {
                    x = i;
                    y = j;
                    found = true;
                    break;
                }
            }
            if (found)
                break;
        }

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (table[i][j] == FieldType.EMPTY || table[i][j] == FieldType.WKING ||
                        table[i][j] == FieldType.WBISHOP || table[i][j] == FieldType.WKNIGHT ||
                        table[i][j] == FieldType.WQUEEN || table[i][j] == FieldType.WPAWN ||
                        table[i][j] == FieldType.WROOK) continue;
                switch (table[i][j]) {
                    case BPAWN:
                        if (x == i + 1 && (y == j + 1 || y == j - 1))
                            return true;
                        break;
                    case BROOK:
                        if (x == i) {
                            //U istom su redu
                            if (y < j) {
                                boolean check = false;
                                for (int k = y + 1; k < j; k++) {
                                    if (table[i][k] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            } else {
                                boolean check = false;
                                for (int k = j + 1; k < y; k++) {
                                    if (table[i][k] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            }
                        } else if (y == j) {
                            // U istom su stupcu
                            if (x < i) {
                                boolean check = false;
                                for (int k = x + 1; k < i; k++) {
                                    if (table[k][j] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            } else {
                                boolean check = false;
                                for (int k = i + 1; k < x; k++) {
                                    if (table[k][j] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            }
                        }
                        break;
                    case BKNIGHT:
                        if (i + 1 == x || i - 1 == x) {
                            if (y == j + 2 || y == j - 2)
                                return true;
                        } else if (i + 2 == x || i - 2 == x) {
                            if (y == j + 1 || y == j - 1)
                                return true;
                        }
                        break;
                    case BBISHOP:
                        if (x > i && y > j) {
                            //Kralj je dolje-desno
                            for (int d = 1; i + d < 8 && j + d < 8; d++) {
                                if (i + d == x && j + d == y)
                                    return true;
                                else if (table[i + d][j + d] != FieldType.EMPTY)
                                    break;
                            }
                        } else if (x > i && y < j) {
                            //dolje-lijevo
                            for (int d = 1; i + d < 8 && j - d >= 0; d++) {
                                if (i + d == x && j - d == y)
                                    return true;
                                else if (table[i + d][j - d] != FieldType.EMPTY)
                                    break;
                            }
                        } else if (x < i && y > j) {
                            //gore-desno
                            for (int d = 1; i - d >= 0 && j + d < 8; d++) {
                                if (i - d == x && j + d == y)
                                    return true;
                                else if (table[i - d][j + d] != FieldType.EMPTY)
                                    break;
                            }
                        } else if (x < i && y < j) {
                            //gore-lijevo
                            for (int d = 1; i - d >= 0 && j - d >= 0; d++) {
                                if (i - d == x && j - d == y)
                                    return true;
                                else if (table[i - d][j - d] != FieldType.EMPTY)
                                    break;
                            }
                        }
                        break;
                    case BQUEEN:
                        //Kraljica se ponasa kao kombinacija topa i lovca
                        //Prvo kopija koda topa
                        if (x == i) {
                            //U istom su redu
                            if (y < j) {
                                boolean check = false;
                                for (int k = y + 1; k < j; k++) {
                                    if (table[i][k] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            } else {
                                boolean check = false;
                                for (int k = j + 1; k < y; k++) {
                                    if (table[i][k] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            }
                        } else if (y == j) {
                            // U istom su stupcu
                            if (x < i) {
                                boolean check = false;
                                for (int k = x + 1; k < i; k++) {
                                    if (table[k][j] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            } else {
                                boolean check = false;
                                for (int k = i + 1; k < x; k++) {
                                    if (table[k][j] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            }
                        }
                        //Kopija koda lovca
                        else if (x > i && y > j) {
                            //Kralj je dolje-desno
                            for (int d = 1; i + d < 8 && j + d < 8; d++) {
                                if (i + d == x && j + d == y)
                                    return true;
                                else if (table[i + d][j + d] != FieldType.EMPTY)
                                    break;
                            }
                        } else if (x > i && y < j) {
                            //dolje-lijevo
                            for (int d = 1; i + d < 8 && j - d >= 0; d++) {
                                if (i + d == x && j - d == y)
                                    return true;
                                else if (table[i + d][j - d] != FieldType.EMPTY)
                                    break;
                            }
                        } else if (x < i && y > j) {
                            //gore-desno
                            for (int d = 1; i - d >= 0 && j + d < 8; d++) {
                                if (i - d == x && j + d == y)
                                    return true;
                                else if (table[i - d][j + d] != FieldType.EMPTY)
                                    break;
                            }
                        } else if (x < i && y < j) {
                            //gore-lijevo
                            for (int d = 1; i - d >= 0 && j - d >= 0; d++) {
                                if (i - d == x && j - d == y)
                                    return true;
                                else if (table[i - d][j - d] != FieldType.EMPTY)
                                    break;
                            }
                        }
                        break;
                    case BKING:
                        //nemoguce da kralj napravi sah kralju
                        break;

                }

            }
        }
        return false;
    }

    //Provjera je li crni kralj u šahu
    public boolean isBlackKingCheck(FieldType[][] table) {
        //Crni kralj se nalazi na (x,y)
        int x = 0, y = 0;
        boolean found = false;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (table[i][j] == FieldType.BKING) {
                    x = i;
                    y = j;
                    found = true;
                    break;
                }
            }
            if (found)
                break;
        }

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (table[i][j] == FieldType.EMPTY || table[i][j] == FieldType.BKING ||
                        table[i][j] == FieldType.BBISHOP || table[i][j] == FieldType.BKNIGHT ||
                        table[i][j] == FieldType.BQUEEN || table[i][j] == FieldType.BPAWN ||
                        table[i][j] == FieldType.BROOK) continue;
                switch (table[i][j]) {
                    case WPAWN:
                        if (x == i - 1 && (y == j + 1 || y == j - 1))
                            return true;
                        break;
                    case WROOK:
                        if (x == i) {
                            //U istom su redu
                            if (y < j) {
                                boolean check = false;
                                for (int k = y + 1; k < j; k++) {
                                    if (table[i][k] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            } else {
                                boolean check = false;
                                for (int k = j + 1; k < y; k++) {
                                    if (table[i][k] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            }
                        } else if (y == j) {
                            // U istom su stupcu
                            if (x < i) {
                                boolean check = false;
                                for (int k = x + 1; k < i; k++) {
                                    if (table[k][j] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            } else {
                                boolean check = false;
                                for (int k = i + 1; k < x; k++) {
                                    if (table[k][j] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            }
                        }
                        break;
                    case WKNIGHT:
                        if (i + 1 == x || i - 1 == x) {
                            if (y == j + 2 || y == j - 2)
                                return true;
                        } else if (i + 2 == x || i - 2 == x) {
                            if (y == j + 1 || y == j - 1)
                                return true;
                        }
                        break;
                    case WBISHOP:
                        if (x > i && y > j) {
                            //Kralj je dolje-desno
                            for (int d = 1; i + d < 8 && j + d < 8; d++) {
                                if (i + d == x && j + d == y)
                                    return true;
                                else if (table[i + d][j + d] != FieldType.EMPTY)
                                    break;
                            }
                        } else if (x > i && y < j) {
                            //dolje-lijevo
                            for (int d = 1; i + d < 8 && j - d >= 0; d++) {
                                if (i + d == x && j - d == y)
                                    return true;
                                else if (table[i + d][j - d] != FieldType.EMPTY)
                                    break;
                            }
                        } else if (x < i && y > j) {
                            //gore-desno
                            for (int d = 1; i - d >= 0 && j + d < 8; d++) {
                                if (i - d == x && j + d == y)
                                    return true;
                                else if (table[i - d][j + d] != FieldType.EMPTY)
                                    break;
                            }
                        } else if (x < i && y < j) {
                            //gore-lijevo
                            for (int d = 1; i - d >= 0 && j - d >= 0; d++) {
                                if (i - d == x && j - d == y)
                                    return true;
                                else if (table[i - d][j - d] != FieldType.EMPTY)
                                    break;
                            }
                        }
                        break;
                    case WQUEEN:
                        //Kraljica se ponasa kao kombinacija topa i lovca
                        //Prvo kopija koda topa
                        if (x == i) {
                            //U istom su redu
                            if (y < j) {
                                boolean check = false;
                                for (int k = y + 1; k < j; k++) {
                                    if (table[i][k] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            } else {
                                boolean check = false;
                                for (int k = j + 1; k < y; k++) {
                                    if (table[i][k] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            }
                        } else if (y == j) {
                            // U istom su stupcu
                            if (x < i) {
                                boolean check = false;
                                for (int k = x + 1; k < i; k++) {
                                    if (table[k][j] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            } else {
                                boolean check = false;
                                for (int k = i + 1; k < x; k++) {
                                    if (table[k][j] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            }
                        }
                        //Kopija koda lovca
                        else if (x > i && y > j) {
                            //Kralj je dolje-desno
                            for (int d = 1; i + d < 8 && j + d < 8; d++) {
                                if (i + d == x && j + d == y)
                                    return true;
                                else if (table[i + d][j + d] != FieldType.EMPTY)
                                    break;
                            }
                        } else if (x > i && y < j) {
                            //dolje-lijevo
                            for (int d = 1; i + d < 8 && j - d >= 0; d++) {
                                if (i + d == x && j - d == y)
                                    return true;
                                else if (table[i + d][j - d] != FieldType.EMPTY)
                                    break;
                            }
                        } else if (x < i && y > j) {
                            //gore-desno
                            for (int d = 1; i - d >= 0 && j + d < 8; d++) {
                                if (i - d == x && j + d == y)
                                    return true;
                                else if (table[i - d][j + d] != FieldType.EMPTY)
                                    break;
                            }
                        } else if (x < i && y < j) {
                            //gore-lijevo
                            for (int d = 1; i - d >= 0 && j - d >= 0; d++) {
                                if (i - d == x && j - d == y)
                                    return true;
                                else if (table[i - d][j - d] != FieldType.EMPTY)
                                    break;
                            }
                        }
                        break;
                    case WKING:
                        //nemoguce da kralj napravi sah kralju
                        break;

                }

            }
        }
        return false;
    }

    //Vraca listu svih mogucih pozicija na koju se figura na (i,j) moze pomaknut
    // Testira se kroz funkcije za sah i mat
    private List<Pair<Integer, Integer>> getMoves(int i, int j, FieldType[][] table) {
        List<Pair<Integer, Integer>> moves = new ArrayList<>();
        switch (table[i][j]) {
            case EMPTY:
                break;
            case WPAWN:
                if (i == 0)
                    break;
                if (table[i - 1][j] == FieldType.EMPTY)
                    moves.add(Pair.of(i - 1, j));
                if (j - 1 > 0 && blacks.contains(table[i - 1][j - 1]))
                    moves.add(Pair.of(i - 1, j - 1));
                if (j < 7 && blacks.contains(table[i - 1][j + 1]))
                    moves.add(Pair.of(i - 1, j + 1));
                break;
            case BPAWN:
                if (i == 7)
                    break;
                if (table[i + 1][j] == FieldType.EMPTY)
                    moves.add(Pair.of(i + 1, j));
                if (j - 1 > 0 && whites.contains(table[i + 1][j - 1]))
                    moves.add(Pair.of(i + 1, j - 1));
                if (j < 7 && whites.contains(table[i + 1][j + 1]))
                    moves.add(Pair.of(i + 1, j + 1));
                break;
            case WBISHOP:
                int k = i + 1;
                int l = j + 1;
                while (k < 8 && l < 8) {
                    if (table[k][l] == FieldType.EMPTY)
                        moves.add(Pair.of(k, l));
                    else if (blacks.contains(table[k][l])) {
                        moves.add(Pair.of(k, l));
                        break;
                    } else
                        break;
                    k++;
                    l++;
                }
                k = i + 1;
                l = j - 1;
                while (k < 8 && l >= 0) {
                    if (table[k][l] == FieldType.EMPTY)
                        moves.add(Pair.of(k, l));
                    else if (blacks.contains(table[k][l])) {
                        moves.add(Pair.of(k, l));
                        break;
                    } else
                        break;
                    k++;
                    l--;
                }
                k = i - 1;
                l = j + 1;
                while (k >= 0 && l < 8) {
                    if (table[k][l] == FieldType.EMPTY)
                        moves.add(Pair.of(k, l));
                    else if (blacks.contains(table[k][l])) {
                        moves.add(Pair.of(k, l));
                        break;
                    } else
                        break;
                    k--;
                    l++;
                }
                k = i - 1;
                l = j - 1;
                while (k >= 0 && l >= 0) {
                    if (table[k][l] == FieldType.EMPTY)
                        moves.add(Pair.of(k, l));
                    else if (blacks.contains(table[k][l])) {
                        moves.add(Pair.of(k, l));
                        break;
                    } else
                        break;
                    k--;
                    l--;
                }
                break;
            case BBISHOP:
                k = i + 1;
                l = j + 1;
                while (k < 8 && l < 8) {
                    if (table[k][j] == FieldType.EMPTY)
                        moves.add(Pair.of(k, l));
                    else if (whites.contains(table[k][l])) {
                        moves.add(Pair.of(k, l));
                        break;
                    } else
                        break;
                    k++;
                    l++;
                }
                k = i + 1;
                l = j - 1;
                while (k < 8 && l >= 0) {
                    if (table[k][j] == FieldType.EMPTY)
                        moves.add(Pair.of(k, l));
                    else if (whites.contains(table[k][l])) {
                        moves.add(Pair.of(k, l));
                        break;
                    } else
                        break;
                    k++;
                    l--;
                }
                k = i - 1;
                l = j + 1;
                while (k >= 0 && l < 8) {
                    if (table[k][l] == FieldType.EMPTY)
                        moves.add(Pair.of(k, l));
                    else if (whites.contains(table[k][l])) {
                        moves.add(Pair.of(k, l));
                        break;
                    } else
                        break;
                    k--;
                    l++;
                }
                k = i - 1;
                l = j - 1;
                while (k >= 0 && l >= 0) {
                    if (table[k][l] == FieldType.EMPTY)
                        moves.add(Pair.of(k, l));
                    else if (whites.contains(table[k][l])) {
                        moves.add(Pair.of(k, l));
                        break;
                    } else
                        break;
                    k--;
                    l--;
                }
                break;
            case WKING:
                for (k = i - 1; k <= i + 1; k++) {
                    for (l = j - 1; l <= j + 1; l++) {
                        if (k == i && l == j)
                            continue;
                        else if (k > 7 || k < 0 || l > 7 || l < 0)
                            continue;
                        else if (table[k][l] == FieldType.EMPTY || blacks.contains(table[k][l]))
                            moves.add(Pair.of(k, l));
                    }
                }
                break;
            case BKING:
                for (k = i - 1; k <= i + 1; k++) {
                    for (l = j - 1; l <= j + 1; l++) {
                        if (k == i && l == j)
                            continue;
                        else if (k > 7 || k < 0 || l > 7 || l < 0)
                            continue;
                        else if (table[k][l] == FieldType.EMPTY || whites.contains(table[k][l]))
                            moves.add(Pair.of(k, l));
                    }
                }
                break;
            case WKNIGHT:
                k = i + 1;
                l = j - 2;
                if (!(k > 7 || k < 0 || l > 7 || l < 0) && (table[k][l] == FieldType.EMPTY || blacks.contains(table[k][l])))
                    moves.add(Pair.of(k, l));
                k = i + 1;
                l = j + 2;
                if (!(k > 7 || k < 0 || l > 7 || l < 0) && (table[k][l] == FieldType.EMPTY || blacks.contains(table[k][l])))
                    moves.add(Pair.of(k, l));
                k = i + 2;
                l = j + 1;
                if (!(k > 7 || k < 0 || l > 7 || l < 0) && (table[k][l] == FieldType.EMPTY || blacks.contains(table[k][l])))
                    moves.add(Pair.of(k, l));
                k = i + 2;
                l = j - 1;
                if (!(k > 7 || k < 0 || l > 7 || l < 0) && (table[k][l] == FieldType.EMPTY || blacks.contains(table[k][l])))
                    moves.add(Pair.of(k, l));
                k = i - 1;
                l = j - 2;
                if (!(k > 7 || k < 0 || l > 7 || l < 0) && (table[k][l] == FieldType.EMPTY || blacks.contains(table[k][l])))
                    moves.add(Pair.of(k, l));
                k = i - 1;
                l = j + 2;
                if (!(k > 7 || k < 0 || l > 7 || l < 0) && (table[k][l] == FieldType.EMPTY || blacks.contains(table[k][l])))
                    moves.add(Pair.of(k, l));
                k = i - 2;
                l = j + 1;
                if (!(k > 7 || k < 0 || l > 7 || l < 0) && (table[k][l] == FieldType.EMPTY || blacks.contains(table[k][l])))
                    moves.add(Pair.of(k, l));
                k = i - 2;
                l = j - 1;
                if (!(k > 7 || k < 0 || l > 7 || l < 0) && (table[k][l] == FieldType.EMPTY || blacks.contains(table[k][l])))
                    moves.add(Pair.of(k, l));
                break;
            case BKNIGHT:
                k = i + 1;
                l = j - 2;
                if (!(k > 7 || k < 0 || l > 7 || l < 0) && (table[k][l] == FieldType.EMPTY || whites.contains(table[k][l])))
                    moves.add(Pair.of(k, l));
                k = i + 1;
                l = j + 2;
                if (!(k > 7 || k < 0 || l > 7 || l < 0) && (table[k][l] == FieldType.EMPTY || whites.contains(table[k][l])))
                    moves.add(Pair.of(k, l));
                k = i + 2;
                l = j + 1;
                if (!(k > 7 || k < 0 || l > 7 || l < 0) && (table[k][l] == FieldType.EMPTY || whites.contains(table[k][l])))
                    moves.add(Pair.of(k, l));
                k = i + 2;
                l = j - 1;
                if (!(k > 7 || k < 0 || l > 7 || l < 0) && (table[k][l] == FieldType.EMPTY || whites.contains(table[k][l])))
                    moves.add(Pair.of(k, l));
                k = i - 1;
                l = j - 2;
                if (!(k > 7 || k < 0 || l > 7 || l < 0) && (table[k][l] == FieldType.EMPTY || whites.contains(table[k][l])))
                    moves.add(Pair.of(k, l));
                k = i - 1;
                l = j + 2;
                if (!(k > 7 || k < 0 || l > 7 || l < 0) && (table[k][l] == FieldType.EMPTY || whites.contains(table[k][l])))
                    moves.add(Pair.of(k, l));
                k = i - 2;
                l = j + 1;
                if (!(k > 7 || k < 0 || l > 7 || l < 0) && (table[k][l] == FieldType.EMPTY || whites.contains(table[k][l])))
                    moves.add(Pair.of(k, l));
                k = i - 2;
                l = j - 1;
                if (!(k > 7 || k < 0 || l > 7 || l < 0) && (table[k][l] == FieldType.EMPTY || whites.contains(table[k][l])))
                    moves.add(Pair.of(k, l));
                break;
            case WROOK:
                k = i + 1;
                l = j;
                while (k < 8) {
                    if (table[k][l] == FieldType.EMPTY)
                        moves.add(Pair.of(k, l));
                    else if (blacks.contains(table[k][l])) {
                        moves.add(Pair.of(k, l));
                        break;
                    } else
                        break;
                    k++;
                }
                k = i - 1;
                while (k >= 0) {
                    if (table[k][l] == FieldType.EMPTY)
                        moves.add(Pair.of(k, l));
                    else if (blacks.contains(table[k][l])) {
                        moves.add(Pair.of(k, l));
                        break;
                    } else
                        break;
                    k--;
                }
                k = i;
                l = j + 1;
                while (l < 8) {
                    if (table[k][l] == FieldType.EMPTY)
                        moves.add(Pair.of(k, l));
                    else if (blacks.contains(table[k][l])) {
                        moves.add(Pair.of(k, l));
                        break;
                    } else
                        break;
                    l++;
                }
                l = j - 1;
                while (l >= 0) {
                    if (table[k][l] == FieldType.EMPTY)
                        moves.add(Pair.of(k, l));
                    else if (blacks.contains(table[k][l])) {
                        moves.add(Pair.of(k, l));
                        break;
                    } else
                        break;
                    l--;
                }
                break;
            case BROOK:
                k = i + 1;
                l = j;
                while (k < 8) {
                    if (table[k][l] == FieldType.EMPTY)
                        moves.add(Pair.of(k, l));
                    else if (whites.contains(table[k][l])) {
                        moves.add(Pair.of(k, l));
                        break;
                    } else
                        break;
                    k++;
                }
                k = i - 1;
                while (k >= 0) {
                    if (table[k][l] == FieldType.EMPTY)
                        moves.add(Pair.of(k, l));
                    else if (whites.contains(table[k][l])) {
                        moves.add(Pair.of(k, l));
                        break;
                    } else
                        break;
                    k--;
                }
                k = i;
                l = j + 1;
                while (l < 8) {
                    if (table[k][l] == FieldType.EMPTY)
                        moves.add(Pair.of(k, l));
                    else if (whites.contains(table[k][l])) {
                        moves.add(Pair.of(k, l));
                        break;
                    } else
                        break;
                    l++;
                }
                l = j - 1;
                while (l >= 0) {
                    if (table[k][l] == FieldType.EMPTY)
                        moves.add(Pair.of(k, l));
                    else if (whites.contains(table[k][l])) {
                        moves.add(Pair.of(k, l));
                        break;
                    } else
                        break;
                    l--;
                }
                break;
            case WQUEEN:
                //Ponasa se kao kombinacija kule i lovca
                table[i][j] = FieldType.WROOK;
                moves.addAll(getMoves(i, j, table));
                table[i][j] = FieldType.WBISHOP;
                moves.addAll(getMoves(i, j, table));
                table[i][j] = FieldType.WQUEEN;
                break;
            case BQUEEN:
                table[i][j] = FieldType.BROOK;
                moves.addAll(getMoves(i, j, table));
                table[i][j] = FieldType.BBISHOP;
                moves.addAll(getMoves(i, j, table));
                table[i][j] = FieldType.BQUEEN;
                break;
        }
        return moves;
    }

    //Provjerava je li se dogodio sah-mat nad bijelim kraljem
    // Pretpostavlja da je kralj u sahu
    public boolean isWhiteKingCheckmate(FieldType[][] table) {
        //Bijeli kralj se nalazi na (x,y)
        int x = 0, y = 0;
        boolean found = false;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (table[i][j] == FieldType.WKING) {
                    x = i;
                    y = j;
                    found = true;
                    break;
                }
            }
            if (found)
                break;
        }

        // pogledajmo sve moguce kraljeve poteze
        List<Pair<Integer, Integer>> moves = getMoves(x, y, table);

        //Prvo provjerimo moze li se kralj pomaknut
        for (Pair<Integer, Integer> p : moves) {
            var temp = table[p.getFirst()][p.getSecond()];
            table[p.getFirst()][p.getSecond()] = FieldType.WKING;
            table[x][y] = FieldType.EMPTY;
            //Provjeri je li sad sah
            if (!isWhiteKingCheck(table))
                return false;
            else {
                //Vrati kako je bilo
                table[p.getFirst()][p.getSecond()] = temp;
                table[x][y] = FieldType.WKING;
            }

        }

        //Moramo provjeriti moze li se neka figura pomaknuti tako da vise nije sah
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                // Trazimo sve bijele figure
                if (!(table[i][j] == FieldType.EMPTY || blacks.contains(table[i][j])
                        || table[i][j] == FieldType.WKING)) {
                    moves.clear();
                    moves = getMoves(i, j, table);
                    for (Pair<Integer, Integer> p : moves) {
                        var temp = table[p.getFirst()][p.getSecond()];
                        table[p.getFirst()][p.getSecond()] = table[i][j];
                        table[i][j] = FieldType.EMPTY;
                        //Provjeri je li sad sah
                        if (!isWhiteKingCheck(table)) {
                            return false;
                        } else {
                            //Vrati kako je bilo
                            table[i][j] = table[p.getFirst()][p.getSecond()];
                            table[p.getFirst()][p.getSecond()] = temp;
                        }

                    }
                }

            }
        }
        return true;
    }

    //Provjerava je li se dogodio sah-mat nad crnim kraljem
    // Pretpostavlja da je kralj u sahu
    public boolean isBlackKingCheckmate(FieldType[][] table) {
        //Bijeli kralj se nalazi na (x,y)
        int x = 0, y = 0;
        boolean found = false;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (table[i][j] == FieldType.BKING) {
                    x = i;
                    y = j;
                    found = true;
                    break;
                }
            }
            if (found)
                break;
        }

        // pogledajmo sve moguce kraljeve poteze
        List<Pair<Integer, Integer>> moves = getMoves(x, y, table);

        //Prvo provjerimo moze li se kralj pomaknut
        for (Pair<Integer, Integer> p : moves) {
            var temp = table[p.getFirst()][p.getSecond()];
            table[p.getFirst()][p.getSecond()] = FieldType.BKING;
            table[x][y] = FieldType.EMPTY;
            //Provjeri je li sad sah
            if (!isBlackKingCheck(table))
                return false;
            else {
                //Vrati kako je bilo
                table[p.getFirst()][p.getSecond()] = temp;
                table[x][y] = FieldType.BKING;
            }

        }

        //Moramo provjeriti moze li se neka figura pomaknuti tako da vise nije sah
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                // Trazimo sve bijele figure
                if (!(table[i][j] == FieldType.EMPTY || whites.contains(table[i][j])
                        || table[i][j] == FieldType.BKING)) {
                    moves.clear();
                    moves = getMoves(i, j, table);
                    for (Pair<Integer, Integer> p : moves) {
                        var temp = table[p.getFirst()][p.getSecond()];
                        table[p.getFirst()][p.getSecond()] = table[i][j];
                        table[i][j] = FieldType.EMPTY;
                        //Provjeri je li sad sah
                        if (!isBlackKingCheck(table)) {
                            return false;
                        } else {
                            //Vrati kako je bilo
                            table[i][j] = table[p.getFirst()][p.getSecond()];
                            table[p.getFirst()][p.getSecond()] = temp;
                        }

                    }
                }

            }
        }
        return true;
    }

}
