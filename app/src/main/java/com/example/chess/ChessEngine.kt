package com.example.chess

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class PieceColor { White, Black }
enum class PieceType { Pawn, Knight, Bishop, Rook, Queen, King }

data class Piece(val type: PieceType, val color: PieceColor)

data class Position(val row: Int, val col: Int) {
    fun isValid() = row in 0..7 && col in 0..7
}

data class Move(
    val from: Position,
    val to: Position,
    val pieceMoved: Piece,
    val pieceCaptured: Piece? = null,
    val isEnPassant: Boolean = false,
    val isCastling: Boolean = false,
    val promotion: PieceType? = null
)

class Board(val grid: Array<Array<Piece?>> = Array(8) { Array<Piece?>(8) { null } }) {
    fun copy(): Board {
        val newGrid = Array(8) { r -> Array(8) { c -> grid[r][c] } }
        return Board(newGrid)
    }

    operator fun get(pos: Position) = grid[pos.row][pos.col]
    operator fun set(pos: Position, piece: Piece?) { grid[pos.row][pos.col] = piece }
}

class ChessEngine {
    var board = Board()
    var currentTurn = PieceColor.White
    var moveHistory = mutableListOf<Move>()
    
    // Castling rights: 0 = W_K, 1 = W_Q, 2 = B_K, 3 = B_Q
    var castlingRights = booleanArrayOf(true, true, true, true)
    var castlingHistory = mutableListOf<BooleanArray>()

    var enPassantTarget: Position? = null
    var enPassantHistory = mutableListOf<Position?>()

    init {
        setupStandardBoard()
    }

    private fun setupStandardBoard() {
        board = Board()
        currentTurn = PieceColor.White
        moveHistory.clear()
        castlingRights = booleanArrayOf(true, true, true, true)
        castlingHistory.clear()
        enPassantTarget = null
        enPassantHistory.clear()

        // Pawns
        for (i in 0..7) {
            board[Position(1, i)] = Piece(PieceType.Pawn, PieceColor.Black)
            board[Position(6, i)] = Piece(PieceType.Pawn, PieceColor.White)
        }
        // Pieces
        val backRank = arrayOf(PieceType.Rook, PieceType.Knight, PieceType.Bishop, PieceType.Queen, PieceType.King, PieceType.Bishop, PieceType.Knight, PieceType.Rook)
        for (i in 0..7) {
            board[Position(0, i)] = Piece(backRank[i], PieceColor.Black)
            board[Position(7, i)] = Piece(backRank[i], PieceColor.White)
        }
    }

    fun restart() {
        setupStandardBoard()
    }

    fun makeMove(move: Move) {
        castlingHistory.add(castlingRights.copyOf())
        enPassantHistory.add(enPassantTarget)

        board[move.to] = move.promotion?.let { Piece(it, move.pieceMoved.color) } ?: move.pieceMoved
        board[move.from] = null

        if (move.isEnPassant) {
            val captureRow = if (move.pieceMoved.color == PieceColor.White) move.to.row + 1 else move.to.row - 1
            board[Position(captureRow, move.to.col)] = null
        }

        if (move.isCastling) {
            if (move.to.col == 6) { // Kingside
                board[Position(move.from.row, 5)] = board[Position(move.from.row, 7)]
                board[Position(move.from.row, 7)] = null
            } else if (move.to.col == 2) { // Queenside
                board[Position(move.from.row, 3)] = board[Position(move.from.row, 0)]
                board[Position(move.from.row, 0)] = null
            }
        }

        // Update castling rights
        if (move.pieceMoved.type == PieceType.King) {
            if (move.pieceMoved.color == PieceColor.White) {
                castlingRights[0] = false
                castlingRights[1] = false
            } else {
                castlingRights[2] = false
                castlingRights[3] = false
            }
        }
        if (move.pieceMoved.type == PieceType.Rook) {
            if (move.from == Position(7, 7)) castlingRights[0] = false
            if (move.from == Position(7, 0)) castlingRights[1] = false
            if (move.from == Position(0, 7)) castlingRights[2] = false
            if (move.from == Position(0, 0)) castlingRights[3] = false
        }

        // Update En Passant
        enPassantTarget = if (move.pieceMoved.type == PieceType.Pawn && abs(move.from.row - move.to.row) == 2) {
            Position((move.from.row + move.to.row) / 2, move.from.col)
        } else {
            null
        }

        moveHistory.add(move)
        currentTurn = if (currentTurn == PieceColor.White) PieceColor.Black else PieceColor.White
    }

