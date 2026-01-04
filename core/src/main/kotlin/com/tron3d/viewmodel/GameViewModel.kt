package com.tron3d.viewmodel

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import com.tron3d.models.ArenaCollider
import com.tron3d.models.Direction
import com.tron3d.models.GameState
import com.tron3d.models.GameStatus
import com.tron3d.models.PlayerTurn
import com.tron3d.network.BluetoothInterface
import com.tron3d.network.BluetoothProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel que maneja la lógica del juego TRON
 * Controla: movimientos, colisiones, puntuación, turnos
 * CON SOPORTE BLUETOOTH Y COLISIONES CON SEGMENTOS
 */
class GameViewModel {

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val moveSpeed = 1f // Velocidad de movimiento (unidades por turno)

    // ✅ Sistema de colisión de la arena
    private var arenaCollider: ArenaCollider? = null

    // ✅ Propiedades para Bluetooth
    private var _bluetoothManager: BluetoothInterface? = null
    private var _isHost: Boolean = false

    // ✅ LÍMITES REALES BASADOS EN COORDENADAS MEDIDAS
    companion object {
        // X min: -19.55, X max: 69.93
        // Z min: -14.03, Z max: 24.76
        const val REAL_MIN_X = -20f      // Redondeado hacia abajo desde -19.55
        const val REAL_MAX_X = 70f       // Redondeado hacia arriba desde 69.93
        const val REAL_MIN_Z = -15f      // Redondeado hacia abajo desde -14.03
        const val REAL_MAX_Z = 25f       // Redondeado hacia arriba desde 24.76
        const val REAL_CENTER_X = 25f    // Calculado: (70 + (-20)) / 2 = 25
        const val REAL_CENTER_Z = 5f     // Calculado: (25 + (-15)) / 2 = 5

        fun isPositionRealValid(x: Float, z: Float): Boolean {
            return x in REAL_MIN_X..REAL_MAX_X &&
                z in REAL_MIN_Z..REAL_MAX_Z
        }

        fun getRealStartPosition(player: PlayerTurn): Vector2 {
            return when (player) {
                PlayerTurn.PLAYER1 -> Vector2(15f, REAL_CENTER_Z)   // Dentro de límites reales
                PlayerTurn.PLAYER2 -> Vector2(55f, REAL_CENTER_Z)   // Dentro de límites reales
            }
        }
    }

    /**
     * Configurar Bluetooth en el ViewModel
     */
    fun setupBluetooth(bluetoothManager: BluetoothInterface?, isHost: Boolean) {
        _bluetoothManager = bluetoothManager
        _isHost = isHost

        Gdx.app.log("GameViewModel", "🔧 Bluetooth configurado - Host: $isHost")
    }

    /**
     * Inicializa el juego con la arena real
     */
    fun initializeWithArena(
        collider: ArenaCollider,
        player1Start: Vector2,
        player2Start: Vector2
    ) {
        arenaCollider = collider

        // ✅ Usar límites REALES para el grid
        val width = (REAL_MAX_X - REAL_MIN_X).toInt()
        val depth = (REAL_MAX_Z - REAL_MIN_Z).toInt()

        // ✅ Verificar que las posiciones iniciales estén dentro de límites REALES
        val safeP1Start = if (isPositionRealValid(player1Start.x, player1Start.y)) {
            player1Start
        } else {
            Gdx.app.error("GameViewModel", "⚠️ P1 fuera de límites reales, ajustando")
            getRealStartPosition(PlayerTurn.PLAYER1)
        }

        val safeP2Start = if (isPositionRealValid(player2Start.x, player2Start.y)) {
            player2Start
        } else {
            Gdx.app.error("GameViewModel", "⚠️ P2 fuera de límites reales, ajustando")
            getRealStartPosition(PlayerTurn.PLAYER2)
        }

        _gameState.value = GameState(
            player1Position = safeP1Start,
            player2Position = safeP2Start,
            player1Direction = Direction.RIGHT,
            player2Direction = Direction.LEFT,
            player1Trail = listOf(safeP1Start),
            player2Trail = listOf(safeP2Start),
            status = GameStatus.PLAYING,
            currentTurn = PlayerTurn.PLAYER1,
            player1Score = 0,
            player2Score = 0,
            currentRound = 1,
            gridWidth = width,
            gridHeight = depth
        )

        Gdx.app.log("GameViewModel", "✅ Juego inicializado con límites REALES: ${width}x${depth}")
        Gdx.app.log("GameViewModel", "📏 Límites REALES: X[$REAL_MIN_X-$REAL_MAX_X] Z[$REAL_MIN_Z-$REAL_MAX_Z]")
        Gdx.app.log("GameViewModel", "🏍️ P1: $safeP1Start")
        Gdx.app.log("GameViewModel", "🏍️ P2: $safeP2Start")

        // ✅ Mostrar información del collider
        val bounds = collider.getBounds()
        Gdx.app.log("GameViewModel", "🎯 ArenaCollider configurado")
        Gdx.app.log("GameViewModel", "   Bounds: X[${bounds["minX"]}-${bounds["maxX"]}] Z[${bounds["minZ"]}-${bounds["maxZ"]}]")
        Gdx.app.log("GameViewModel", "   Centro: (${bounds["centerX"]}, ${bounds["centerZ"]})")
    }

