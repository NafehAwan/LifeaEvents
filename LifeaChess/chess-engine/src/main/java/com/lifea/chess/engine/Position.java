package com.lifea.chess.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * A mutable chess position with full make/unmake support.
 *
 * <p>Board indexing: {@code sq = rank * 8 + file}, a1 = 0, h8 = 63. The position
 * tracks side to move, castling rights, the en passant target square, the
 * half-move clock and the full-move number. All rule logic (attacks, legal move
 * generation, castling, en passant, promotion) lives here; it has no Minecraft
 * dependencies.
 */
public final class Position {
    public static final int CASTLE_WK = 1;
    public static final int CASTLE_WQ = 2;
    public static final int CASTLE_BK = 4;
    public static final int CASTLE_BQ = 8;

    private static final int[] KNIGHT_DF = {1, 2, 2, 1, -1, -2, -2, -1};
    private static final int[] KNIGHT_DR = {2, 1, -1, -2, -2, -1, 1, 2};
    private static final int[] KING_DF = {1, 1, 1, 0, 0, -1, -1, -1};
    private static final int[] KING_DR = {1, 0, -1, 1, -1, 1, 0, -1};
    private static final int[] ROOK_DF = {1, -1, 0, 0};
    private static final int[] ROOK_DR = {0, 0, 1, -1};
    private static final int[] BISHOP_DF = {1, 1, -1, -1};
    private static final int[] BISHOP_DR = {1, -1, 1, -1};

    private final Piece[] board = new Piece[64];
    private Color sideToMove = Color.WHITE;
    private int castling;
    private int epSquare = Square.NONE;
    private int halfmoveClock;
    private int fullmoveNumber = 1;

    private Undo[] undoStack = new Undo[64];
    private int undoDepth;

    private record Undo(Move move, Piece moved, Piece captured, int capturedSquare,
                        int castling, int epSquare, int halfmoveClock) {
    }

    private Position() {
    }

    /** The standard starting position. */
    public static Position initial() {
        return Fen.parse(Fen.START);
    }

    /** Builds a position from raw components (used by the FEN parser). */
    static Position of(Piece[] squares, Color side, int castling, int ep, int halfmove, int fullmove) {
        Position p = new Position();
        System.arraycopy(squares, 0, p.board, 0, 64);
        p.sideToMove = side;
        p.castling = castling;
        p.epSquare = ep;
        p.halfmoveClock = halfmove;
        p.fullmoveNumber = fullmove;
        p.sanitizeCastling();
        return p;
    }

    /** Deep copy without undo history. */
    public Position copy() {
        return of(board, sideToMove, castling, epSquare, halfmoveClock, fullmoveNumber);
    }

    // ------------------------------------------------------------------ accessors

    public Piece pieceAt(int sq) {
        return board[sq];
    }

    public Color sideToMove() {
        return sideToMove;
    }

    public int castlingRights() {
        return castling;
    }

    public boolean canCastle(int flag) {
        return (castling & flag) != 0;
    }

    public int epSquare() {
        return epSquare;
    }

    public int halfmoveClock() {
        return halfmoveClock;
    }

    public int fullmoveNumber() {
        return fullmoveNumber;
    }

    /** Number of plies currently on the undo stack. */
    public int ply() {
        return undoDepth;
    }

    public int kingSquare(Color color) {
        Piece king = Piece.of(color, PieceType.KING);
        for (int sq = 0; sq < 64; sq++) {
            if (board[sq] == king) {
                return sq;
            }
        }
        return Square.NONE;
    }

    public boolean isInCheck(Color color) {
        int k = kingSquare(color);
        return k != Square.NONE && isSquareAttacked(k, color.opposite());
    }

    public boolean inCheck() {
        return isInCheck(sideToMove);
    }

    /** Sum of material values (pawn = 1 ... queen = 9) for one side. */
    public int material(Color color) {
        int total = 0;
        for (Piece p : board) {
            if (p != null && p.color() == color) {
                total += p.type().value();
            }
        }
        return total;
    }

    // ------------------------------------------------------------------ attacks