    fun undoMove() {
        if (moveHistory.isEmpty()) return
        val move = moveHistory.removeLast()
        
        currentTurn = if (currentTurn == PieceColor.White) PieceColor.Black else PieceColor.White
        board[move.from] = move.pieceMoved
        board[move.to] = move.pieceCaptured
        
        if (move.isEnPassant) {
            val captureRow = if (move.pieceMoved.color == PieceColor.White) move.to.row + 1 else move.to.row - 1
            board[Position(captureRow, move.to.col)] = Piece(PieceType.Pawn, currentTurn.opposite())
        }

        if (move.isCastling) {
            if (move.to.col == 6) {
                board[Position(move.from.row, 7)] = board[Position(move.from.row, 5)]
                board[Position(move.from.row, 5)] = null
            } else if (move.to.col == 2) {
                board[Position(move.from.row, 0)] = board[Position(move.from.row, 3)]
                board[Position(move.from.row, 3)] = null
            }
        }

        castlingRights = castlingHistory.removeLast()
        enPassantTarget = enPassantHistory.removeLast()
    }

    private fun PieceColor.opposite() = if (this == PieceColor.White) PieceColor.Black else PieceColor.White

    fun getLegalMoves(color: PieceColor = currentTurn): List<Move> {
        val pseudoLegal = getAllPseudoLegalMoves(color)
        return pseudoLegal.filter { !leavesKingInCheck(it, color) }
    }

