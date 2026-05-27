package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chess.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChessApp()
                }
            }
        }
    }
}

@Composable
fun ChessApp() {
    val navController = rememberNavController()
    val viewModel: ChessViewModel = viewModel()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(navController)
        }
        composable("home") {
            HomeScreen(navController, viewModel)
        }
        composable("game") {
            GameScreen(navController, viewModel)
        }
    }
}

@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        delay(1500)
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WoodBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "♞",
                fontSize = 120.sp,
                color = PrimaryWoodLight
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "CHESS",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun HomeScreen(navController: NavController, viewModel: ChessViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoodBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "PLAY",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryWoodLight,
            letterSpacing = 4.sp,
            modifier = Modifier.padding(bottom = 60.dp)
        )

        MenuButton("Player vs Player") {
            viewModel.startGame(ChessViewModel.GameMode.PlayerVsPlayer)
            navController.navigate("game")
        }
        MenuButton("Play vs AI (Easy)") {
            viewModel.startGame(ChessViewModel.GameMode.PlayerVsAI_Easy)
            navController.navigate("game")
        }
        MenuButton("Play vs AI (Medium)") {
            viewModel.startGame(ChessViewModel.GameMode.PlayerVsAI_Medium)
            navController.navigate("game")
        }
        MenuButton("Play vs AI (Hard)") {
            viewModel.startGame(ChessViewModel.GameMode.PlayerVsAI_Hard)
            navController.navigate("game")
        }
    }
}

@Composable
fun MenuButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = WoodCard, contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
    ) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GameScreen(navController: NavController, viewModel: ChessViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WoodBackground)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar(navController = navController, onRestart = { viewModel.restart() })

        Spacer(modifier = Modifier.height(16.dp))

        // Status Text
        val statusText = when {
            state.isGameOver -> if (state.winner == null) "Stalemate!" else "${state.winner} Wins!"
            state.isThinking -> "AI is thinking..."
            else -> "${state.currentTurn}'s Turn"
        }

        Text(
            text = statusText,
            color = PrimaryWoodLight,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Chess Board
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .aspectRatio(1f)
                .shadow(16.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(WoodCard)
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 0..7) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (c in 0..7) {
                            val pos = Position(r, c)
                            val piece = state.board[pos]
                            val isLightSquare = (r + c) % 2 == 0
                            val squareColor = if (isLightSquare) BoardLight else BoardDark

                            val isSelected = state.selectedPos == pos
                            val isLegalMove = state.legalMovesForSelected.any { it.to == pos }
                            val isLastMove = state.moveHistory.lastOrNull()?.let { it.from == pos || it.to == pos } == true

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        when {
                                            isSelected -> Highlight
                                            isLegalMove -> squareColor.copy(alpha = 0.5f) // overlay blend
                                            isLastMove -> LastMoveHighlight.copy(alpha = 0.7f)
                                            else -> squareColor
                                        }
                                    )
                                    .clickable { viewModel.onSquareClick(pos) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLegalMove && piece == null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(0.25f)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.2f))
                                    )
                                }
                                
                                if (isLegalMove && piece != null) {
                                     Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(Color.Transparent)
                                            .background(Color.Red.copy(alpha = 0.4f), CircleShape)
                                    )
                                }

                                if (piece != null) {
                                    Text(
                                        text = pieceToChar(piece.type),
                                        fontSize = 32.sp, // Will scale well inside the box
                                        color = if (piece.color == PieceColor.White) Color(0xFFFAFAFA) else Color(0xFF111111),
                                        style = androidx.compose.ui.text.TextStyle(
                                            shadow = androidx.compose.ui.graphics.Shadow(
                                                color = Color.Black.copy(alpha = 0.3f),
                                                blurRadius = 4f,
                                                offset = androidx.compose.ui.geometry.Offset(2f, 2f)
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { viewModel.undo() },
                colors = ButtonDefaults.buttonColors(containerColor = WoodCard),
                modifier = Modifier.width(120.dp)
            ) {
                Text("Undo", color = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Move history preview (Optional basic view)
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            items(state.moveHistory.chunked(2)) { turn ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val wMove = turn.getOrNull(0)
                    val bMove = turn.getOrNull(1)
                    Text(text = wMove?.let { formatMove(it) } ?: "", color = Color.LightGray, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text(text = bMove?.let { formatMove(it) } ?: "", color = Color.LightGray, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun TopBar(navController: NavController, onRestart: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = { navController.popBackStack() }) {
            Text("← Back", color = PrimaryWoodLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onRestart) {
            Text("Restart", color = PrimaryWoodLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

fun pieceToChar(type: PieceType): String = when (type) {
    PieceType.Pawn -> "♟"
    PieceType.Knight -> "♞"
    PieceType.Bishop -> "♝"
    PieceType.Rook -> "♜"
    PieceType.Queen -> "♛"
    PieceType.King -> "♚"
}

fun formatMove(move: Move): String {
    val fileFrom = (move.from.col + 'a'.code).toChar()
    val rankFrom = 8 - move.from.row
    val fileTo = (move.to.col + 'a'.code).toChar()
    val rankTo = 8 - move.to.row
    val pieceStr = when(move.pieceMoved.type) {
        PieceType.Pawn -> ""
        PieceType.Knight -> "N"
        PieceType.Bishop -> "B"
        PieceType.Rook -> "R"
        PieceType.Queen -> "Q"
        PieceType.King -> "K"
    }
    val capture = if (move.pieceCaptured != null || move.isEnPassant) "x" else ""
    return "$pieceStr$fileFrom$rankFrom$capture$fileTo$rankTo"
}