    /** True when any piece of {@code by} attacks {@code sq}. */
    public boolean isSquareAttacked(int sq, Color by) {
        int f = Square.file(sq);
        int r = Square.rank(sq);

        // Pawns: a pawn attacks diagonally forward, so look one rank "behind" the square.
        int pr = r - by.pawnDirection();
        if (pr >= 0 && pr < 8) {
            Piece pawn = Piece.of(by, PieceType.PAWN);
            if (f > 0 && board[Square.of(f - 1, pr)] == pawn) {
                return true;
            }
            if (f < 7 && board[Square.of(f + 1, pr)] == pawn) {
                return true;
            }
        }

        Piece knight = Piece.of(by, PieceType.KNIGHT);
        for (int i = 0; i < 8; i++) {
            int tf = f + KNIGHT_DF[i];
            int tr = r + KNIGHT_DR[i];
            if (Square.valid(tf, tr) && board[Square.of(tf, tr)] == knight) {
                return true;
            }
        }

        Piece king = Piece.of(by, PieceType.KING);
        for (int i = 0; i < 8; i++) {
            int tf = f + KING_DF[i];
            int tr = r + KING_DR[i];
            if (Square.valid(tf, tr) && board[Square.of(tf, tr)] == king) {
                return true;
            }
        }

        Piece rook = Piece.of(by, PieceType.ROOK);
        Piece bishop = Piece.of(by, PieceType.BISHOP);
        Piece queen = Piece.of(by, PieceType.QUEEN);
        if (rayHits(f, r, ROOK_DF, ROOK_DR, rook, queen)) {
            return true;
        }
        return rayHits(f, r, BISHOP_DF, BISHOP_DR, bishop, queen);
    }