    /**
     * Inicia un nuevo juego con límites REALES
     */
    fun startNewGame() {
        val p1Pos = getRealStartPosition(PlayerTurn.PLAYER1)
        val p2Pos = getRealStartPosition(PlayerTurn.PLAYER2)

        val width = (REAL_MAX_X - REAL_MIN_X).toInt()
        val depth = (REAL_MAX_Z - REAL_MIN_Z).toInt()

        _gameState.value = GameState(
            player1Position = p1Pos,
            player2Position = p2Pos,
            player1Direction = Direction.RIGHT,
            player2Direction = Direction.LEFT,
            player1Trail = listOf(p1Pos),
            player2Trail = listOf(p2Pos),
            status = GameStatus.PLAYING,
            currentTurn = PlayerTurn.PLAYER1,
            player1Score = 0,
            player2Score = 0,
            currentRound = 1,
            gridWidth = width,
            gridHeight = depth
        )

        Gdx.app.log("GameViewModel", "🎮 Nuevo juego con límites REALES")
        Gdx.app.log("GameViewModel", "📏 Arena real: ${width}x${depth}")
        Gdx.app.log("GameViewModel", "📍 P1: $p1Pos, P2: $p2Pos")

        if (arenaCollider != null) {
            val bounds = arenaCollider!!.getBounds()
            Gdx.app.log("GameViewModel", "🎯 ArenaCollider disponible con ${bounds["width"]}x${bounds["depth"]}")
        }
    }

    /**
     * ✅ Limpia todos los rastros de la partida anterior
     */
    fun clearTrails() {
        val currentState = _gameState.value

        // Crear una nueva lista con solo la posición actual (punto de inicio)
        val newPlayer1Trail = listOf(currentState.player1Position.cpy())
        val newPlayer2Trail = listOf(currentState.player2Position.cpy())

        _gameState.value = currentState.copy(
            player1Trail = newPlayer1Trail,
            player2Trail = newPlayer2Trail
        )

        Gdx.app.log("GameViewModel", "🧹 Rastros limpiados en ViewModel")
    }

    /**
     * Realiza un movimiento en la dirección especificada (con turnos)
     * Usado en modo local
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

    /**
     * Hace un movimiento para un jugador específico (SIN sistema de turnos)
     * Usado en modo Bluetooth donde cada dispositivo controla su propia moto
     */
    fun makeMoveForPlayer(direction: Direction, player: PlayerTurn) {
        if (_gameState.value.status != GameStatus.PLAYING) return

        val currentState = _gameState.value

        when (player) {
            PlayerTurn.PLAYER1 -> {
                // Validar que no sea dirección opuesta
                if (direction.isOpposite(currentState.player1Direction)) {
                    return
                }

                // Actualizar player 1
                updatePlayer1(direction)
            }

            PlayerTurn.PLAYER2 -> {
                // Validar que no sea dirección opuesta
                if (direction.isOpposite(currentState.player2Direction)) {
                    return
                }

                // Actualizar player 2
                updatePlayer2(direction)
            }
        }
    }

