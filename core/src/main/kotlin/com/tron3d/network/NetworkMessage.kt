// core/src/main/kotlin/com/tron3d/network/NetworkMessage.kt
package com.tron3d.network

import com.tron3d.models.Direction

/**
 * Tipos de mensajes de red
 */
enum class MessageType {
    MOVE,           // Movimiento de un jugador
    PAUSE,          // Pausar juego
    RESUME,         // Reanudar juego
    RESET_GAME,     // Reiniciar juego
    PING,           // Verificar conexión
    SYNC_STATE      // Sincronizar estado completo
}

/**
 * Mensaje de red para comunicación Bluetooth
 */
data class NetworkMessage(
    val type: MessageType,
    val playerId: Int = 0,
    val direction: Direction? = null,
    val timestamp: Long = System.currentTimeMillis()
)
