package com.tron3d.models

/**
 * Indica de quién es el turno en modo multijugador
 */
enum class PlayerTurn {
    PLAYER1,
    PLAYER2;

    /**
     * Cambia al siguiente turno
     */
    fun next(): PlayerTurn {
        return when (this) {
            PLAYER1 -> PLAYER2
            PLAYER2 -> PLAYER1
        }
    }

    /**
     * Retorna el nombre del jugador para mostrar en UI
     */
    fun getDisplayName(): String {
        return when (this) {
            PLAYER1 -> "Jugador 1"
            PLAYER2 -> "Jugador 2"
        }
    }
}
