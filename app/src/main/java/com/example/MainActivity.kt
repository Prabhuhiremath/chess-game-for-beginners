package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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

/** A soft sky-to-grape gradient used as the playful background everywhere. */
private fun skyBrush() = Brush.verticalGradient(listOf(SkyTop, SkyBottom))

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
        delay(1900)
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
        }
    }

    // Gentle bounce + wiggle for the mascot knight.
    val transition = rememberInfiniteTransition(label = "splash")
    val bounce by transition.animateFloat(
        initialValue = 0f,
        targetValue = -28f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    val wiggle by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wiggle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(skyBrush()),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .offset(y = bounce.dp)
                    .size(160.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\uD83D\uDC0E", // horse mascot
                    fontSize = 96.sp,
                    modifier = Modifier.rotate(wiggle)
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Chess Kids",
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = Color.White
            )
            Text(
                text = "Let's learn and play!",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

private data class GameOption(
    val title: String,
    val subtitle: String,
    val emoji: String,
    val color: Color,
    val mode: ChessViewModel.GameMode
)

@Composable
fun HomeScreen(navController: NavController, viewModel: ChessViewModel) {
    val options = listOf(
        GameOption("Two Players", "Play with a friend", "\uD83D\uDC6B", Bubblegum, ChessViewModel.GameMode.PlayerVsPlayer),
        GameOption("Easy Robot", "A gentle first opponent", "\uD83E\uDD16", Leaf, ChessViewModel.GameMode.PlayerVsAI_Easy),
        GameOption("Clever Robot", "Thinks a little harder", "\uD83E\uDDE0", Tangerine, ChessViewModel.GameMode.PlayerVsAI_Medium),
        GameOption("Super Robot", "A real brain teaser", "\uD83D\uDC51", Grape, ChessViewModel.GameMode.PlayerVsAI_Hard)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(skyBrush())
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text("\uD83D\uDC0E", fontSize = 64.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Chess Kids",
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 1.sp
        )
        Text(
            text = "Pick how you want to play!",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.padding(bottom = 28.dp)
        )

        options.forEach { option ->
            MenuCard(option) {
                viewModel.startGame(option.mode)
                navController.navigate("game")
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MenuCard(option: GameOption, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(option.color),
            contentAlignment = Alignment.Center
        ) {
            Text(option.emoji, fontSize = 30.sp)
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                option.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = DarkText
            )
            Text(
                option.subtitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DarkText.copy(alpha = 0.6f)
            )
        }
        Text("\u25B6", fontSize = 22.sp, color = option.color)
    }
}

@Composable
fun GameScreen(navController: NavController, viewModel: ChessViewModel) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(skyBrush())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopBar(navController = navController, onRestart = { viewModel.restart() })

            Spacer(modifier = Modifier.height(8.dp))

            // Captured pieces collected by the opponent of the player at the top.
            CapturedTray(pieces = state.capturedByBlack, color = PieceColor.White)

            Spacer(modifier = Modifier.height(8.dp))

            StatusBanner(state)

            Spacer(modifier = Modifier.height(12.dp))

            ChessBoard(state = state, onSquareClick = { viewModel.onSquareClick(it) })

            Spacer(modifier = Modifier.height(8.dp))

            CapturedTray(pieces = state.capturedByWhite, color = PieceColor.Black)

            Spacer(modifier = Modifier.height(16.dp))

            // Big friendly controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BigButton(
                    text = "Take Back",
                    emoji = "\u21A9\uFE0F",
                    color = Sunny,
                    modifier = Modifier.weight(1f)
                ) { viewModel.undo() }
                BigButton(
                    text = "New Game",
                    emoji = "\uD83D\uDD04",
                    color = Leaf,
                    modifier = Modifier.weight(1f)
                ) { viewModel.restart() }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Celebration overlay on game over
        AnimatedVisibility(
            visible = state.isGameOver,
            enter = fadeIn() + scaleIn(initialScale = 0.6f)
        ) {
            GameOverOverlay(
                state = state,
                onPlayAgain = { viewModel.restart() },
                onHome = {
                    navController.popBackStack("home", inclusive = false)
                }
            )
        }
    }
}

