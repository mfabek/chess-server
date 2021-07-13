package com.pmf.chessgame.storage.model.response;

import lombok.Data;

@Data
public class MovePieceResponse {
    private Boolean isCheckmate;
    private String whoWon;

    public MovePieceResponse(Boolean isCheckmate, String whoWon) {
        this.isCheckmate = isCheckmate;
        this.whoWon = whoWon;
    }
}
