package com.tron3d.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.tron3d.config.TronVisualConfig
import com.tron3d.models.Direction
import com.tron3d.models.GameStatus
import com.tron3d.models.LightCycle
import com.tron3d.models.PlayerTurn
import com.tron3d.models.ArenaModel
import com.tron3d.network.BluetoothInterface
import com.tron3d.network.BluetoothProtocol
import com.tron3d.rendering.TronRenderer
import com.tron3d.ui.GameHUD
import com.tron3d.viewmodel.GameViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * GameScreen 3D - CON ARENA 3D Y GESTOS DE ZOOM/PAN
 * AMBAS MOTOS VISIBLES Y FUNCIONALES
 */
class GameScreen(
    private val game: Tron3DGame,
    private val gameViewModel: GameViewModel,
    private val isBluetooth: Boolean = false,
    private val isHost: Boolean = false,
    private val bluetoothManager: BluetoothInterface? = null
) : Screen {

    private lateinit var camera: PerspectiveCamera
    private lateinit var renderer: TronRenderer

    // ✅ AMBAS MOTOS DEBEN SER VISIBLES
    private lateinit var player1Cycle: LightCycle
    private lateinit var player2Cycle: LightCycle

    // ✅ ARENA 3D para TODOS los modos
    private var arenaModel: ArenaModel? = null

    private val spriteBatch: SpriteBatch = SpriteBatch()
    private val font: BitmapFont = BitmapFont()
    private val gameHUD: GameHUD
    private val shapeRenderer: ShapeRenderer = ShapeRenderer()

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    // Variables para gestos táctiles
    private var firstFinger = Vector2()
    private var secondFinger = Vector2()
    private var isPinching = false
    private var initialPinchDistance = 0f
    private var lastSingleTouch = Vector2()
    private var isDragging = false

    // Variables de cámara
    private var cameraHeight = 150f
    private var cameraDistance = 150f
    private val cameraZoomSpeed = 0.1f
    private val cameraPanSpeed = 0.3f
    private val cameraRotateSpeed = 0.5f
    private val minCameraHeight = 15f
    private val maxCameraHeight = 80f
    private val minCameraDistance = 10f
    private val maxCameraDistance = 50f

    // Variables de joystick
    private val joystickRadius = 140f
    private val joystickInnerRadius = 60f
    private var joystickCenter = Vector2()
    private var joystickPosition = Vector2()
    private var joystickTouched = false
    private var joystickPointer = -1

    private val tronCyan = TronVisualConfig.NeonColors.CYAN
    private val tronOrange = TronVisualConfig.NeonColors.ORANGE

    private val gridWidth = 50f
    private val gridHeight = 30f

    private var controlledPlayer: PlayerTurn = if (isHost) PlayerTurn.PLAYER1 else PlayerTurn.PLAYER2

    init {
        font.color = tronCyan
        font.data.setScale(1.8f)
        gameHUD = GameHUD(spriteBatch, font)
        updateJoystickPosition()

        // Inicializar vectores para gestos
        lastSingleTouch = Vector2()
        firstFinger = Vector2()
        secondFinger = Vector2()
    }

    override fun show() {
        // Configurar cámara
        setupCamera()

        // ✅ INICIALIZAR AMBAS MOTOS
        initializeCycles()

        // Cargar arena
        loadArena()

        // Configurar juego
        gameViewModel.startNewGame()
        observeGameState()

        // Configurar Bluetooth si es necesario
        setupBluetoothListener()

        val mode = if (isBluetooth) {
            if (isHost) "BLUETOOTH HOST (CYAN)" else "BLUETOOTH CLIENT (ORANGE)"
        } else {
            "LOCAL"
        }
        Gdx.app.log("GameScreen", "🎮 Modo: $mode")
        Gdx.app.log("GameScreen", "🚀 Jugador 1: ${player1Cycle.position}")
        Gdx.app.log("GameScreen", "🚀 Jugador 2: ${player2Cycle.position}")
    }

    /**
     * ✅ Configurar cámara
     */
    private fun setupCamera() {
        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.position.set(gridWidth / 2f, cameraHeight, gridHeight / 2f + cameraDistance)
        camera.lookAt(gridWidth / 2f, 0f, gridHeight / 2f)
        camera.near = 1f
        camera.far = 300f
        camera.update()

        renderer = TronRenderer(camera)
    }

    /**
     * ✅ Inicializar ambas motos con posiciones diferentes
     */
    private fun initializeCycles() {
        // Posiciones iniciales opuestas en la arena
        val player1Start = Vector3(10f, 0f, 15f)
        val player2Start = Vector3(40f, 0f, 15f)

        // ✅ Usando solo los parámetros que existen en LightCycle
        player1Cycle = LightCycle(
            colorNeon = tronCyan,
            initialPosition = player1Start,
            playerId = 1,
            modelPath = "models/uploads_files_3392844_tron.g3db"
        )

        player2Cycle = LightCycle(
            colorNeon = tronOrange,
            initialPosition = player2Start,
            playerId = 2,
            modelPath = "models/uploads_files_3392844_tron.g3db"
        )

        Gdx.app.log("GameScreen", "✅ Motos inicializadas:")
        Gdx.app.log("GameScreen", "  P1 en (${player1Start.x}, ${player1Start.z})")
        Gdx.app.log("GameScreen", "  P2 en (${player2Start.x}, ${player2Start.z})")
    }

    /**
     * ✅ Cargar arena
     */
    private fun loadArena() {
        arenaModel = ArenaModel()
        val arenaLoaded = arenaModel?.load() ?: false

        if (arenaLoaded) {
            Gdx.app.log("GameScreen", "✅ Arena 3D cargada")
        } else {
            Gdx.app.log("GameScreen", "⚠️ Arena no disponible, usando grid fallback")
        }
    }

    /**
     * ✅ Observar cambios en el estado del juego
     */
    private fun observeGameState() {
        coroutineScope.launch {
            gameViewModel.gameState.collect { state ->
                // ✅ ACTUALIZAR AMBAS MOTOS - usando solo las propiedades que existen
                player1Cycle.position.set(state.player1Position.x, 0f, state.player1Position.y)
                player1Cycle.rotation = state.player1Direction.getRotationAngle()

                player2Cycle.position.set(state.player2Position.x, 0f, state.player2Position.y)
                player2Cycle.rotation = state.player2Direction.getRotationAngle()

                // Log de depuración (simplificado - sin LOG_DEBUG)
                Gdx.app.log("GameScreen-DEBUG", "Estado actualizado: P1(${state.player1Position.x.toInt()},${state.player1Position.y.toInt()}) P2(${state.player2Position.x.toInt()},${state.player2Position.y.toInt()})")
            }
        }
    }

    /**
     * ✅ Configurar listener Bluetooth
     */
    private fun setupBluetoothListener() {
        if (isBluetooth && bluetoothManager != null) {
            bluetoothManager.setOnMessageReceived { message ->
                Gdx.app.postRunnable {
                    handleBluetoothMessage(message)
                }
            }
            Gdx.app.log("GameScreen", "✅ Listener Bluetooth configurado")
        }
    }

    /**
     * ✅ Manejar mensajes Bluetooth
     */
    private fun handleBluetoothMessage(message: String) {
        val moveData = BluetoothProtocol.parsePlayerMoveMessage(message) ?: return

        if (isHost && moveData.playerNumber == 2) {
            gameViewModel.updatePlayer2FromNetwork(moveData.position, moveData.direction, moveData.trail)
            Gdx.app.log("GameScreen", "📥 Host recibió: P2 en (${moveData.position.x.toInt()}, ${moveData.position.y.toInt()})")
        } else if (!isHost && moveData.playerNumber == 1) {
            gameViewModel.updatePlayer1FromNetwork(moveData.position, moveData.direction, moveData.trail)
            Gdx.app.log("GameScreen", "📥 Cliente recibió: P1 en (${moveData.position.x.toInt()}, ${moveData.position.y.toInt()})")
        }
    }

    /**
     * ✅ Enviar estado del jugador por Bluetooth
     */
    private fun sendPlayerState() {
        if (!isBluetooth || bluetoothManager == null) return

        val state = gameViewModel.gameState.value
        val message = if (isHost) {
            BluetoothProtocol.createPlayerMoveMessage(1, state.player1Position, state.player1Direction, state.player1Trail)
        } else {
            BluetoothProtocol.createPlayerMoveMessage(2, state.player2Position, state.player2Direction, state.player2Trail)
        }

        bluetoothManager.sendMessage(message)
        Gdx.app.log("GameScreen", "📤 Estado enviado: ${if (isHost) "P1" else "P2"}")
    }

    /**
     * Manejar gestos táctiles para cámara
     */
    private fun handleCameraGestures() {
        // Detectar gesto de pinzado (dos dedos)
        if (Gdx.input.isTouched(0) && Gdx.input.isTouched(1)) {
            handlePinchZoom()
        }
        // Detectar gesto de arrastre con un dedo (fuera del joystick)
        else if (Gdx.input.isTouched(0) && !isJoystickAreaTouched()) {
            handleDragPan()
        }

        // Resetear estados cuando no hay toques
        if (!Gdx.input.isTouched(0) && !Gdx.input.isTouched(1)) {
            isPinching = false
            isDragging = false
        }
    }

    private fun handlePinchZoom() {
        val currentFinger1 = Vector2(Gdx.input.getX(0).toFloat(), Gdx.input.getY(0).toFloat())
        val currentFinger2 = Vector2(Gdx.input.getX(1).toFloat(), Gdx.input.getY(1).toFloat())

        if (!isPinching) {
            // Inicio del gesto de pinzado
            firstFinger.set(currentFinger1)
            secondFinger.set(currentFinger2)
            initialPinchDistance = firstFinger.dst(secondFinger)
            isPinching = true
            isDragging = false
        } else {
            // Calcular cambio de distancia
            val currentDistance = currentFinger1.dst(currentFinger2)
            val zoomFactor = initialPinchDistance / currentDistance

            // Aplicar zoom cambiando altura y distancia de cámara
            cameraHeight *= zoomFactor
            cameraDistance *= zoomFactor

            // Limitar valores
            cameraHeight = cameraHeight.coerceIn(minCameraHeight, maxCameraHeight)
            cameraDistance = cameraDistance.coerceIn(minCameraDistance, maxCameraDistance)

            // Actualizar cámara
            updateCameraPosition()

            // Actualizar distancia inicial para siguiente frame
            initialPinchDistance = currentDistance
        }
    }

    private fun handleDragPan() {
        val currentTouch = Vector2(Gdx.input.getX(0).toFloat(), Gdx.input.getY(0).toFloat())

        if (!isDragging) {
            // Inicio del arrastre
            lastSingleTouch.set(currentTouch)
            isDragging = true
            isPinching = false
        } else {
            // Calcular delta de movimiento
            val deltaX = currentTouch.x - lastSingleTouch.x
            val deltaY = currentTouch.y - lastSingleTouch.y

            // Rotar cámara alrededor del centro de la arena
            rotateCameraAroundCenter(deltaX, deltaY)

            // Actualizar última posición
            lastSingleTouch.set(currentTouch)
        }
    }

    private fun rotateCameraAroundCenter(deltaX: Float, deltaY: Float) {
        val center = Vector3(gridWidth / 2f, 0f, gridHeight / 2f)

        // Calcular dirección actual de la cámara al centro
        val direction = Vector3(camera.position).sub(center)

        // Convertir a coordenadas esféricas
        val radius = direction.len()
        var theta = atan2(direction.z, direction.x).toFloat() // Ángulo en plano XZ
        var phi = atan2(sqrt(direction.x * direction.x + direction.z * direction.z), direction.y).toFloat() // Ángulo vertical

        // Aplicar rotación
        theta -= deltaX * cameraRotateSpeed * 0.01f
        phi -= deltaY * cameraRotateSpeed * 0.01f

        // Limitar ángulo vertical
        phi = phi.coerceIn(0.1f, 2.5f) // Evitar que pase por encima o debajo

        // Convertir de vuelta a coordenadas cartesianas
        val newX = center.x + radius * sin(phi) * cos(theta)
        val newY = center.y + radius * cos(phi)
        val newZ = center.z + radius * sin(phi) * sin(theta)

        // Actualizar posición de la cámara
        camera.position.set(newX, newY, newZ)
        camera.lookAt(center)
        camera.update()
    }

    private fun updateCameraPosition() {
        val center = Vector3(gridWidth / 2f, 0f, gridHeight / 2f)

        // Calcular nueva posición manteniendo el lookAt al centro
        val direction = Vector3(camera.position).sub(center).nor()
        val newPosition = center.cpy().add(
            direction.x * cameraDistance,
            cameraHeight,
            direction.z * cameraDistance
        )

        camera.position.set(newPosition)
        camera.lookAt(center)
        camera.update()
    }

    /**
     * Verificar si el toque está en el área del joystick
     */
    private fun isJoystickAreaTouched(): Boolean {
        val touchX = Gdx.input.getX().toFloat()
        val touchY = Gdx.graphics.height - Gdx.input.getY().toFloat()
        val distance = distance(touchX, touchY, joystickCenter.x, joystickCenter.y)
        return distance < joystickRadius * 1.5f // Área ligeramente mayor que el joystick
    }

    /**
     * Método para resetear la vista de cámara
     */
    private fun resetCameraView() {
        cameraHeight = 45f
        cameraDistance = 25f
        updateCameraPosition()
    }

    override fun render(delta: Float) {
        // ✅ Manejar gestos de cámara ANTES del input del juego
        handleCameraGestures()

        handleInput()

        // ✅ ACTUALIZAR AMBAS MOTOS
        player1Cycle.update(delta)
        player2Cycle.update(delta)

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        // ✅ RENDERIZAR AMBAS MOTOS Y LA ARENA
        renderer.render(listOf(player1Cycle, player2Cycle), arenaModel)

        gameHUD.render(gameViewModel)
        renderJoystickHexagonal()
        renderMenuButton()
        renderDebugInfo(gameViewModel.gameState.value)
        renderCameraControls()

        // ✅ LOGS DE DEBUG (OPCIONAL)
        if (frameCount % 60 == 0) { // Cada 60 frames (1 segundo aprox)
            Gdx.app.log("GameScreen-DEBUG", "=== DEBUG INFO ===")
            Gdx.app.log("GameScreen-DEBUG", "P1: Pos=${player1Cycle.position}, Rot=${player1Cycle.rotation}")
            Gdx.app.log("GameScreen-DEBUG", "P2: Pos=${player2Cycle.position}, Rot=${player2Cycle.rotation}")
        }
        frameCount++
    }

    private var frameCount = 0

    /**
     * Renderizar indicadores de controles de cámara
     */
    private fun renderCameraControls() {
        spriteBatch.begin()
        font.color = Color.LIGHT_GRAY
        font.data.setScale(1.0f)

        // Indicar controles
        val controlsText = "Gestos: 2 dedos = Zoom | 1 dedo = Girar | Botón = Reset"
        font.draw(spriteBatch, controlsText, 20f, Gdx.graphics.height - 60f)

        // Mostrar valores de cámara
        val cameraInfo = "Cámara: Altura=${cameraHeight.toInt()} Dist=${cameraDistance.toInt()}"
        font.draw(spriteBatch, cameraInfo, 20f, Gdx.graphics.height - 85f)

        spriteBatch.end()
    }

    private fun renderDebugInfo(state: com.tron3d.models.GameState) {
        spriteBatch.begin()
        font.color = Color.YELLOW
        font.data.setScale(1f)

        if (isBluetooth) {
            font.draw(spriteBatch, "BLUETOOTH: ${if (isHost) "HOST" else "CLIENT"}", 20f, 220f)
            font.draw(spriteBatch, "Control: ${if (isHost) "CYAN" else "ORANGE"}", 20f, 190f)
        }

        font.draw(spriteBatch, "P1: (${state.player1Position.x.toInt()}, ${state.player1Position.y.toInt()}) ${state.player1Direction}", 20f, 160f)
        font.draw(spriteBatch, "P2: (${state.player2Position.x.toInt()}, ${state.player2Position.y.toInt()}) ${state.player2Direction}", 20f, 130f)

        // ✅ Información básica de depuración
        font.color = Color.GREEN
        font.draw(spriteBatch, "Motos activas: P1 y P2", 20f, 100f)

        // Mostrar gesto activo
        font.color = Color.WHITE
        if (isPinching) {
            font.draw(spriteBatch, "ZOOM ACTIVO", 20f, 70f)
        } else if (isDragging) {
            font.draw(spriteBatch, "GIRO ACTIVO", 20f, 70f)
        }

        spriteBatch.end()
    }

    private fun renderMenuButton() {
        spriteBatch.begin()
        font.color = Color.WHITE
        font.data.setScale(1.5f)
        font.draw(spriteBatch, "MENU", 30f, Gdx.graphics.height - 30f)

        // Botón para resetear vista
        font.draw(spriteBatch, "RESET VISTA", Gdx.graphics.width - 200f, Gdx.graphics.height - 30f)

        if (gameViewModel.gameState.value.status.isGameOver()) {
            font.color = tronCyan
            font.data.setScale(2.5f)
            font.draw(spriteBatch, "TOCA PARA CONTINUAR", Gdx.graphics.width / 2f - 350f, Gdx.graphics.height / 2f)
        }
        spriteBatch.end()
    }

    private fun renderJoystickHexagonal() {
        if (gameViewModel.gameState.value.status != GameStatus.PLAYING) return

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.1f, 0.3f, 0.5f, 0.4f)
        drawHexagon(joystickCenter.x, joystickCenter.y, joystickRadius, true)
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glLineWidth(5f)
        shapeRenderer.color = if (controlledPlayer == PlayerTurn.PLAYER1) tronCyan else tronOrange
        drawHexagon(joystickCenter.x, joystickCenter.y, joystickRadius, false)
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        if (joystickTouched) {
            shapeRenderer.color = Color(0.3f, 0.9f, 1f, 0.5f)
            shapeRenderer.circle(joystickPosition.x, joystickPosition.y, joystickInnerRadius + 20f)
        }

        shapeRenderer.color = if (joystickTouched) {
            if (controlledPlayer == PlayerTurn.PLAYER1) tronCyan else tronOrange
        } else {
            Color(0.2f, 0.5f, 0.7f, 0.7f)
        }
        drawHexagon(joystickPosition.x, joystickPosition.y, joystickInnerRadius, true)
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glLineWidth(4f)
        shapeRenderer.color = Color.WHITE
        drawHexagon(joystickPosition.x, joystickPosition.y, joystickInnerRadius, false)
        shapeRenderer.end()

        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun drawHexagon(centerX: Float, centerY: Float, radius: Float, filled: Boolean) {
        val vertices = mutableListOf<Float>()
        for (i in 0..6) {
            val angle = Math.toRadians((60 * i - 30).toDouble())
            vertices.add(centerX + (radius * cos(angle)).toFloat())
            vertices.add(centerY + (radius * sin(angle)).toFloat())
        }

        if (filled) {
            for (i in 1 until vertices.size / 2 - 1) {
                shapeRenderer.triangle(
                    vertices[0], vertices[1],
                    vertices[i * 2], vertices[i * 2 + 1],
                    vertices[(i + 1) * 2], vertices[(i + 1) * 2 + 1]
                )
            }
        } else {
            for (i in 0 until vertices.size / 2 - 1) {
                shapeRenderer.line(
                    vertices[i * 2], vertices[i * 2 + 1],
                    vertices[(i + 1) * 2], vertices[(i + 1) * 2 + 1]
                )
            }
        }
    }

    private fun updateJoystickPosition() {
        joystickCenter.set(Gdx.graphics.width - 200f, 220f)
        joystickPosition.set(joystickCenter)
    }

    private fun handleInput() {
        val state = gameViewModel.gameState.value

        if (state.status.isGameOver()) {
            if (Gdx.input.justTouched()) {
                gameViewModel.restartRound()
            }
            return
        }

        if (state.status != GameStatus.PLAYING) return

        // Manejar clic en botón "RESET VISTA"
        if (Gdx.input.justTouched()) {
            val touchX = Gdx.input.getX().toFloat()
            val touchY = Gdx.graphics.height - Gdx.input.getY().toFloat()

            // Verificar si es clic en botón "RESET VISTA"
            if (touchX > Gdx.graphics.width - 250f && touchY > Gdx.graphics.height - 80f) {
                resetCameraView()
                return
            }

            // Verificar si es clic en botón "MENU"
            if (touchX < 150f && touchY > Gdx.graphics.height - 100f) {
                game.showMenu()
                return
            }
        }

        handleJoystickInput()
    }

    private fun handleJoystickInput() {
        var anyTouched = false
        var moveExecuted = false

        for (i in 0 until 5) {
            if (Gdx.input.isTouched(i) && !moveExecuted) {
                anyTouched = true
                val touchX = Gdx.input.getX(i).toFloat()
                val touchY = Gdx.graphics.height - Gdx.input.getY(i).toFloat()

                if (!joystickTouched && distance(touchX, touchY, joystickCenter.x, joystickCenter.y) < joystickRadius) {
                    joystickTouched = true
                    joystickPointer = i
                }

                if (joystickTouched && joystickPointer == i) {
                    updateJoystickStick(touchX, touchY)

                    val direction = getJoystickDirection()
                    if (direction != null) {
                        Gdx.app.log("GameScreen", "🎮 Movimiento: $direction para $controlledPlayer")

                        if (isBluetooth) {
                            // En modo Bluetooth, cada jugador controla su propia moto
                            gameViewModel.makeMoveForPlayer(direction, controlledPlayer)
                            sendPlayerState()
                        } else {
                            // En modo local, alternar turnos
                            gameViewModel.makeMove(direction)
                        }

                        moveExecuted = true

                        // Reset del joystick después del movimiento
                        joystickTouched = false
                        joystickPosition.set(joystickCenter)
                        joystickPointer = -1
                    }
                }
            }
        }

        if (!anyTouched && joystickTouched) {
            joystickTouched = false
            joystickPosition.set(joystickCenter)
            joystickPointer = -1
        }
    }

    private fun updateJoystickStick(touchX: Float, touchY: Float) {
        val deltaX = touchX - joystickCenter.x
        val deltaY = touchY - joystickCenter.y
        val dist = sqrt(deltaX * deltaX + deltaY * deltaY)

        if (dist < joystickRadius * 0.8f) {
            joystickPosition.set(touchX, touchY)
        } else {
            val angle = atan2(deltaY, deltaX)
            joystickPosition.set(
                joystickCenter.x + cos(angle) * joystickRadius * 0.7f,
                joystickCenter.y + sin(angle) * joystickRadius * 0.7f
            )
        }
    }

    private fun getJoystickDirection(): Direction? {
        val deltaX = joystickPosition.x - joystickCenter.x
        val deltaY = joystickPosition.y - joystickCenter.y
        val dist = sqrt(deltaX * deltaX + deltaY * deltaY)

        if (dist < 40f) return null

        return if (kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY)) {
            if (deltaX > 0) Direction.RIGHT else Direction.LEFT
        } else {
            if (deltaY > 0) Direction.UP else Direction.DOWN
        }
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
        gameHUD.resize(width, height)
        renderer.resize(width, height)
        updateJoystickPosition()
    }

    override fun pause() {
        gameViewModel.pauseGame()
    }

    override fun resume() {
        gameViewModel.resumeGame()
    }

    override fun hide() {}

    override fun dispose() {
        renderer.dispose()
        player1Cycle.dispose()
        player2Cycle.dispose()
        arenaModel?.dispose()
        spriteBatch.dispose()
        font.dispose()
        shapeRenderer.dispose()
        gameHUD.dispose()
    }
}