    private fun getAllPseudoLegalMoves(color: PieceColor): List<Move> {
        val moves = mutableListOf<Move>()
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[Position(r, c)]
                if (piece != null && piece.color == color) {
                    moves.addAll(getPseudoLegalMovesForPiece(Position(r, c), piece))
                }
            }
        }
        return moves
    }

    fun getPseudoLegalMovesForPiece(pos: Position, piece: Piece = board[pos]!!): List<Move> {
        val moves = mutableListOf<Move>()
        when (piece.type) {
            PieceType.Pawn -> generatePawnMoves(pos, piece, moves)
            PieceType.Knight -> generateKnightMoves(pos, piece, moves)
            PieceType.Bishop -> generateSlidingMoves(pos, piece, moves, arrayOf(Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)))
            PieceType.Rook -> generateSlidingMoves(pos, piece, moves, arrayOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1)))
            PieceType.Queen -> generateSlidingMoves(pos, piece, moves, arrayOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1), Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)))
            PieceType.King -> generateKingMoves(pos, piece, moves)
        }
        return moves
    }

    private fun generatePawnMoves(pos: Position, piece: Piece, moves: MutableList<Move>) {
        val dir = if (piece.color == PieceColor.White) -1 else 1
        val startRow = if (piece.color == PieceColor.White) 6 else 1

        // Forward
        var next = Position(pos.row + dir, pos.col)
        if (next.isValid() && board[next] == null) {
            addPawnMove(pos, next, piece, null, false, moves)
            // Double forward
            if (pos.row == startRow) {
                next = Position(pos.row + dir * 2, pos.col)
                if (board[next] == null) {
                    moves.add(Move(pos, next, piece))
                }
            }
        }

        // Capture
        for (c in arrayOf(-1, 1)) {
            next = Position(pos.row + dir, pos.col + c)
            if (next.isValid()) {
                val target = board[next]
                if (target != null && target.color != piece.color) {
                    addPawnMove(pos, next, piece, target, false, moves)
                } else if (next == enPassantTarget) {
                    addPawnMove(pos, next, piece, Piece(PieceType.Pawn, piece.color.opposite()), true, moves)
                }
            }
        }
    }

    private fun addPawnMove(from: Position, to: Position, piece: Piece, captured: Piece?, isEnPassant: Boolean, moves: MutableList<Move>) {
        val promoRow = if (piece.color == PieceColor.White) 0 else 7
        if (to.row == promoRow) {
            moves.add(Move(from, to, piece, captured, isEnPassant, promotion = PieceType.Queen))
            moves.add(Move(from, to, piece, captured, isEnPassant, promotion = PieceType.Rook))
            moves.add(Move(from, to, piece, captured, isEnPassant, promotion = PieceType.Bishop))
            moves.add(Move(from, to, piece, captured, isEnPassant, promotion = PieceType.Knight))
        } else {
            moves.add(Move(from, to, piece, captured, isEnPassant))
        }
    }

    private fun generateKnightMoves(pos: Position, piece: Piece, moves: MutableList<Move>) {
        val jumps = arrayOf(Pair(2, 1), Pair(2, -1), Pair(-2, 1), Pair(-2, -1), Pair(1, 2), Pair(1, -2), Pair(-1, 2), Pair(-1, -2))
        for (j in jumps) {
            val next = Position(pos.row + j.first, pos.col + j.second)
            if (next.isValid()) {
                val target = board[next]
                if (target == null || target.color != piece.color) {
                    moves.add(Move(pos, next, piece, target))
                }
            }
        }
    }

    private fun generateSlidingMoves(pos: Position, piece: Piece, moves: MutableList<Move>, directions: Array<Pair<Int, Int>>) {
        for (d in directions) {
            var curr = Position(pos.row + d.first, pos.col + d.second)
            while (curr.isValid()) {
                val target = board[curr]
                if (target == null) {
                    moves.add(Move(pos, curr, piece, null))
                } else {
                    if (target.color != piece.color) {
                        moves.add(Move(pos, curr, piece, target))
                    }
                    break
                }
                curr = Position(curr.row + d.first, curr.col + d.second)
            }
        }
    }

    private fun generateKingMoves(pos: Position, piece: Piece, moves: MutableList<Move>) {
        for (r in -1..1) {
            for (c in -1..1) {
                if (r == 0 && c == 0) continue
                val next = Position(pos.row + r, pos.col + c)
                if (next.isValid()) {
                    val target = board[next]
                    if (target == null || target.color != piece.color) {
                        moves.add(Move(pos, next, piece, target))
                    }
                }
            }
        }

        // Castling
        val isWhite = piece.color == PieceColor.White
        val rank = if (isWhite) 7 else 0
        if (pos == Position(rank, 4) && !isInCheck(piece.color)) {
            val kRights = if (isWhite) castlingRights[0] else castlingRights[2]
            val qRights = if (isWhite) castlingRights[1] else castlingRights[3]

            if (kRights && board[Position(rank, 5)] == null && board[Position(rank, 6)] == null) {
                if (!isUnderAttack(Position(rank, 5), piece.color) && !isUnderAttack(Position(rank, 6), piece.color)) {
                    moves.add(Move(pos, Position(rank, 6), piece, isCastling = true))
                }
            }
            if (qRights && board[Position(rank, 3)] == null && board[Position(rank, 2)] == null && board[Position(rank, 1)] == null) {
                if (!isUnderAttack(Position(rank, 3), piece.color) && !isUnderAttack(Position(rank, 2), piece.color)) {
                    moves.add(Move(pos, Position(rank, 2), piece, isCastling = true))
                }
            }
        }
    }

    fun isInCheck(color: PieceColor = currentTurn): Boolean {
        var kingPos: Position? = null
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[Position(r, c)]
                if (p != null && p.type == PieceType.King && p.color == color) {
                    kingPos = Position(r, c)
                    break
                }
            }
        }
        if (kingPos == null) return false
        return isUnderAttack(kingPos, color)
    }

    private fun leavesKingInCheck(move: Move, color: PieceColor): Boolean {
        makeMove(move)
        val inCheck = isInCheck(color)
        undoMove()
        return inCheck
    }

    private fun isUnderAttack(pos: Position, friendlyColor: PieceColor): Boolean {
        val enemyColor = friendlyColor.opposite()
        
        // Check pawns
        val pawnDir = if (friendlyColor == PieceColor.White) -1 else 1
        for (c in arrayOf(-1, 1)) {
            val prev = Position(pos.row - pawnDir, pos.col + c)
            if (prev.isValid()) {
                val p = board[prev]
                if (p != null && p.color == enemyColor && p.type == PieceType.Pawn) return true
            }
        }

        // Check knights
        val knightJumps = arrayOf(Pair(2, 1), Pair(2, -1), Pair(-2, 1), Pair(-2, -1), Pair(1, 2), Pair(1, -2), Pair(-1, 2), Pair(-1, -2))
        for (j in knightJumps) {
            val next = Position(pos.row + j.first, pos.col + j.second)
            if (next.isValid()) {
                val p = board[next]
                if (p != null && p.color == enemyColor && p.type == PieceType.Knight) return true
            }
        }

        // Check sliding pieces
        fun checkSliding(directions: Array<Pair<Int, Int>>, types: List<PieceType>): Boolean {
            for (d in directions) {
                var curr = Position(pos.row + d.first, pos.col + d.second)
                while (curr.isValid()) {
                    val p = board[curr]
                    if (p != null) {
                        if (p.color == enemyColor && p.type in types) return true
                        break // Blocked
                    }
                    curr = Position(curr.row + d.first, curr.col + d.second)
                }
            }
            return false
        }

        if (checkSliding(arrayOf(Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)), listOf(PieceType.Bishop, PieceType.Queen))) return true
        if (checkSliding(arrayOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1)), listOf(PieceType.Rook, PieceType.Queen))) return true

        // Check king
        for (r in -1..1) {
            for (c in -1..1) {
                if (r == 0 && c == 0) continue
                val next = Position(pos.row + r, pos.col + c)
                if (next.isValid()) {
                    val p = board[next]
                    if (p != null && p.color == enemyColor && p.type == PieceType.King) return true
                }
            }
        }

        return false
    }

    // --- AI LOGIC (Minimax w/ Alpha-Beta) ---
    sealed class Difficulty(val depth: Int) {
        object Easy : Difficulty(1)
        object Medium : Difficulty(2)
        object Hard : Difficulty(3) // 3 is usually fast enough for native Kotlin on Android without freezing the UI thread for too long.
    }

    fun getBestMove(difficulty: Difficulty): Move? {
        val maxDepth = difficulty.depth
        var bestMove: Move? = null
        var maxScore = -999999
        val moves = getLegalMoves(currentTurn).shuffled() // Shuffle for variety in equal positions

        if (moves.isEmpty()) return null

        val alpha = -999999
        val beta = 999999

        for (move in moves) {
            makeMove(move)
            val score = -minimax(maxDepth - 1, -beta, -alpha)
            undoMove()
            
            if (score > maxScore) {
                maxScore = score
                bestMove = move
            }
        }

        return bestMove ?: moves.firstOrNull()
    }

    private fun minimax(depth: Int, alpha: Int, beta: Int): Int {
        if (depth == 0) return evaluateBoard() * (if (currentTurn == PieceColor.White) 1 else -1)

        val moves = getLegalMoves(currentTurn)
        if (moves.isEmpty()) {
            if (isInCheck()) return -99999 + depth // Prefer longer mates
            return 0 // Stalemate
        }

        var currentAlpha = alpha
        var maxScore = -999999

        for (move in moves) {
            makeMove(move)
            val score = -minimax(depth - 1, -beta, -currentAlpha)
            undoMove()

            if (score >= beta) return score
            if (score > maxScore) {
                maxScore = score
                currentAlpha = max(currentAlpha, score)
            }
        }
        return maxScore
    }

    private fun evaluateBoard(): Int {
        var score = 0
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[Position(r, c)]
                if (piece != null) {
                    val value = getPieceValue(piece.type)
                    
                    // Simple positional bonuses (central control)
                    val positionalScore = if (piece.type == PieceType.Pawn || piece.type == PieceType.Knight) {
                        val centerDistRow = abs(3.5 - r)
                        val centerDistCol = abs(3.5 - c)
                        (3 - centerDistRow.toInt()) + (3 - centerDistCol.toInt())
                    } else 0

                    if (piece.color == PieceColor.White) {
                        score += value + positionalScore
                    } else {
                        score -= value + positionalScore
                    }
                }
            }
        }
        return score
    }

    private fun getPieceValue(type: PieceType): Int = when (type) {
        PieceType.Pawn -> 100
        PieceType.Knight -> 320
        PieceType.Bishop -> 330
        PieceType.Rook -> 500
        PieceType.Queen -> 900
        PieceType.King -> 20000
    }
}
