package com.pmf.chessgame.storage.model.request;

import com.fasterxml.jackson.databind.util.JSONPObject;
import lombok.Data;


@Data
public class MovePieceRequest {
    private String name;
    private String board;
    private String move;
}