    private boolean rayHits(int f, int r, int[] dfs, int[] drs, Piece a, Piece b) {
        for (int d = 0; d < 4; d++) {
            int tf = f + dfs[d];
            int tr = r + drs[d];
            while (Square.valid(tf, tr)) {
                Piece p = board[Square.of(tf, tr)];
                if (p != null) {
                    if (p == a || p == b) {
                        return true;
                    }
                    break;
                }
                tf += dfs[d];
                tr += drs[d];
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ move generation

    /** All legal moves for the side to move. */
    public List<Move> legalMoves() {
        List<Move> pseudo = pseudoLegalMoves();
        List<Move> legal = new ArrayList<>(pseudo.size());
        Color us = sideToMove;
        for (Move m : pseudo) {
            make(m);
            if (!isInCheck(us)) {
                legal.add(m);
            }
            unmake();
        }
        return legal;
    }

    public boolean hasLegalMoves() {
        List<Move> pseudo = pseudoLegalMoves();
        Color us = sideToMove;
        for (Move m : pseudo) {
            make(m);
            boolean ok = !isInCheck(us);
            unmake();
            if (ok) {
                return true;
            }
        }
        return false;
    }

    public boolean isLegal(Move m) {
        return legalMoves().contains(m);
    }

    /** Moves that obey piece movement but may leave the own king in check. */
    public List<Move> pseudoLegalMoves() {
        List<Move> moves = new ArrayList<>(48);
        Color us = sideToMove;
        for (int sq = 0; sq < 64; sq++) {
            Piece p = board[sq];
            if (p == null || p.color() != us) {
                continue;
            }
            switch (p.type()) {
                case PAWN -> genPawn(sq, us, moves);
                case KNIGHT -> genSteps(sq, us, KNIGHT_DF, KNIGHT_DR, moves);
                case KING -> genSteps(sq, us, KING_DF, KING_DR, moves);
                case ROOK -> genRays(sq, us, ROOK_DF, ROOK_DR, moves);
                case BISHOP -> genRays(sq, us, BISHOP_DF, BISHOP_DR, moves);
                case QUEEN -> {
                    genRays(sq, us, ROOK_DF, ROOK_DR, moves);
                    genRays(sq, us, BISHOP_DF, BISHOP_DR, moves);
                }
            }
        }
        genCastling(us, moves);
        return moves;
    }

    private void genPawn(int sq, Color us, List<Move> moves) {
        int f = Square.file(sq);
        int r = Square.rank(sq);
        int dir = us.pawnDirection();
        int r1 = r + dir;
        if (r1 < 0 || r1 > 7) {
            return;
        }
        int oneUp = Square.of(f, r1);
        if (board[oneUp] == null) {
            addPawnMove(sq, oneUp, us, moves);
            if (r == us.pawnStartRank()) {
                int twoUp = Square.of(f, r + 2 * dir);
                if (board[twoUp] == null) {
                    moves.add(new Move(sq, twoUp));
                }
            }
        }
        for (int df = -1; df <= 1; df += 2) {
            int tf = f + df;
            if (tf < 0 || tf > 7) {
                continue;
            }
            int to = Square.of(tf, r1);
            Piece target = board[to];
            if (target != null && target.color() != us) {
                addPawnMove(sq, to, us, moves);
            } else if (target == null && to == epSquare) {
                moves.add(new Move(sq, to));
            }
        }
    }

    private void addPawnMove(int from, int to, Color us, List<Move> moves) {
        if (Square.rank(to) == us.promotionRank()) {
            moves.add(new Move(from, to, PieceType.QUEEN));
            moves.add(new Move(from, to, PieceType.ROOK));
            moves.add(new Move(from, to, PieceType.BISHOP));
            moves.add(new Move(from, to, PieceType.KNIGHT));
        } else {
            moves.add(new Move(from, to));
        }
    }

    private void genSteps(int sq, Color us, int[] dfs, int[] drs, List<Move> moves) {
        int f = Square.file(sq);
        int r = Square.rank(sq);
        for (int i = 0; i < dfs.length; i++) {
            int tf = f + dfs[i];
            int tr = r + drs[i];
            if (!Square.valid(tf, tr)) {
                continue;
            }
            int to = Square.of(tf, tr);
            Piece target = board[to];
            if (target == null || target.color() != us) {
                moves.add(new Move(sq, to));
            }
        }
    }

    private void genRays(int sq, Color us, int[] dfs, int[] drs, List<Move> moves) {
        int f = Square.file(sq);
        int r = Square.rank(sq);
        for (int d = 0; d < 4; d++) {
            int tf = f + dfs[d];
            int tr = r + drs[d];
            while (Square.valid(tf, tr)) {
                int to = Square.of(tf, tr);
                Piece target = board[to];
                if (target == null) {
                    moves.add(new Move(sq, to));
                } else {
                    if (target.color() != us) {
                        moves.add(new Move(sq, to));
                    }
                    break;
                }
                tf += dfs[d];
                tr += drs[d];
            }
        }
    }

    private void genCastling(Color us, List<Move> moves) {
        int back = us.backRank();
        int e = Square.of(4, back);
        if (board[e] != Piece.of(us, PieceType.KING)) {
            return;
        }
        Color them = us.opposite();
        int kFlag = us == Color.WHITE ? CASTLE_WK : CASTLE_BK;
        int qFlag = us == Color.WHITE ? CASTLE_WQ : CASTLE_BQ;
        Piece rook = Piece.of(us, PieceType.ROOK);

        if ((castling & kFlag) != 0
                && board[Square.of(7, back)] == rook
                && board[Square.of(5, back)] == null
                && board[Square.of(6, back)] == null
                && !isSquareAttacked(e, them)
                && !isSquareAttacked(Square.of(5, back), them)
                && !isSquareAttacked(Square.of(6, back), them)) {
            moves.add(new Move(e, Square.of(6, back)));
        }
        if ((castling & qFlag) != 0
                && board[Square.of(0, back)] == rook
                && board[Square.of(1, back)] == null
                && board[Square.of(2, back)] == null
                && board[Square.of(3, back)] == null
                && !isSquareAttacked(e, them)
                && !isSquareAttacked(Square.of(3, back), them)
                && !isSquareAttacked(Square.of(2, back), them)) {
            moves.add(new Move(e, Square.of(2, back)));
        }
    }

    // ------------------------------------------------------------------ move semantics

    public boolean isCastling(Move m) {
        Piece p = board[m.from()];
        return p != null && p.type() == PieceType.KING
                && Math.abs(Square.file(m.to()) - Square.file(m.from())) == 2;
    }

    public boolean isEnPassant(Move m) {
        Piece p = board[m.from()];
        return p != null && p.type() == PieceType.PAWN
                && m.to() == epSquare && board[m.to()] == null
                && Square.file(m.from()) != Square.file(m.to());
    }

    public boolean isCapture(Move m) {
        return board[m.to()] != null || isEnPassant(m);
    }

    /** The piece a move would capture (including en passant), or null. */
    public Piece capturedBy(Move m) {
        if (isEnPassant(m)) {
            return Piece.of(sideToMove.opposite(), PieceType.PAWN);
        }
        return board[m.to()];
    }

    // ------------------------------------------------------------------ make / unmake

    /** Plays a move. The caller is responsible for legality. */
    public void make(Move m) {
        int from = m.from();
        int to = m.to();
        Piece moved = board[from];
        if (moved == null) {
            throw new IllegalStateException("No piece on " + Square.name(from));
        }
        Color us = moved.color();
        Piece captured = board[to];
        int capSq = to;
        boolean isPawn = moved.type() == PieceType.PAWN;

        if (isPawn && to == epSquare && captured == null && Square.file(from) != Square.file(to)) {
            capSq = to - 8 * us.pawnDirection();
            captured = board[capSq];
            board[capSq] = null;
        }

        pushUndo(new Undo(m, moved, captured, capSq, castling, epSquare, halfmoveClock));

        board[from] = null;
        board[to] = m.isPromotion() ? Piece.of(us, m.promotion()) : moved;

        if (moved.type() == PieceType.KING && Math.abs(Square.file(to) - Square.file(from)) == 2) {
            if (to > from) {
                board[to - 1] = board[to + 1];
                board[to + 1] = null;
            } else {
                board[to + 1] = board[to - 2];
                board[to - 2] = null;
            }
        }

        if (moved.type() == PieceType.KING) {
            castling &= us == Color.WHITE ? ~(CASTLE_WK | CASTLE_WQ) : ~(CASTLE_BK | CASTLE_BQ);
        } else if (moved.type() == PieceType.ROOK) {
            castling &= ~rookFlag(from);
        }
        if (captured != null && captured.type() == PieceType.ROOK) {
            castling &= ~rookFlag(capSq);
        }

        epSquare = (isPawn && Math.abs(Square.rank(to) - Square.rank(from)) == 2)
                ? from + 8 * us.pawnDirection()
                : Square.NONE;
        halfmoveClock = (isPawn || captured != null) ? 0 : halfmoveClock + 1;
        if (us == Color.BLACK) {
            fullmoveNumber++;
        }
        sideToMove = us.opposite();
    }

    private static int rookFlag(int sq) {
        return switch (sq) {
            case 0 -> CASTLE_WQ;
            case 7 -> CASTLE_WK;
            case 56 -> CASTLE_BQ;
            case 63 -> CASTLE_BK;
            default -> 0;
        };
    }

    /** Reverts the last {@link #make(Move)}. */
    public void unmake() {
        if (undoDepth == 0) {
            throw new IllegalStateException("Nothing to unmake");
        }
        Undo u = undoStack[--undoDepth];
        undoStack[undoDepth] = null;
        Move m = u.move();
        Color us = u.moved().color();

        board[m.from()] = u.moved();
        board[m.to()] = null;
        if (u.captured() != null) {
            board[u.capturedSquare()] = u.captured();
        }
        if (u.moved().type() == PieceType.KING && Math.abs(Square.file(m.to()) - Square.file(m.from())) == 2) {
            if (m.to() > m.from()) {
                board[m.to() + 1] = board[m.to() - 1];
                board[m.to() - 1] = null;
            } else {
                board[m.to() - 2] = board[m.to() + 1];
                board[m.to() + 1] = null;
            }
        }
        castling = u.castling();
        epSquare = u.epSquare();
        halfmoveClock = u.halfmoveClock();
        if (us == Color.BLACK) {
            fullmoveNumber--;
        }
        sideToMove = us;
    }

    private void pushUndo(Undo u) {
        if (undoDepth == undoStack.length) {
            Undo[] bigger = new Undo[undoStack.length * 2];
            System.arraycopy(undoStack, 0, bigger, 0, undoStack.length);
            undoStack = bigger;
        }
        undoStack[undoDepth++] = u;
    }

    /** Drops castling rights whose king/rook are not on their origin squares. */
    private void sanitizeCastling() {
        if (board[4] != Piece.WHITE_KING) {
            castling &= ~(CASTLE_WK | CASTLE_WQ);
        }
        if (board[7] != Piece.WHITE_ROOK) {
            castling &= ~CASTLE_WK;
        }
        if (board[0] != Piece.WHITE_ROOK) {
            castling &= ~CASTLE_WQ;
        }
        if (board[60] != Piece.BLACK_KING) {
            castling &= ~(CASTLE_BK | CASTLE_BQ);
        }
        if (board[63] != Piece.BLACK_ROOK) {
            castling &= ~CASTLE_BK;
        }
        if (board[56] != Piece.BLACK_ROOK) {
            castling &= ~CASTLE_BQ;
        }
    }

    // ------------------------------------------------------------------ keys & material rules

    /**
     * Zobrist key of the position. The en passant file is folded in only when
     * {@code includeEp} is true; callers implementing the repetition rule pass
     * {@code true} only when a legal en passant capture actually exists.
     */
    public long zobristKey(boolean includeEp) {
        long key = 0L;
        for (int sq = 0; sq < 64; sq++) {
            Piece p = board[sq];
            if (p != null) {
                key ^= Zobrist.PIECE[p.index()][sq];
            }
        }
        if (sideToMove == Color.BLACK) {
            key ^= Zobrist.SIDE;
        }
        key ^= Zobrist.CASTLING[castling];
        if (includeEp && epSquare != Square.NONE) {
            key ^= Zobrist.EP_FILE[Square.file(epSquare)];
        }
        return key;
    }

    /** Key used for the three/fivefold repetition rule (FIDE 9.2). */
    public long repetitionKey() {
        boolean epCapturePossible = false;
        if (epSquare != Square.NONE) {
            for (Move m : legalMoves()) {
                if (isEnPassant(m)) {
                    epCapturePossible = true;
                    break;
                }
            }
        }
        return zobristKey(epCapturePossible);
    }

    /**
     * True when no sequence of legal moves can lead to checkmate for either side:
     * K v K, K+B v K, K+N v K, and K+B(s) v K+B(s) with all bishops on one colour.
     */
    public boolean isDeadPosition() {
        int minors = 0;
        boolean lightBishop = false;
        boolean darkBishop = false;
        boolean knight = false;
        for (int sq = 0; sq < 64; sq++) {
            Piece p = board[sq];
            if (p == null || p.type() == PieceType.KING) {
                continue;
            }
            switch (p.type()) {
                case PAWN, ROOK, QUEEN -> {
                    return false;
                }
                case BISHOP -> {
                    minors++;
                    if (Square.isLight(sq)) {
                        lightBishop = true;
                    } else {
                        darkBishop = true;
                    }
                }
                case KNIGHT -> {
                    minors++;
                    knight = true;
                }
                default -> {
                }
            }
        }
        if (minors == 0) {
            return true;
        }
        if (minors == 1) {
            return true;
        }
        return !knight && !(lightBishop && darkBishop);
    }

    /**
     * Whether {@code color} could still deliver checkmate by any legal sequence —
     * used for the timeout rule (a lone king or king + one minor cannot).
     */
    public boolean hasMatingMaterial(Color color) {
        int minors = 0;
        for (Piece p : board) {
            if (p == null || p.color() != color) {
                continue;
            }
            switch (p.type()) {
                case PAWN, ROOK, QUEEN -> {
                    return true;
                }
                case BISHOP, KNIGHT -> minors++;
                default -> {
                }
            }
        }
        return minors >= 2;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int r = 7; r >= 0; r--) {
            sb.append(r + 1).append(' ');
            for (int f = 0; f < 8; f++) {
                Piece p = board[Square.of(f, r)];
                sb.append(p == null ? '.' : p.fenChar()).append(' ');
            }
            sb.append('\n');
        }
        sb.append("  a b c d e f g h\n");
        return sb.toString();
    }
}