    private fun updatePlayer1(newDirection: Direction) {
        val state = _gameState.value
        val movement = newDirection.toVector()
        val newPosition = Vector2(
            state.player1Position.x + movement.first * moveSpeed,
            state.player1Position.y + movement.second * moveSpeed
        )

        Gdx.app.log("Movement", "P1 desde (${state.player1Position.x}, ${state.player1Position.y}) → (${newPosition.x}, ${newPosition.y})")
        Gdx.app.log("Movement-DETAIL", "P1 dirección: $newDirection, movimiento: $movement")

        // Verificar colisión
        if (checkCollision(newPosition, state)) {
            Gdx.app.error("Collision", "🚫🚫 P1 PERDIÓ por colisión en: (${newPosition.x}, ${newPosition.y})")
            endRound(PlayerTurn.PLAYER2) // Player 1 perdió
            return
        }

        // ✅ VERIFICACIÓN FINAL: Asegurar que la posición sea válida
        if (!isPositionRealValid(newPosition.x, newPosition.y)) {
            Gdx.app.error("Collision", "❌ P1 FUERA DE LÍMITES REALES!")
            Gdx.app.error("Collision", "   Posición: (${newPosition.x}, ${newPosition.y})")
            endRound(PlayerTurn.PLAYER2)
            return
        }

        // Actualizar estado
        val newTrail = state.player1Trail + newPosition
        _gameState.value = state.copy(
            player1Position = newPosition,
            player1Direction = newDirection,
            player1Trail = newTrail
        )

        Gdx.app.log("Movement", "✅ P1 movido a: (${newPosition.x}, ${newPosition.y})")
    }

    private fun updatePlayer2(newDirection: Direction) {
        val state = _gameState.value
        val movement = newDirection.toVector()
        val newPosition = Vector2(
            state.player2Position.x + movement.first * moveSpeed,
            state.player2Position.y + movement.second * moveSpeed
        )

        Gdx.app.log("Movement", "P2 desde (${state.player2Position.x}, ${state.player2Position.y}) → (${newPosition.x}, ${newPosition.y})")
        Gdx.app.log("Movement-DETAIL", "P2 dirección: $newDirection, movimiento: $movement")

        // Verificar colisión
        if (checkCollision(newPosition, state)) {
            Gdx.app.error("Collision", "🚫🚫 P2 PERDIÓ por colisión en: (${newPosition.x}, ${newPosition.y})")
            endRound(PlayerTurn.PLAYER1) // Player 2 perdió
            return
        }

        // ✅ VERIFICACIÓN FINAL: Asegurar que la posición sea válida
        if (!isPositionRealValid(newPosition.x, newPosition.y)) {
            Gdx.app.error("Collision", "❌ P2 FUERA DE LÍMITES REALES!")
            Gdx.app.error("Collision", "   Posición: (${newPosition.x}, ${newPosition.y})")
            endRound(PlayerTurn.PLAYER1)
            return
        }

        // Actualizar estado
        val newTrail = state.player2Trail + newPosition
        _gameState.value = state.copy(
            player2Position = newPosition,
            player2Direction = newDirection,
            player2Trail = newTrail
        )

        Gdx.app.log("Movement", "✅ P2 movido a: (${newPosition.x}, ${newPosition.y})")
    }

