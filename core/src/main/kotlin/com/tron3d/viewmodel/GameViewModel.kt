package com.tron3d.viewmodel

import com.badlogic.gdx.math.Vector2
import com.tron3d.models.Direction
import com.tron3d.models.GameState
import com.tron3d.models.GameStatus
import com.tron3d.models.PlayerTurn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel que maneja la lógica del juego TRON
 * Controla: movimientos, colisiones, puntuación, turnos
 */
class GameViewModel {

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val moveSpeed = 1f // Velocidad de movimiento (unidades por turno)

    /**
     * Inicia un nuevo juego
     */
    fun startNewGame() {
        _gameState.value = GameState(
            player1Position = Vector2(10f, 15f),
            player2Position = Vector2(40f, 15f),
            player1Direction = Direction.RIGHT,
            player2Direction = Direction.LEFT,
            player1Trail = listOf(Vector2(10f, 15f)),
            player2Trail = listOf(Vector2(40f, 15f)),
            status = GameStatus.PLAYING,
            currentTurn = PlayerTurn.PLAYER1,
            player1Score = 0,
            player2Score = 0,
            currentRound = 1
        )
    }

    /**
     * Realiza un movimiento en la dirección especificada
     */
    fun makeMove(direction: Direction) {
        val state = _gameState.value

        if (state.status != GameStatus.PLAYING) {
            return
        }

        val currentTurn = state.currentTurn

        // Verificar que no sea dirección opuesta (no puede dar vuelta en U)
        val currentDirection = if (currentTurn == PlayerTurn.PLAYER1) {
            state.player1Direction
        } else {
            state.player2Direction
        }

        if (direction.isOpposite(currentDirection)) {
            return // Movimiento inválido
        }

        // Actualizar dirección y posición
        if (currentTurn == PlayerTurn.PLAYER1) {
            updatePlayer1(direction)
        } else {
            updatePlayer2(direction)
        }

        // Cambiar turno si es multijugador
        if (state.isMultiplayer) {
            _gameState.value = _gameState.value.copy(
                currentTurn = currentTurn.next()
            )
        }
    }

    private fun updatePlayer1(newDirection: Direction) {
        val state = _gameState.value
        val movement = newDirection.toVector()
        val newPosition = Vector2(
            state.player1Position.x + movement.first * moveSpeed,
            state.player1Position.y + movement.second * moveSpeed
        )

        // Verificar colisión
        if (checkCollision(newPosition, state)) {
            endRound(PlayerTurn.PLAYER2) // Player 1 perdió
            return
        }

        // Actualizar estado
        val newTrail = state.player1Trail + newPosition
        _gameState.value = state.copy(
            player1Position = newPosition,
            player1Direction = newDirection,
            player1Trail = newTrail
        )
    }

    private fun updatePlayer2(newDirection: Direction) {
        val state = _gameState.value
        val movement = newDirection.toVector()
        val newPosition = Vector2(
            state.player2Position.x + movement.first * moveSpeed,
            state.player2Position.y + movement.second * moveSpeed
        )

        // Verificar colisión
        if (checkCollision(newPosition, state)) {
            endRound(PlayerTurn.PLAYER1) // Player 2 perdió
            return
        }

        // Actualizar estado
        val newTrail = state.player2Trail + newPosition
        _gameState.value = state.copy(
            player2Position = newPosition,
            player2Direction = newDirection,
            player2Trail = newTrail
        )
    }

    /**
     * Verifica si una posición causa colisión
     */
    private fun checkCollision(position: Vector2, state: GameState): Boolean {
        // Colisión con bordes
        if (state.isOutOfBounds(position)) {
            return true
        }

        // Colisión con trails (excluyendo la posición actual)
        if (state.isPositionOccupied(position)) {
            return true
        }

        return false
    }

    /**
     * Termina el round actual con un ganador
     */
    private fun endRound(winner: PlayerTurn) {
        val state = _gameState.value

        val newStatus = if (winner == PlayerTurn.PLAYER1) {
            GameStatus.PLAYER1_WON
        } else {
            GameStatus.PLAYER2_WON
        }

        val newPlayer1Score = if (winner == PlayerTurn.PLAYER1) {
            state.player1Score + 1
        } else {
            state.player1Score
        }

        val newPlayer2Score = if (winner == PlayerTurn.PLAYER2) {
            state.player2Score + 1
        } else {
            state.player2Score
        }

        _gameState.value = state.copy(
            status = newStatus,
            player1Score = newPlayer1Score,
            player2Score = newPlayer2Score
        )
    }

    /**
     * Reinicia el round (mantiene puntuación)
     */
    fun restartRound() {
        val state = _gameState.value

        _gameState.value = GameState(
            player1Position = Vector2(10f, 15f),
            player2Position = Vector2(40f, 15f),
            player1Direction = Direction.RIGHT,
            player2Direction = Direction.LEFT,
            player1Trail = listOf(Vector2(10f, 15f)),
            player2Trail = listOf(Vector2(40f, 15f)),
            status = GameStatus.PLAYING,
            currentTurn = PlayerTurn.PLAYER1,
            player1Score = state.player1Score,
            player2Score = state.player2Score,
            currentRound = state.currentRound + 1,
            isMultiplayer = state.isMultiplayer,
            gridWidth = state.gridWidth,
            gridHeight = state.gridHeight
        )
    }

    /**
     * Pausa el juego
     */
    fun pauseGame() {
        val state = _gameState.value
        if (state.status == GameStatus.PLAYING) {
            _gameState.value = state.copy(status = GameStatus.PAUSED)
        }
    }

    /**
     * Resume el juego
     */
    fun resumeGame() {
        val state = _gameState.value
        if (state.status == GameStatus.PAUSED) {
            _gameState.value = state.copy(status = GameStatus.PLAYING)
        }
    }
}
