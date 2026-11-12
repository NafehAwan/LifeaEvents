package com.lifea.chess.table;

/** A chair at a table: index 0 is White's chair, 1 is Black's. */
public record SeatRef(ChessTable table, int index) {
    public boolean isWhite() {
        return index == ChessTable.WHITE_SEAT;
    }
}