    /**
     * Verifica si una posición causa colisión
     * ✅ VERIFICACIÓN CON SEGMENTOS DE ARENA + TRAILS
     */
    private fun checkCollision(position: Vector2, state: GameState): Boolean {
        val x = position.x
        val z = position.y

        // ✅ 1. PRIMERO verificar con el ArenaCollider (segmentos)
        if (arenaCollider != null) {
            Gdx.app.log("Collision-DEBUG", "🔍 Verificando: (${"%.2f".format(x)}, ${"%.2f".format(z)})")

            val isOutOfBounds = arenaCollider!!.isOutOfBounds(position)

            if (isOutOfBounds) {
                Gdx.app.error("Collision", "🚫 COLISIÓN CON ARENA en: (${"%.2f".format(x)}, ${"%.2f".format(z)})")

                // Debug adicional: mostrar punto más cercano válido
                val closest = arenaCollider!!.getClosestValidPoint(position)
                Gdx.app.error("Collision", "   Punto válido más cercano: (${"%.2f".format(closest.x)}, ${"%.2f".format(closest.y)})")

                return true
            } else {
                Gdx.app.log("Collision-DEBUG", "✅ Dentro de los límites de la arena")
            }
        } else {
            // ✅ 2. Fallback: verificación de bounding box
            Gdx.app.log("Collision-DEBUG", "⚠️ Sin ArenaCollider, usando fallback")

            if (x < GameViewModel.REAL_MIN_X || x > GameViewModel.REAL_MAX_X ||
                z < GameViewModel.REAL_MIN_Z || z > GameViewModel.REAL_MAX_Z) {
                Gdx.app.error("Collision", "❌ FUERA DE BOUNDING BOX!")
                Gdx.app.error("Collision", "   Posición: (${x}, ${z})")
                return true
            } else {
                Gdx.app.log("Collision-DEBUG", "✅ Dentro de bounding box")
            }
        }

        // ✅ 3. Colisión con trails (excluyendo la posición actual)
        if (state.isPositionOccupied(position)) {
            Gdx.app.error("Collision", "🚫 Colisión con trail en: (${x}, ${z})")
            return true
        } else {
            Gdx.app.log("Collision-DEBUG", "✅ Sin colisión con trails")
        }

        Gdx.app.log("Collision-DEBUG", "✅✅✅ MOVIMIENTO VÁLIDO")
        return false
    }

    /**
     * Enviar mensaje de game over por Bluetooth (solo el host)
     */
    private fun sendGameOverOverBluetooth(winner: PlayerTurn) {
        if (_bluetoothManager == null || !_isHost) return

        val state = _gameState.value
        val winnerCode = when (winner) {
            PlayerTurn.PLAYER1 -> 1
            PlayerTurn.PLAYER2 -> 2
        }

        val message = BluetoothProtocol.createGameOverMessageWithScore(
            winner = winnerCode,
            player1Score = state.player1Score,
            player2Score = state.player2Score,
            currentRound = state.currentRound
        )

        _bluetoothManager?.sendMessage(message)
        Gdx.app.log("GameViewModel", "🏆 Enviado game over por Bluetooth: P1=${state.player1Score}, P2=${state.player2Score}")
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

        Gdx.app.log("GameViewModel", "🏆 RONDA TERMINADA - Ganador: $winner")
        Gdx.app.log("GameViewModel", "📊 Puntuación: P1=$newPlayer1Score, P2=$newPlayer2Score")

        // ✅ ENVIAR POR BLUETOOTH SI ES HOST
        if (_bluetoothManager != null && _isHost) {
            sendGameOverOverBluetooth(winner)
        }
    }

    /**
     * Actualizar puntuación desde mensaje de game over
     */
    fun updateScoreFromNetwork(gameOverData: com.tron3d.network.GameOverData) {  // <- CAMBIADO
        _gameState.value = _gameState.value.copy(
            player1Score = gameOverData.player1Score,
            player2Score = gameOverData.player2Score,
            currentRound = gameOverData.currentRound,
            status = when (gameOverData.winner) {
                1 -> GameStatus.PLAYER1_WON
                2 -> GameStatus.PLAYER2_WON
                else -> GameStatus.DRAW
            }
        )

        Gdx.app.log("GameViewModel", "📥 Puntuación actualizada desde red: P1=${gameOverData.player1Score}, P2=${gameOverData.player2Score}")
    }

