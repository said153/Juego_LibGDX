// core/src/main/kotlin/com/tron3d/viewmodel/GameViewModel_Bluetooth.kt
package com.tron3d.viewmodel

import com.badlogic.gdx.math.Vector2
import com.tron3d.models.Direction
import com.tron3d.models.GameState
import com.tron3d.models.GameStatus
import com.tron3d.models.PlayerTurn
import com.tron3d.network.BluetoothInterface
import com.tron3d.network.MessageType
import com.tron3d.network.NetworkMessage
import kotlin.math.abs

/**
 * ViewModel del juego con soporte para Bluetooth Multiplayer
 * Sincroniza el estado del juego entre dos dispositivos en tiempo real
 */
class GameViewModel_Bluetooth(
    private val bluetoothManager: BluetoothInterface? = null
) {

    // Estado del juego
    var gameState = GameState()
        private set

    // Callbacks para UI
    var onStateChanged: ((GameState) -> Unit)? = null
    var onGameOver: ((String) -> Unit)? = null
    var onConnectionChanged: ((Boolean) -> Unit)? = null

    // Modo de juego
    var isMultiplayer: Boolean = false
        private set

    // ID del jugador local (1 o 2)
    var localPlayerId: Int = 1
        private set

    init {
        setupBluetoothListeners()
    }

    /**
     * Configura los listeners de Bluetooth
     */
    private fun setupBluetoothListeners() {
        bluetoothManager?.apply {
            // Listener para mensajes recibidos
            messageListener = { message ->
                handleNetworkMessage(message)
            }

            // Listener para cambios de conexión
            connectionListener = { connected ->
                handleConnectionChange(connected)
            }
        }
    }

    /**
     * Inicia una nueva partida local (sin Bluetooth)
     */
    fun startLocalGame() {
        isMultiplayer = false
        localPlayerId = 1
        resetGame()
    }

    /**
     * Inicia una partida multiplayer como HOST
     */
    fun startMultiplayerAsHost(callback: (Boolean) -> Unit) {
        isMultiplayer = true
        localPlayerId = 1 // El host siempre es jugador 1

        bluetoothManager?.startServer { success ->
            if (success) {
                resetGame()
                callback(true)
            } else {
                callback(false)
            }
        }
    }

    /**
     * Inicia una partida multiplayer como CLIENTE
     */
    fun startMultiplayerAsClient(callback: (Boolean) -> Unit) {
        isMultiplayer = true
        localPlayerId = 2 // El cliente siempre es jugador 2

        // En lugar de connectToServer, usamos connectToDevice
        // que ya fue llamado desde BluetoothSelectionScreen
        if (bluetoothManager?.isConnected() == true) {
            callback(true)
        } else {
            callback(false)
        }
    }

    /**
     * Procesa un mensaje de red recibido
     */
    private fun handleNetworkMessage(message: NetworkMessage) {
        when (message.type) {
            MessageType.MOVE -> {
                // Solo procesar movimientos del oponente
                if (message.playerId != localPlayerId && message.direction != null) {
                    processMoveFromNetwork(message.playerId, message.direction)
                }
            }

            MessageType.RESET_GAME -> {
                resetGame()
            }

            MessageType.PAUSE -> {
                pauseGame()
            }

            MessageType.RESUME -> {
                resumeGame()
            }

            else -> {}
        }
    }

    /**
     * Procesa un movimiento recibido por red
     */
    private fun processMoveFromNetwork(playerId: Int, direction: Direction) {
        if (gameState.status != GameStatus.PLAYING) return

        // Validar que sea el turno correcto
        val expectedPlayer = if (playerId == 1) PlayerTurn.PLAYER1 else PlayerTurn.PLAYER2
        if (gameState.currentTurn != expectedPlayer) {
            println("⚠️ Movimiento fuera de turno recibido")
            return
        }

        // Validar movimiento
        val currentPos = if (playerId == 1) gameState.player1Position else gameState.player2Position
        val currentDir = if (playerId == 1) gameState.player1Direction else gameState.player2Direction

        if (direction == currentDir.opposite()) {
            println("⚠️ Movimiento inválido recibido (dirección opuesta)")
            return
        }

        // Ejecutar movimiento localmente
        executeMoveLocally(playerId, direction)
    }

    /**
     * Maneja cambios en la conexión
     */
    private fun handleConnectionChange(connected: Boolean) {
        onConnectionChanged?.invoke(connected)

        if (!connected && isMultiplayer) {
            // Pausar juego si se pierde conexión
            pauseGame()
        }
    }

    /**
     * Realiza un movimiento del jugador local
     */
    fun makeMove(direction: Direction) {
        if (gameState.status != GameStatus.PLAYING) return

        val currentPlayer = if (gameState.currentTurn == PlayerTurn.PLAYER1) 1 else 2

        // En multiplayer, solo permitir mover al jugador local
        if (isMultiplayer && currentPlayer != localPlayerId) {
            println("⚠️ No es tu turno")
            return
        }

        // Obtener posición y dirección actual
        val currentPos = if (currentPlayer == 1) {
            gameState.player1Position
        } else {
            gameState.player2Position
        }

        val currentDir = if (currentPlayer == 1) {
            gameState.player1Direction
        } else {
            gameState.player2Direction
        }

        // Validar que no se mueva en dirección opuesta
        if (direction == currentDir.opposite()) {
            println("⚠️ No puedes moverte en dirección opuesta")
            return
        }

        // Enviar movimiento por Bluetooth si es multiplayer
        if (isMultiplayer && bluetoothManager?.isConnected() == true) {
            bluetoothManager.sendMove(direction, localPlayerId)
        }

        // Ejecutar movimiento localmente
        executeMoveLocally(currentPlayer, direction)
    }

    /**
     * Ejecuta un movimiento en el estado local del juego
     */
    private fun executeMoveLocally(playerId: Int, direction: Direction) {
        // Calcular nueva posición
        val currentPos = if (playerId == 1) {
            gameState.player1Position
        } else {
            gameState.player2Position
        }

        val newPos = when (direction) {
            Direction.UP -> Vector2(currentPos.x, currentPos.y - 1)
            Direction.DOWN -> Vector2(currentPos.x, currentPos.y + 1)
            Direction.LEFT -> Vector2(currentPos.x - 1, currentPos.y)
            Direction.RIGHT -> Vector2(currentPos.x + 1, currentPos.y)
        }

        // Verificar colisiones
        val collision = checkCollisions(newPos, playerId)

        if (collision) {
            handleCollision(playerId)
            return
        }

        // Actualizar estado
        gameState = if (playerId == 1) {
            gameState.copy(
                player1Position = newPos,
                player1Direction = direction,
                player1Trail = gameState.player1Trail + newPos,
                currentTurn = PlayerTurn.PLAYER2
            )
        } else {
            gameState.copy(
                player2Position = newPos,
                player2Direction = direction,
                player2Trail = gameState.player2Trail + newPos,
                currentTurn = PlayerTurn.PLAYER1
            )
        }

        onStateChanged?.invoke(gameState)
    }

    /**
     * Verifica colisiones
     */
    private fun checkCollisions(position: Vector2, playerId: Int): Boolean {
        val gridSize = 20

        // Colisión con paredes
        if (position.x < 0 || position.x >= gridSize ||
            position.y < 0 || position.y >= gridSize) {
            return true
        }

        // Colisión con rastro del jugador 1
        if (gameState.player1Trail.any {
                abs(it.x - position.x) < 0.1f && abs(it.y - position.y) < 0.1f
            }) {
            return true
        }

        // Colisión con rastro del jugador 2
        if (gameState.player2Trail.any {
                abs(it.x - position.x) < 0.1f && abs(it.y - position.y) < 0.1f
            }) {
            return true
        }

        return false
    }

    /**
     * Maneja una colisión
     */
    private fun handleCollision(playerId: Int) {
        val winner = if (playerId == 1) "Jugador 2" else "Jugador 1"

        gameState = gameState.copy(
            status = GameStatus.FINISHED,
            winner = winner,
            player1Score = if (playerId == 2) gameState.player1Score + 1 else gameState.player1Score,
            player2Score = if (playerId == 1) gameState.player2Score + 1 else gameState.player2Score
        )

        onStateChanged?.invoke(gameState)
        onGameOver?.invoke(winner)
    }

    /**
     * Pausa el juego
     */
    fun pauseGame() {
        if (gameState.status == GameStatus.PLAYING) {
            gameState = gameState.copy(status = GameStatus.PAUSED)

            // Enviar pausa por Bluetooth (solo el host)
            if (isMultiplayer && bluetoothManager?.isHost == true) {
                bluetoothManager.sendPause()
            }

            onStateChanged?.invoke(gameState)
        }
    }

    /**
     * Reanuda el juego
     */
    fun resumeGame() {
        if (gameState.status == GameStatus.PAUSED) {
            gameState = gameState.copy(status = GameStatus.PLAYING)

            // Enviar resume por Bluetooth (solo el host)
            if (isMultiplayer && bluetoothManager?.isHost == true) {
                bluetoothManager.sendResume()
            }

            onStateChanged?.invoke(gameState)
        }
    }

    /**
     * Reinicia el juego
     */
    fun resetGame() {
        val gridSize = 20

        gameState = GameState(
            player1Position = Vector2(5f, gridSize / 2f),
            player2Position = Vector2((gridSize - 5).toFloat(), gridSize / 2f),
            player1Direction = Direction.RIGHT,
            player2Direction = Direction.LEFT,
            player1Trail = mutableListOf(),
            player2Trail = mutableListOf(),
            currentTurn = PlayerTurn.PLAYER1,
            status = GameStatus.PLAYING,
            player1Score = gameState.player1Score,
            player2Score = gameState.player2Score,
            winner = null
        )

        // Enviar reset por Bluetooth (solo el host)
        if (isMultiplayer && bluetoothManager?.isHost == true) {
            bluetoothManager.sendResetGame()
        }

        onStateChanged?.invoke(gameState)
    }

    /**
     * Limpia recursos
     */
    fun dispose() {
        bluetoothManager?.disconnect()
    }
}
