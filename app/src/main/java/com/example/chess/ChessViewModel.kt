package com.example.chess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GameState(
    val board: Board = Board(),
    val currentTurn: PieceColor = PieceColor.White,
    val selectedPos: Position? = null,
    val legalMovesForSelected: List<Move> = emptyList(),
    val moveHistory: List<Move> = emptyList(),
    val isGameOver: Boolean = false,
    val winner: PieceColor? = null,
    val isThinking: Boolean = false
)

class ChessViewModel : ViewModel() {
    private val engine = ChessEngine()
    
    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState

    var gameMode: GameMode = GameMode.PlayerVsPlayer
    
    enum class GameMode {
        PlayerVsPlayer, PlayerVsAI_Easy, PlayerVsAI_Medium, PlayerVsAI_Hard
    }

    init {
        updateState()
    }

    fun startGame(mode: GameMode) {
        this.gameMode = mode
        engine.restart()
        updateState()
    }

    fun onSquareClick(pos: Position) {
        val state = _uiState.value
        if (state.isGameOver || state.isThinking) return

        // If it's my turn
        if (gameMode != GameMode.PlayerVsPlayer && engine.currentTurn == PieceColor.Black) return // Assuming Player is always White in PvAI

        val piece = engine.board[pos]
        
        // If clicking on a valid move
        val clickedMove = state.legalMovesForSelected.find { it.to == pos }
        if (clickedMove != null) {
            engine.makeMove(clickedMove)
            updateState()
            
            checkGameOver()
            
            // Trigger AI if needed
            if (!_uiState.value.isGameOver && gameMode != GameMode.PlayerVsPlayer && engine.currentTurn == PieceColor.Black) {
                makeAIMove()
            }
        } else {
            // Select piece
            if (piece != null && piece.color == engine.currentTurn) {
                _uiState.value = state.copy(
                    selectedPos = pos,
                    legalMovesForSelected = engine.getPseudoLegalMovesForPiece(pos).filter { m -> 
                        engine.getLegalMoves().any { it.from == m.from && it.to == m.to } 
                    }
                )
            } else {
                // Clear selection
                _uiState.value = state.copy(selectedPos = null, legalMovesForSelected = emptyList())
            }
        }
    }

    private fun makeAIMove() {
        _uiState.value = _uiState.value.copy(isThinking = true)
        viewModelScope.launch {
            val difficulty = when (gameMode) {
                GameMode.PlayerVsAI_Easy -> ChessEngine.Difficulty.Easy
                GameMode.PlayerVsAI_Medium -> ChessEngine.Difficulty.Medium
                GameMode.PlayerVsAI_Hard -> ChessEngine.Difficulty.Hard
                else -> ChessEngine.Difficulty.Easy
            }
            
            val bestMove = withContext(Dispatchers.Default) {
                engine.getBestMove(difficulty)
            }
            
            if (bestMove != null) {
                engine.makeMove(bestMove)
            }
            updateState()
            checkGameOver()
            _uiState.value = _uiState.value.copy(isThinking = false)
        }
    }

    fun undo() {
        if (_uiState.value.isThinking) return
        engine.undoMove()
        if (gameMode != GameMode.PlayerVsPlayer) {
            // Undo twice to undo AI move as well
            engine.undoMove()
        }
        updateState()
    }
    
    fun restart() {
        engine.restart()
        updateState()
    }

    private fun updateState() {
        _uiState.value = _uiState.value.copy(
            board = engine.board.copy(),
            currentTurn = engine.currentTurn,
            selectedPos = null,
            legalMovesForSelected = emptyList(),
            moveHistory = engine.moveHistory.toList()
        )
    }

    private fun checkGameOver() {
        val legalMoves = engine.getLegalMoves()
        if (legalMoves.isEmpty()) {
            if (engine.isInCheck()) {
                _uiState.value = _uiState.value.copy(isGameOver = true, winner = if (engine.currentTurn == PieceColor.White) PieceColor.Black else PieceColor.White)
            } else {
                // Stalemate
                _uiState.value = _uiState.value.copy(isGameOver = true, winner = null)
            }
        }
    }
}