    /**
     * Reinicia el round (mantiene puntuación) y limpia los rastros
     * ✅ USA POSICIONES REALES SIEMPRE
     */
    fun restartRound() {
        val state = _gameState.value

        // ✅ Usar posiciones REALES SIEMPRE
        val p1Pos = getRealStartPosition(PlayerTurn.PLAYER1)
        val p2Pos = getRealStartPosition(PlayerTurn.PLAYER2)

        _gameState.value = GameState(
            player1Position = p1Pos,
            player2Position = p2Pos,
            player1Direction = Direction.RIGHT,
            player2Direction = Direction.LEFT,
            player1Trail = listOf(p1Pos), // ✅ Lista nueva con solo la posición inicial
            player2Trail = listOf(p2Pos), // ✅ Lista nueva con solo la posición inicial
            status = GameStatus.PLAYING,
            currentTurn = PlayerTurn.PLAYER1,
            player1Score = state.player1Score,
            player2Score = state.player2Score,
            currentRound = state.currentRound + 1,
            isMultiplayer = state.isMultiplayer,
            gridWidth = state.gridWidth,
            gridHeight = state.gridHeight
        )
        Gdx.app.log("GameViewModel", "🔄 Reiniciando ronda ${state.currentRound + 1}")
        Gdx.app.log("GameViewModel", "🏍️ P1: $p1Pos, P2: $p2Pos")
        Gdx.app.log("GameViewModel", "🧹 Rastros limpios y posiciones REALES")
    }

    /**
     * Pausa el juego
     */
    fun pauseGame() {
        val state = _gameState.value
        if (state.status == GameStatus.PLAYING) {
            _gameState.value = state.copy(status = GameStatus.PAUSED)
            Gdx.app.log("GameViewModel", "⏸️ Juego pausado")
        }
    }

    /**
     * Resume el juego
     */
    fun resumeGame() {
        val state = _gameState.value
        if (state.status == GameStatus.PAUSED) {
            _gameState.value = state.copy(status = GameStatus.PLAYING)
            Gdx.app.log("GameViewModel", "▶️ Juego reanudado")
        }
    }

    /**
     * Actualiza posición del jugador 1 desde datos externos (Bluetooth)
     */
    fun updatePlayer1FromNetwork(position: Vector2, direction: Direction, trail: List<Vector2>) {
        val state = _gameState.value

        // ✅ Verificar que la posición recibida sea válida según límites REALES
        if (!isPositionRealValid(position.x, position.y)) {
            Gdx.app.error("GameViewModel", "❌ Posición P1 recibida por red fuera de límites REALES: $position")
            return
        }

        _gameState.value = state.copy(
            player1Position = position,
            player1Direction = direction,
            player1Trail = trail
        )

        Gdx.app.log("GameViewModel", "📥 P1 actualizado desde red: $position $direction")
    }

    /**
     * Actualiza posición del jugador 2 desde datos externos (Bluetooth)
     */
    fun updatePlayer2FromNetwork(position: Vector2, direction: Direction, trail: List<Vector2>) {
        val state = _gameState.value

        // ✅ Verificar que la posición recibida sea válida según límites REALES
        if (!isPositionRealValid(position.x, position.y)) {
            Gdx.app.error("GameViewModel", "❌ Posición P2 recibida por red fuera de límites REALES: $position")
            return
        }

        _gameState.value = state.copy(
            player2Position = position,
            player2Direction = direction,
            player2Trail = trail
        )

        Gdx.app.log("GameViewModel", "📥 P2 actualizado desde red: $position $direction")
    }

    /**
     * ✅ NUEVO: Verificar si el collider está activo
     */
    fun isColliderActive(): Boolean {
        return arenaCollider != null
    }

    /**
     * ✅ NUEVO: Obtener información del collider para debug
     */
    fun getColliderInfo(): String {
        return if (arenaCollider != null) {
            val bounds = arenaCollider!!.getBounds()
            "ArenaCollider activo - ${bounds["width"]}x${bounds["depth"]}"
        } else {
            "ArenaCollider NO configurado"
        }
    }
}
