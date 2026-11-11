package com.lifea.chess.engine;

import java.util.List;

/** Standard Algebraic Notation generation and parsing. */
public final class San {
    private San() {
    }

    /**
     * SAN for {@code move} played in {@code pos}, including disambiguation,
     * capture marker, promotion suffix and check/mate suffix. The position is
     * left unchanged.
     */
    public static String format(Position pos, Move move) {
        return format(pos, move, pos.legalMoves());
    }

    static String format(Position pos, Move move, List<Move> legal) {
        Piece piece = pos.pieceAt(move.from());
        if (piece == null) {
            throw new IllegalArgumentException("No piece on " + Square.name(move.from()));
        }
        StringBuilder sb = new StringBuilder(8);
        boolean capture = pos.isCapture(move);

        if (pos.isCastling(move)) {
            sb.append(move.to() > move.from() ? "O-O" : "O-O-O");
        } else if (piece.type() == PieceType.PAWN) {
            if (capture) {
                sb.append(Square.fileChar(move.from())).append('x');
            }
            sb.append(Square.name(move.to()));
            if (move.isPromotion()) {
                sb.append('=').append(move.promotion().letter());
            }
        } else {
            sb.append(piece.type().letter());
            sb.append(disambiguation(pos, move, piece, legal));
            if (capture) {
                sb.append('x');
            }
            sb.append(Square.name(move.to()));
        }

        pos.make(move);
        if (pos.inCheck()) {
            sb.append(pos.hasLegalMoves() ? '+' : '#');
        }
        pos.unmake();
        return sb.toString();
    }

    private static String disambiguation(Position pos, Move move, Piece piece, List<Move> legal) {
        boolean sameFile = false;
        boolean sameRank = false;
        boolean any = false;
        for (Move other : legal) {
            if (other.from() == move.from() || other.to() != move.to()) {
                continue;
            }
            if (pos.pieceAt(other.from()) != piece) {
                continue;
            }
            any = true;
            if (Square.file(other.from()) == Square.file(move.from())) {
                sameFile = true;
            }
            if (Square.rank(other.from()) == Square.rank(move.from())) {
                sameRank = true;
            }
        }
        if (!any) {
            return "";
        }
        if (!sameFile) {
            return String.valueOf(Square.fileChar(move.from()));
        }
        if (!sameRank) {
            return String.valueOf(Square.rankChar(move.from()));
        }
        return Square.name(move.from());
    }

    /**
     * Resolves a SAN string to the matching legal move in {@code pos}. Accepts
     * optional check/mate/annotation suffixes and "0-0" style castling.
     */
    public static Move parse(Position pos, String san) {
        if (san == null) {
            throw new IllegalArgumentException("Null SAN");
        }
        String wanted = normalize(san);
        List<Move> legal = pos.legalMoves();
        for (Move m : legal) {
            if (normalize(format(pos, m, legal)).equals(wanted)) {
                return m;
            }
        }
        throw new IllegalArgumentException("Illegal or unrecognised SAN: " + san);
    }

    private static String normalize(String s) {
        String t = s.trim().replace("0-0-0", "O-O-O").replace("0-0", "O-O");
        int end = t.length();
        while (end > 0) {
            char c = t.charAt(end - 1);
            if (c == '+' || c == '#' || c == '!' || c == '?') {
                end--;
            } else {
                break;
            }
        }
        t = t.substring(0, end);
        if (t.endsWith("e.p.")) {
            t = t.substring(0, t.length() - 4);
        }
        return t;
    }
}
