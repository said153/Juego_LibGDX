package com.tron3d.models

/**
 * Estados posibles del juego
 */
enum class GameStatus {
    /**
     * El juego está en curso, los jugadores pueden moverse
     */
    PLAYING,

    /**
     * El juego está pausado
     */
    PAUSED,

    /**
     * El juego terminó - Jugador 1 ganó
     */
    PLAYER1_WON,

    /**
     * El juego terminó - Jugador 2 ganó
     */
    PLAYER2_WON,

    /**
     * El juego terminó - Empate (ambos chocaron al mismo tiempo)
     */
    DRAW;

    /**
     * Retorna si el juego está activo
     */
    fun isActive(): Boolean {
        return this == PLAYING
    }

    /**
     * Retorna si el juego terminó
     */
    fun isGameOver(): Boolean {
        return this == PLAYER1_WON || this == PLAYER2_WON || this == DRAW
    }

    /**
     * Retorna el mensaje para mostrar cuando termina el juego
     */
    fun getEndMessage(): String {
        return when (this) {
            PLAYER1_WON -> "¡Jugador 1 Gana!"
            PLAYER2_WON -> "¡Jugador 2 Gana!"
            DRAW -> "¡Empate!"
            PLAYING -> "Jugando..."
            PAUSED -> "Pausado"
        }
    }
}
