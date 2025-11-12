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
     * Crea mensaje de game over
     * Formato: GAMEOVER|winner
     */
    fun createGameOverMessage(winner: Int): String {
        return "$MSG_GAME_OVER|$winner"
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
