package com.tron3d.models

import com.badlogic.gdx.math.Vector2

/**
 * Estado completo del juego
 * Contiene toda la información necesaria para renderizar y jugar
 */
data class GameState(
    // Posiciones de los jugadores
    val player1Position: Vector2 = Vector2(10f, 15f),
    val player2Position: Vector2 = Vector2(40f, 15f),

    // Direcciones actuales
    val player1Direction: Direction = Direction.RIGHT,
    val player2Direction: Direction = Direction.LEFT,

    // Trails (rastros dejados por las motos)
    val player1Trail: List<Vector2> = listOf(player1Position.cpy()),
    val player2Trail: List<Vector2> = listOf(player2Position.cpy()),

    // Estado del juego
    val status: GameStatus = GameStatus.PLAYING,
    val currentTurn: PlayerTurn = PlayerTurn.PLAYER1,

    // Puntuación
    val player1Score: Int = 0,
    val player2Score: Int = 0,

    // Configuración
    val isMultiplayer: Boolean = true,
    val gridWidth: Int = 50,
    val gridHeight: Int = 30,

    // Round actual
    val currentRound: Int = 1
) {
    /**
     * Verifica si una posición está ocupada por algún trail
     */
    fun isPositionOccupied(position: Vector2): Boolean {
        return player1Trail.any { it.epsilonEquals(position, 0.1f) } ||
            player2Trail.any { it.epsilonEquals(position, 0.1f) }
    }

    /**
     * Verifica si una posición está fuera de los límites
     */
    fun isOutOfBounds(position: Vector2): Boolean {
        return position.x < 0 || position.x >= gridWidth ||
            position.y < 0 || position.y >= gridHeight
    }

    /**
     * Retorna una copia del estado con un trail actualizado para el jugador 1
     */
    fun withPlayer1Trail(newTrail: List<Vector2>): GameState {
        return copy(player1Trail = newTrail)
    }

    /**
     * Retorna una copia del estado con un trail actualizado para el jugador 2
     */
    fun withPlayer2Trail(newTrail: List<Vector2>): GameState {
        return copy(player2Trail = newTrail)
    }

    /**
     * Retorna el ganador del round actual (si lo hay)
     */
    fun getWinner(): PlayerTurn? {
        return when (status) {
            GameStatus.PLAYER1_WON -> PlayerTurn.PLAYER1
            GameStatus.PLAYER2_WON -> PlayerTurn.PLAYER2
            else -> null
        }
    }
}