@Composable
fun StatusBanner(state: GameState) {
    // Friendly, kid-appropriate status messages.
    val (message, emoji) = when {
        state.isGameOver && state.winner == null -> "It's a tie! Great game!" to "\uD83E\uDD1D"
        state.isGameOver && state.winner == PieceColor.White -> "White wins! Hooray!" to "\uD83C\uDF89"
        state.isGameOver && state.winner == PieceColor.Black ->
            (if (state.vsComputer) "The robot wins! Try again!" else "Black wins! Hooray!") to "\uD83C\uDF89"
        state.isThinking -> "Robot is thinking..." to "\uD83E\uDD14"
        state.isCheck && state.currentTurn == PieceColor.White -> "Careful! White king is in check!" to "\u26A0\uFE0F"
        state.isCheck && state.currentTurn == PieceColor.Black -> "Careful! Black king is in check!" to "\u26A0\uFE0F"
        state.currentTurn == PieceColor.White -> "White's turn" to "\u26AA"
        else -> (if (state.vsComputer) "Robot's turn" else "Black's turn") to "\u26AB"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(vertical = 12.dp, horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 24.sp)
        Spacer(Modifier.width(10.dp))
        Text(
            text = message,
            color = DarkText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ChessBoard(state: GameState, onSquareClick: (Position) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .aspectRatio(1f)
            .shadow(16.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(10.dp)
            .clip(RoundedCornerShape(10.dp))
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

                        // Highlight a king that is currently in check.
                        val isCheckedKing = state.isCheck &&
                            piece != null &&
                            piece.type == PieceType.King &&
                            piece.color == state.currentTurn

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    when {
                                        isCheckedKing -> CheckHighlight
                                        isSelected -> Highlight
                                        isLastMove -> LastMoveHighlight
                                        else -> squareColor
                                    }
                                )
                                .clickable { onSquareClick(pos) },
                            contentAlignment = Alignment.Center
                        ) {
                            // Green dot for empty legal-move squares.
                            if (isLegalMove && piece == null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(0.34f)
                                        .clip(CircleShape)
                                        .background(MoveDotColor)
                                )
                            }
                            // Orange capture ring for legal captures.
                            if (isLegalMove && piece != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(0.92f)
                                        .clip(CircleShape)
                                        .background(CaptureRing.copy(alpha = 0.35f))
                                )
                            }

                            if (piece != null) {
                                PieceGlyph(piece)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PieceGlyph(piece: Piece) {
    // Gentle pop-in when pieces appear/move.
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(220, easing = EaseOutBack),
        label = "pieceScale"
    )
    Text(
        text = pieceToChar(piece.type),
        fontSize = 34.sp,
        color = if (piece.color == PieceColor.White) Color(0xFFFFFFFF) else Color(0xFF2B2B2B),
        modifier = Modifier.scale(scale),
        style = androidx.compose.ui.text.TextStyle(
            shadow = androidx.compose.ui.graphics.Shadow(
                color = Color.Black.copy(alpha = 0.35f),
                blurRadius = 5f,
                offset = androidx.compose.ui.geometry.Offset(2f, 3f)
            )
        )
    )
}

/** Shows the pieces a side has captured as a little trophy row. */
@Composable
fun CapturedTray(pieces: List<PieceType>, color: PieceColor) {
    val glyphColor = if (color == PieceColor.White) Color(0xFFFFFFFF) else Color(0xFF2B2B2B)
    Row(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .height(34.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.35f))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (pieces.isEmpty()) {
            Text(
                "No pieces captured yet",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText.copy(alpha = 0.5f)
            )
        } else {
            // Sort by value so the trophy row looks tidy (big pieces first).
            pieces.sortedByDescending { pieceValue(it) }.forEach { type ->
                Text(
                    text = pieceToChar(type),
                    fontSize = 20.sp,
                    color = glyphColor,
                    modifier = Modifier.padding(end = 2.dp)
                )
            }
        }
    }
}

@Composable
fun BigButton(text: String, emoji: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .height(58.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun GameOverOverlay(state: GameState, onPlayAgain: () -> Unit, onHome: () -> Unit) {
    val title: String
    val emoji: String
    when {
        state.winner == null -> {
            title = "It's a Tie!"
            emoji = "\uD83E\uDD1D"
        }
        state.vsComputer && state.winner == PieceColor.Black -> {
            title = "Robot Wins!"
            emoji = "\uD83E\uDD16"
        }
        state.vsComputer && state.winner == PieceColor.White -> {
            title = "You Win!"
            emoji = "\uD83C\uDFC6"
        }
        else -> {
            title = "${if (state.winner == PieceColor.White) "White" else "Black"} Wins!"
            emoji = "\uD83C\uDFC6"
        }
    }

    // Pulsing trophy
    val transition = rememberInfiniteTransition(label = "celebrate")
    val pulse by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .shadow(20.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 84.sp, modifier = Modifier.scale(pulse))
            Spacer(Modifier.height(12.dp))
            Text(
                title,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Grape,
                textAlign = TextAlign.Center
            )
            Text(
                "Good game! \u2B50",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(20.dp))
            BigButton(
                text = "Play Again",
                emoji = "\uD83D\uDD04",
                color = Leaf,
                modifier = Modifier.fillMaxWidth()
            ) { onPlayAgain() }
            Spacer(Modifier.height(12.dp))
            BigButton(
                text = "Home",
                emoji = "\uD83C\uDFE0",
                color = Bubblegum,
                modifier = Modifier.fillMaxWidth()
            ) { onHome() }
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
        RoundIconButton(emoji = "\u2B05\uFE0F") { navController.popBackStack() }
        Text(
            "Chess Kids",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black
        )
        RoundIconButton(emoji = "\uD83D\uDD04") { onRestart() }
    }
}

@Composable
fun RoundIconButton(emoji: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 20.sp)
    }
}

fun pieceToChar(type: PieceType): String = when (type) {
    PieceType.Pawn -> "\u265F"
    PieceType.Knight -> "\u265E"
    PieceType.Bishop -> "\u265D"
    PieceType.Rook -> "\u265C"
    PieceType.Queen -> "\u265B"
    PieceType.King -> "\u265A"
}

private fun pieceValue(type: PieceType): Int = when (type) {
    PieceType.Pawn -> 1
    PieceType.Knight -> 3
    PieceType.Bishop -> 3
    PieceType.Rook -> 5
    PieceType.Queen -> 9
    PieceType.King -> 10
}

fun formatMove(move: Move): String {
    val fileFrom = (move.from.col + 'a'.code).toChar()
    val rankFrom = 8 - move.from.row
    val fileTo = (move.to.col + 'a'.code).toChar()
    val rankTo = 8 - move.to.row
    val pieceStr = when (move.pieceMoved.type) {
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
