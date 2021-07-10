package com.pmf.chessgame.storage.model.request;

import lombok.Data;

@Data
public class MovePieceRequest {
    private String name;
    private String board;
}
