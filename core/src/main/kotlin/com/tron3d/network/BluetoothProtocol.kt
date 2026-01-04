package com.tron3d.network

import com.badlogic.gdx.math.Vector2
import com.tron3d.models.Direction

/**
 * Protocolo de mensajes para sincronización Bluetooth en TRON
 * Usa formato simple de texto delimitado por pipes (|)
 */
object BluetoothProtocol {

    // Tipos de mensajes
    const val MSG_PLAYER_MOVE = "MOVE"
    const val MSG_COLLISION = "COLLISION"
    const val MSG_GAME_OVER = "GAMEOVER"

    /**
     * Crea un mensaje de movimiento del jugador
     * Formato: MOVE|player|posX|posY|direction|trailSize|x1,y1|x2,y2|...
     */
    fun createPlayerMoveMessage(
        playerNumber: Int,
        position: Vector2,
        direction: Direction,
        trail: List<Vector2>
    ): String {
        val sb = StringBuilder()
        sb.append(MSG_PLAYER_MOVE)
        sb.append("|").append(playerNumber)
        sb.append("|").append(position.x)
        sb.append("|").append(position.y)
        sb.append("|").append(direction.name)
        sb.append("|").append(trail.size)

        // Agregar trail (últimos 50 puntos para no saturar)
        val maxTrailPoints = 50
        val trailToSend = if (trail.size > maxTrailPoints) {
            trail.takeLast(maxTrailPoints)
        } else {
            trail
        }

        trailToSend.forEach { point ->
            sb.append("|").append(point.x).append(",").append(point.y)
        }

        return sb.toString()
    }

    /**
     * Parsea un mensaje de movimiento
     * Formato esperado: MOVE|player|posX|posY|direction|trailSize|x1,y1|x2,y2|...
     */
    fun parsePlayerMoveMessage(message: String): PlayerMoveData? {
        return try {
            val parts = message.split("|")

            if (parts.isEmpty() || parts[0] != MSG_PLAYER_MOVE) {
                return null
            }

            if (parts.size < 6) {
                return null
            }

            val playerNumber = parts[1].toInt()
            val posX = parts[2].toFloat()
            val posY = parts[3].toFloat()
            val position = Vector2(posX, posY)
            val direction = Direction.valueOf(parts[4])
            val trailSize = parts[5].toInt()

            // Parsear trail
            val trail = mutableListOf<Vector2>()
            for (i in 6 until minOf(6 + trailSize, parts.size)) {
                val coords = parts[i].split(",")
                if (coords.size == 2) {
                    val x = coords[0].toFloat()
                    val y = coords[1].toFloat()
                    trail.add(Vector2(x, y))
                }
            }

            PlayerMoveData(playerNumber, position, direction, trail)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Crea mensaje de colisión
     * Formato: COLLISION|player
     */
    fun createCollisionMessage(playerNumber: Int): String {
        return "$MSG_COLLISION|$playerNumber"
    }

    /**
     * Crea mensaje de game over CON PUNTUACIÓN
     * Formato: GAMEOVER|winner|player1Score|player2Score|currentRound
     */
    fun createGameOverMessageWithScore(
        winner: Int,
        player1Score: Int,
        player2Score: Int,
        currentRound: Int
    ): String {
        return "$MSG_GAME_OVER|$winner|$player1Score|$player2Score|$currentRound"
    }

    /**
     * Parsea mensaje de game over con puntuación
     */
    fun parseGameOverMessage(message: String): GameOverData? {
        return try {
            val parts = message.split("|")

            if (parts.isEmpty() || parts[0] != MSG_GAME_OVER) {
                return null
            }

            if (parts.size < 5) {
                return null
            }

            val winner = parts[1].toInt()
            val player1Score = parts[2].toInt()
            val player2Score = parts[3].toInt()
            val currentRound = parts[4].toInt()

            GameOverData(winner, player1Score, player2Score, currentRound)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * Datos de movimiento del jugador
 */
data class PlayerMoveData(
    val playerNumber: Int,
    val position: Vector2,
    val direction: Direction,
    val trail: List<Vector2>
)

/**
 * Data class para datos de game over
 */
data class GameOverData(
    val winner: Int,
    val player1Score: Int,
    val player2Score: Int,
    val currentRound: Int
)
