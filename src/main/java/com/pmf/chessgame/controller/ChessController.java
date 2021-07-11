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

            FieldType[][] temp = getBoardLook(request.getBoard());
            System.out.println("Bijeli kralj u sahu: " + isWhiteKingCheck(temp));
            System.out.println("Crni kralj u sahu: " + isBlackKingCheck(temp));

            chessBoardEntity.setBoard(request.getBoard());
            List<ChessBoardEntity> list = gameEntity.getBoards();
            list.add(chessBoardEntity);
            gameEntity.setBoards(list);
            gameEntityRepository.save(gameEntity);
        }
    }
    public enum FieldType{
        EMPTY, BPAWN, BROOK, BKNIGHT, BBISHOP, BQUEEN, BKING, WPAWN, WROOK, WKNIGHT, WBISHOP, WQUEEN, WKING;
    }

    //Vraca matricu koja opisuje stanje ploce
    private FieldType[][] getBoardLook(String s){
        FieldType[][] result = new FieldType[8][8];
        int i = 0;
        int j = 0;
        int l = 0;

        String board = s.split(" ")[0];
        while (l < board.length()){
            switch (board.charAt(l)){
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
                    for (int k = 0; k < num; k++){
                        result[i][j] = FieldType.EMPTY;
                        j++;
                    }
            }
            l++;
        }
        return result;
    }

    //Provjera je li bijeli kralj u šahu
    private boolean isWhiteKingCheck(FieldType[][] table){
        //Bijeli kralj se nalazi na (x,y)
        int x = 0, y = 0;
        boolean found = false;
        for (int i = 0; i < 8; i++){
            for (int j = 0; j < 8; j++){
                if (table[i][j] == FieldType.WKING){
                    x = i;
                    y = j;
                    found = true;
                    break;
                }
            }
            if (found)
                break;
        }

        for (int i = 0; i < 8; i++){
            for (int j = 0; j < 8; j++){
                if (table[i][j] == FieldType.EMPTY || table[i][j] == FieldType.WKING ||
                        table[i][j] == FieldType.WBISHOP || table[i][j] == FieldType.WKNIGHT ||
                        table[i][j] == FieldType.WQUEEN || table[i][j] == FieldType.WPAWN ||
                        table[i][j] == FieldType.WROOK) continue;
                switch (table[i][j]){
                    case BPAWN:
                        if (x == i + 1 && (y == j+1 || y == j-1))
                            return true;
                        break;
                    case BROOK:
                        if (x == i){
                            //U istom su redu
                            if (y < j){
                                boolean check = false;
                                for (int k = y+1; k < j; k++) {
                                    if (table[i][k] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            }
                            else{
                                boolean check = false;
                                for (int k = j+1; k < y; k++) {
                                    if (table[i][k] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            }
                        }
                        else if (y == j){
                            // U istom su stupcu
                            if (x < i){
                                boolean check = false;
                                for (int k = x+1; k < i; k++) {
                                    if (table[k][j] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            }
                            else{
                                boolean check = false;
                                for (int k = i+1; k < x; k++) {
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
                        if (i+1 == x || i-1 == x ){
                            if (y == j+2 || y == j-2)
                                return true;
                        }
                        else if (i+2 == x || i-2 == x){
                            if (y == j+1 || y == j-1)
                                return  true;
                        }
                        break;
                    case BBISHOP:
                        if (x > i && y > j){
                            //Kralj je dolje-desno
                            for(int d = 1; i+d < 8 && j+d < 8; d++){
                                if (i+d == x && j+d == y)
                                    return true;
                                else if (table[i+d][j+d] != FieldType.EMPTY)
                                    break;
                            }
                        }
                        else if (x > i && y < j){
                            //dolje-lijevo
                            for(int d = 1; i+d < 8 && j-d >= 0; d++){
                                if (i+d == x && j-d == y)
                                    return true;
                                else if (table[i+d][j-d] != FieldType.EMPTY)
                                    break;
                            }
                        }
                        else if (x < i && y > j){
                            //gore-desno
                            for(int d = 1; i-d >= 0 && j+d < 8; d++){
                                if (i-d == x && j+d == y)
                                    return true;
                                else if (table[i-d][j+d] != FieldType.EMPTY)
                                    break;
                            }
                        }
                        else if (x < i && y < j){
                            //gore-lijevo
                            for(int d = 1; i-d >= 0 && j-d >= 0; d++){
                                if (i-d == x && j-d == y)
                                    return true;
                                else if (table[i-d][j-d] != FieldType.EMPTY)
                                    break;
                            }
                        }
                        break;
                    case BQUEEN:
                        //Kraljica se ponasa kao kombinacija topa i lovca
                        //Prvo kopija koda topa
                        if (x == i){
                            //U istom su redu
                            if (y < j){
                                boolean check = false;
                                for (int k = y+1; k < j; k++) {
                                    if (table[i][k] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            }
                            else{
                                boolean check = false;
                                for (int k = j+1; k < y; k++) {
                                    if (table[i][k] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            }
                        }
                        else if (y == j){
                            // U istom su stupcu
                            if (x < i){
                                boolean check = false;
                                for (int k = x+1; k < i; k++) {
                                    if (table[k][j] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            }
                            else{
                                boolean check = false;
                                for (int k = i+1; k < x; k++) {
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
                        else if (x > i && y > j){
                            //Kralj je dolje-desno
                            for(int d = 1; i+d < 8 && j+d < 8; d++){
                                if (i+d == x && j+d == y)
                                    return true;
                                else if (table[i+d][j+d] != FieldType.EMPTY)
                                    break;
                            }
                        }
                        else if (x > i && y < j){
                            //dolje-lijevo
                            for(int d = 1; i+d < 8 && j-d >= 0; d++){
                                if (i+d == x && j-d == y)
                                    return true;
                                else if (table[i+d][j-d] != FieldType.EMPTY)
                                    break;
                            }
                        }
                        else if (x < i && y > j){
                            //gore-desno
                            for(int d = 1; i-d >= 0 && j+d < 8; d++){
                                if (i-d == x && j+d == y)
                                    return true;
                                else if (table[i-d][j+d] != FieldType.EMPTY)
                                    break;
                            }
                        }
                        else if (x < i && y < j){
                            //gore-lijevo
                            for(int d = 1; i-d >= 0 && j-d >= 0; d++){
                                if (i-d == x && j-d == y)
                                    return true;
                                else if (table[i-d][j-d] != FieldType.EMPTY)
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
    private boolean isBlackKingCheck(FieldType[][] table){
        //Crni kralj se nalazi na (x,y)
        int x = 0, y = 0;
        boolean found = false;
        for (int i = 0; i < 8; i++){
            for (int j = 0; j < 8; j++){
                if (table[i][j] == FieldType.BKING){
                    x = i;
                    y = j;
                    found = true;
                    break;
                }
            }
            if (found)
                break;
        }

        for (int i = 0; i < 8; i++){
            for (int j = 0; j < 8; j++){
                if (table[i][j] == FieldType.EMPTY || table[i][j] == FieldType.BKING ||
                        table[i][j] == FieldType.BBISHOP || table[i][j] == FieldType.BKNIGHT ||
                        table[i][j] == FieldType.BQUEEN || table[i][j] == FieldType.BPAWN ||
                        table[i][j] == FieldType.BROOK) continue;
                switch (table[i][j]){
                    case WPAWN:
                        if (x == i - 1 && (y == j+1 || y == j-1))
                            return true;
                        break;
                    case WROOK:
                        if (x == i){
                            //U istom su redu
                            if (y < j){
                                boolean check = false;
                                for (int k = y+1; k < j; k++) {
                                    if (table[i][k] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            }
                            else{
                                boolean check = false;
                                for (int k = j+1; k < y; k++) {
                                    if (table[i][k] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            }
                        }
                        else if (y == j){
                            // U istom su stupcu
                            if (x < i){
                                boolean check = false;
                                for (int k = x+1; k < i; k++) {
                                    if (table[k][j] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            }
                            else{
                                boolean check = false;
                                for (int k = i+1; k < x; k++) {
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
                        if (i+1 == x || i-1 == x ){
                            if (y == j+2 || y == j-2)
                                return true;
                        }
                        else if (i+2 == x || i-2 == x){
                            if (y == j+1 || y == j-1)
                                return  true;
                        }
                        break;
                    case WBISHOP:
                        if (x > i && y > j){
                            //Kralj je dolje-desno
                            for(int d = 1; i+d < 8 && j+d < 8; d++){
                                if (i+d == x && j+d == y)
                                    return true;
                                else if (table[i+d][j+d] != FieldType.EMPTY)
                                    break;
                            }
                        }
                        else if (x > i && y < j){
                            //dolje-lijevo
                            for(int d = 1; i+d < 8 && j-d >= 0; d++){
                                if (i+d == x && j-d == y)
                                    return true;
                                else if (table[i+d][j-d] != FieldType.EMPTY)
                                    break;
                            }
                        }
                        else if (x < i && y > j){
                            //gore-desno
                            for(int d = 1; i-d >= 0 && j+d < 8; d++){
                                if (i-d == x && j+d == y)
                                    return true;
                                else if (table[i-d][j+d] != FieldType.EMPTY)
                                    break;
                            }
                        }
                        else if (x < i && y < j){
                            //gore-lijevo
                            for(int d = 1; i-d >= 0 && j-d >= 0; d++){
                                if (i-d == x && j-d == y)
                                    return true;
                                else if (table[i-d][j-d] != FieldType.EMPTY)
                                    break;
                            }
                        }
                        break;
                    case WQUEEN:
                        //Kraljica se ponasa kao kombinacija topa i lovca
                        //Prvo kopija koda topa
                        if (x == i){
                            //U istom su redu
                            if (y < j){
                                boolean check = false;
                                for (int k = y+1; k < j; k++) {
                                    if (table[i][k] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            }
                            else{
                                boolean check = false;
                                for (int k = j+1; k < y; k++) {
                                    if (table[i][k] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            }
                        }
                        else if (y == j){
                            // U istom su stupcu
                            if (x < i){
                                boolean check = false;
                                for (int k = x+1; k < i; k++) {
                                    if (table[k][j] != FieldType.EMPTY) {
                                        check = true;
                                        break;
                                    }
                                }
                                if (!check)
                                    //Znaci da nema nikoga izmedu njih
                                    return true;
                            }
                            else{
                                boolean check = false;
                                for (int k = i+1; k < x; k++) {
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
                        else if (x > i && y > j){
                            //Kralj je dolje-desno
                            for(int d = 1; i+d < 8 && j+d < 8; d++){
                                if (i+d == x && j+d == y)
                                    return true;
                                else if (table[i+d][j+d] != FieldType.EMPTY)
                                    break;
                            }
                        }
                        else if (x > i && y < j){
                            //dolje-lijevo
                            for(int d = 1; i+d < 8 && j-d >= 0; d++){
                                if (i+d == x && j-d == y)
                                    return true;
                                else if (table[i+d][j-d] != FieldType.EMPTY)
                                    break;
                            }
                        }
                        else if (x < i && y > j){
                            //gore-desno
                            for(int d = 1; i-d >= 0 && j+d < 8; d++){
                                if (i-d == x && j+d == y)
                                    return true;
                                else if (table[i-d][j+d] != FieldType.EMPTY)
                                    break;
                            }
                        }
                        else if (x < i && y < j){
                            //gore-lijevo
                            for(int d = 1; i-d >= 0 && j-d >= 0; d++){
                                if (i-d == x && j-d == y)
                                    return true;
                                else if (table[i-d][j-d] != FieldType.EMPTY)
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
}
