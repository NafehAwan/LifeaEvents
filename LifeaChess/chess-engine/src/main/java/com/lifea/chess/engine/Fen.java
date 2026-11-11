package com.lifea.chess.engine;

/** Forsyth–Edwards Notation parsing and generation. */
public final class Fen {
    public static final String START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private Fen() {
    }

    public static Position parse(String fen) {
        if (fen == null || fen.isBlank()) {
            throw new IllegalArgumentException("Empty FEN");
        }
        String[] parts = fen.trim().split("\\s+");
        if (parts.length < 2) {
            throw new IllegalArgumentException("FEN needs at least placement and side: " + fen);
        }

        Piece[] board = new Piece[64];
        String[] ranks = parts[0].split("/");
        if (ranks.length != 8) {
            throw new IllegalArgumentException("FEN must have 8 ranks: " + fen);
        }
        for (int i = 0; i < 8; i++) {
            int rank = 7 - i;
            int file = 0;
            for (char c : ranks[i].toCharArray()) {
                if (Character.isDigit(c)) {
                    file += c - '0';
                } else {
                    if (file > 7) {
                        throw new IllegalArgumentException("Rank overflow in FEN: " + fen);
                    }
                    board[Square.of(file, rank)] = Piece.fromFenChar(c);
                    file++;
                }
            }
            if (file != 8) {
                throw new IllegalArgumentException("Rank " + (rank + 1) + " does not have 8 files: " + fen);
            }
        }

        Color side = switch (parts[1]) {
            case "w" -> Color.WHITE;
            case "b" -> Color.BLACK;
            default -> throw new IllegalArgumentException("Bad side to move: " + parts[1]);
        };

        int castling = 0;
        if (parts.length > 2 && !parts[2].equals("-")) {
            for (char c : parts[2].toCharArray()) {
                switch (c) {
                    case 'K' -> castling |= Position.CASTLE_WK;
                    case 'Q' -> castling |= Position.CASTLE_WQ;
                    case 'k' -> castling |= Position.CASTLE_BK;
                    case 'q' -> castling |= Position.CASTLE_BQ;
                    default -> throw new IllegalArgumentException("Bad castling field: " + parts[2]);
                }
            }
        }

        int ep = Square.NONE;
        if (parts.length > 3 && !parts[3].equals("-")) {
            ep = Square.parse(parts[3]);
        }
        int halfmove = parts.length > 4 ? Integer.parseInt(parts[4]) : 0;
        int fullmove = parts.length > 5 ? Integer.parseInt(parts[5]) : 1;

        int whiteKings = 0;
        int blackKings = 0;
        for (Piece p : board) {
            if (p == Piece.WHITE_KING) {
                whiteKings++;
            } else if (p == Piece.BLACK_KING) {
                blackKings++;
            }
        }
        if (whiteKings != 1 || blackKings != 1) {
            throw new IllegalArgumentException("Each side needs exactly one king: " + fen);
        }
        return Position.of(board, side, castling, ep, halfmove, fullmove);
    }

    public static String format(Position p) {
        StringBuilder sb = new StringBuilder(80);
        for (int rank = 7; rank >= 0; rank--) {
            int empty = 0;
            for (int file = 0; file < 8; file++) {
                Piece piece = p.pieceAt(Square.of(file, rank));
                if (piece == null) {
                    empty++;
                } else {
                    if (empty > 0) {
                        sb.append(empty);
                        empty = 0;
                    }
                    sb.append(piece.fenChar());
                }
            }
            if (empty > 0) {
                sb.append(empty);
            }
            if (rank > 0) {
                sb.append('/');
            }
        }
        sb.append(' ').append(p.sideToMove() == Color.WHITE ? 'w' : 'b').append(' ');
        int c = p.castlingRights();
        if (c == 0) {
            sb.append('-');
        } else {
            if ((c & Position.CASTLE_WK) != 0) sb.append('K');
            if ((c & Position.CASTLE_WQ) != 0) sb.append('Q');
            if ((c & Position.CASTLE_BK) != 0) sb.append('k');
            if ((c & Position.CASTLE_BQ) != 0) sb.append('q');
        }
        sb.append(' ').append(Square.name(p.epSquare()));
        sb.append(' ').append(p.halfmoveClock()).append(' ').append(p.fullmoveNumber());
        return sb.toString();
    }
}
