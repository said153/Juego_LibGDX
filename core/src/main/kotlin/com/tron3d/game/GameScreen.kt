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
 * ZOOM CON DOS DEDOS IMPLEMENTADO
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
    private var isDragging = false
    private var initialPinchDistance = 0f
    private var previousPinchDistance = 0f
    private var lastSingleTouch = Vector2()

    // Variables de cámara
    private var cameraHeight = 20f
    private var cameraDistance = 20f
    private val cameraZoomSpeed = 0.1f
    private val cameraPanSpeed = 0.3f
    private val cameraRotateSpeed = 0.5f
    private val minCameraHeight = 15f
    private val maxCameraHeight = 80f
    private val minCameraDistance = 10f
    private val maxCameraDistance = 50f

    // ✅ NUEVAS CONSTANTES PARA LÍMITES DE ROTACIÓN
    private val minVerticalAngle = 0.3f  // Límite superior (evitar ver desde arriba)
    private val maxVerticalAngle = 1.5f  // Límite inferior (evitar ver desde abajo)

    // Variables de zoom
    private val pinchZoomSensitivity = 0.005f
    private val minZoomFactor = 0.5f
    private val maxZoomFactor = 3.0f
    private var currentZoomFactor = 1.0f

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
        setupCamera()

        // ✅ CARGAR ARENA PRIMERO
        loadArena()

        // ✅ LUEGO inicializar motos con posiciones correctas
        initializeCycles()

        // ✅ LIMPIAR RASTROS INMEDIATAMENTE AL INICIAR
        clearAllTrailsImmediately()

        gameViewModel.startNewGame()
        observeGameState()
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
     * ✅ Configurar cámara con posición inicial segura
     */
    private fun setupCamera() {
        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

        // ✅ POSICIÓN INICIAL SEGURA (evita ver el vacío)
        camera.position.set(
            gridWidth / 2f + 20f,  // Desplazada en X
            cameraHeight,
            gridHeight / 2f + cameraDistance
        )

        camera.lookAt(gridWidth / 2f, 0f, gridHeight / 2f)
        camera.near = 1f
        camera.far = 300f
        camera.update()

        renderer = TronRenderer(camera)

        // ✅ Asegurar que la cámara no esté mirando hacia abajo
        ensureCameraSafePosition()
    }

    /**
     * ✅ Inicializar ambas motos con posiciones diferentes
     */
    private fun initializeCycles() {
        // ✅ Obtener posiciones del collider de la arena
        val p1Start = arenaModel?.collider?.getStartPosition(0.2f) ?: Vector2(10f, 15f)
        val p2Start = arenaModel?.collider?.getStartPosition(0.8f) ?: Vector2(40f, 15f)

        // Convertir Vector2 a Vector3 (con Y=2 para que estén visibles sobre el piso)
        val player1Start = Vector3(p1Start.x, 2f, p1Start.y)
        val player2Start = Vector3(p2Start.x, 2f, p2Start.y)

        Gdx.app.log("GameScreen", "🏍️ Posiciones calculadas:")
        Gdx.app.log("GameScreen", "  P1: $player1Start")
        Gdx.app.log("GameScreen", "  P2: $player2Start")

        // ✅ SI LAS MOTOS YA EXISTEN, LIMPIAR SUS RASTROS PRIMERO
        if (this::player1Cycle.isInitialized) {
            player1Cycle.clearTrail()
        }
        if (this::player2Cycle.isInitialized) {
            player2Cycle.dispose()
        }

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

        // ✅ LIMPIAR RASTROS AL INICIALIZAR
        player1Cycle.clearTrail()
        player2Cycle.clearTrail()

        Gdx.app.log("GameScreen", "✅ Motos inicializadas con posiciones de la arena (rastros limpios)")
    }

    /**
     * ✅ Cargar arena
     */
    private fun loadArena() {
        arenaModel = ArenaModel()
        val arenaLoaded = arenaModel?.load() ?: false

        if (arenaLoaded) {
            // ✅ Configurar límites y posiciones basándose en la arena real
            arenaModel?.collider?.let { collider ->
                val width = collider.getPlayableWidth()
                val depth = collider.getPlayableDepth()
                val p1Pos = collider.getStartPosition(0.2f)  // 20% del ancho
                val p2Pos = collider.getStartPosition(0.8f)  // 80% del ancho

                Gdx.app.log("GameScreen", "🏟️ Arena: ${width}x${depth}")
                Gdx.app.log("GameScreen", "🏍️ P1: $p1Pos")
                Gdx.app.log("GameScreen", "🏍️ P2: $p2Pos")

                // Actualizar ViewModel con límites reales
                gameViewModel.initializeWithArena(
                    collider,
                    p1Pos,
                    p2Pos
                )
            }

            Gdx.app.log("GameScreen", "✅ Arena 3D cargada")
        } else {
            Gdx.app.log("GameScreen", "⚠️ Arena no disponible")
        }
    }

    /**
     * ✅ Observar cambios en el estado del juego
     */
    private fun observeGameState() {
        coroutineScope.launch {
            gameViewModel.gameState.collect { state ->
                player1Cycle.position.set(state.player1Position.x, 2f, state.player1Position.y)
                player1Cycle.rotation = state.player1Direction.getRotationAngle()

                player2Cycle.position.set(state.player2Position.x, 2f, state.player2Position.y)
                player2Cycle.rotation = state.player2Direction.getRotationAngle()

                // ✅ Los trails se manejan separadamente en LightCycle
            }
        }
    }

    /**
     * ✅ Limpiar todos los trails inmediatamente (ambos lugares)
     */
    private fun clearAllTrailsImmediately() {
        // ✅ Verificar que las motos estén inicializadas
        if (!this::player1Cycle.isInitialized || !this::player2Cycle.isInitialized) {
            Gdx.app.log("GameScreen", "⚠️ Motos no inicializadas, omitiendo limpieza")
            return
        }

        // ✅ Limpiar en las motos 3D
        player1Cycle.clearTrail()
        player2Cycle.clearTrail()

        // ✅ Limpiar en el ViewModel
        gameViewModel.clearTrails()

        Gdx.app.log("GameScreen", "🧹 TODOS los rastros han sido limpiados inmediatamente")
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
        val touchCount = getTouchCount()

        when (touchCount) {
            2 -> {
                handlePinchZoom()
                // Resetear arrastre cuando hay pinch
                isDragging = false
            }
            1 -> {
                // Solo manejar arrastre si no está en área del joystick
                val touchX = Gdx.input.getX(0).toFloat()
                val touchY = Gdx.graphics.height - Gdx.input.getY(0).toFloat()

                if (!isJoystickAreaTouched(touchX, touchY)) {
                    handleDragPan()
                }
            }
            else -> {
                // Resetear estados cuando no hay toques
                isPinching = false
                isDragging = false
            }
        }
    }

    /**
     * Manejar zoom con dos dedos (pellizco)
     */
    private fun handlePinchZoom() {
        val currentFinger1 = Vector2(Gdx.input.getX(0).toFloat(), Gdx.input.getY(0).toFloat())
        val currentFinger2 = Vector2(Gdx.input.getX(1).toFloat(), Gdx.input.getY(1).toFloat())

        val currentDistance = currentFinger1.dst(currentFinger2)

        if (!isPinching) {
            // Inicio del gesto de pinzado
            firstFinger.set(currentFinger1)
            secondFinger.set(currentFinger2)
            initialPinchDistance = currentDistance
            previousPinchDistance = currentDistance
            isPinching = true
            isDragging = false
        } else {
            // Calcular cambio de distancia
            val distanceDelta = currentDistance - previousPinchDistance

            // Aplicar zoom con sensibilidad ajustada
            val zoomDelta = distanceDelta * pinchZoomSensitivity
            currentZoomFactor -= zoomDelta

            // Limitar el factor de zoom
            currentZoomFactor = currentZoomFactor.coerceIn(minZoomFactor, maxZoomFactor)

            // Aplicar el zoom cambiando tanto la altura como la distancia
            applyZoomToCamera(currentZoomFactor)

            // Actualizar distancia para siguiente frame
            previousPinchDistance = currentDistance
        }

        // ✅ Verificar que la cámara esté en posición segura
        ensureCameraSafePosition()
    }

    /**
     * Aplicar zoom a la cámara
     */
    private fun applyZoomToCamera(zoomFactor: Float) {
        val center = Vector3(gridWidth / 2f, 0f, gridHeight / 2f)

        // Calcular nueva altura y distancia basadas en el factor de zoom
        cameraHeight = 20f * zoomFactor
        cameraDistance = 20f * zoomFactor

        // Limitar valores
        cameraHeight = cameraHeight.coerceIn(minCameraHeight, maxCameraHeight)
        cameraDistance = cameraDistance.coerceIn(minCameraDistance, maxCameraDistance)

        // Obtener dirección actual de la cámara
        val currentDirection = Vector3(camera.position).sub(center).nor()

        // Calcular nueva posición manteniendo el lookAt al centro
        val newPosition = center.cpy().add(
            currentDirection.x * cameraDistance,
            cameraHeight,
            currentDirection.z * cameraDistance
        )

        camera.position.set(newPosition)
        camera.lookAt(center)
        camera.update()
    }

    /**
     * Manejar arrastre con un dedo para rotar la cámara
     */
    private fun handleDragPan() {
        // Verificar que no haya dos dedos tocando (gesto de pinch)
        if (Gdx.input.isTouched(1)) {
            return
        }

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

            // Solo rotar si el movimiento es significativo (evita movimientos accidentales)
            if (Math.abs(deltaX) > 2 || Math.abs(deltaY) > 2) {
                // Rotar cámara alrededor del centro de la arena
                rotateCameraAroundCenter(deltaX, deltaY)
            }

            // Actualizar última posición
            lastSingleTouch.set(currentTouch)
        }
    }

    /**
     * Rotar cámara alrededor del centro de la arena
     */
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

        // ✅ LIMITAR ÁNGULO VERTICAL PARA EVITAR VER EL VACÍO
        phi = phi.coerceIn(minVerticalAngle, maxVerticalAngle)

        // Convertir de vuelta a coordenadas cartesianas
        val newX = center.x + radius * sin(phi) * cos(theta)
        val newY = center.y + radius * cos(phi)
        val newZ = center.z + radius * sin(phi) * sin(theta)

        // Actualizar posición de la cámara
        camera.position.set(newX, newY, newZ)
        camera.lookAt(center)
        camera.update()
    }

    /**
     * Contar toques activos en pantalla
     */
    private fun getTouchCount(): Int {
        var count = 0
        for (i in 0 until 5) {
            if (Gdx.input.isTouched(i)) {
                count++
            }
        }
        return count
    }

    /**
     * ✅ Asegurar que la cámara no pueda ver el vacío debajo de la arena
     */
    private fun ensureCameraSafePosition() {
        val center = Vector3(gridWidth / 2f, 0f, gridHeight / 2f)
        val direction = Vector3(camera.position).sub(center)

        // Calcular ángulo vertical actual
        val phi = atan2(sqrt(direction.x * direction.x + direction.z * direction.z), direction.y).toFloat()

        // Si el ángulo está fuera de límites, ajustarlo
        if (phi < minVerticalAngle || phi > maxVerticalAngle) {
            // Ajustar a un ángulo seguro
            val safePhi = phi.coerceIn(minVerticalAngle, maxVerticalAngle)

            // Recalcular posición
            val radius = direction.len()
            val theta = atan2(direction.z, direction.x).toFloat()

            val newX = center.x + radius * sin(safePhi) * cos(theta)
            val newY = center.y + radius * cos(safePhi)
            val newZ = center.z + radius * sin(safePhi) * sin(theta)

            camera.position.set(newX, newY, newZ)
            camera.lookAt(center)
            camera.update()
        }
    }

    /**
     * Verificar si el toque está en el área del joystick
     */
    private fun isJoystickAreaTouched(touchX: Float, touchY: Float): Boolean {
        val distance = distance(touchX, touchY, joystickCenter.x, joystickCenter.y)
        return distance < joystickRadius * 1.5f // Área ligeramente mayor que el joystick
    }

    /**
     * Método para resetear la vista de cámara a posición segura
     */
    private fun resetCameraView() {
        // Restablecer valores de zoom
        currentZoomFactor = 1.0f
        cameraHeight = 20f
        cameraDistance = 20f

        // ✅ Restablecer a posición inicial segura
        val center = Vector3(gridWidth / 2f, 0f, gridHeight / 2f)
        val safePhi = 0.7f  // Ángulo seguro intermedio

        camera.position.set(
            center.x + cameraDistance * sin(safePhi) * cos(0f),
            center.y + cameraHeight * cos(safePhi),
            center.z + cameraDistance * sin(safePhi) * sin(0f)
        )

        camera.lookAt(center)
        camera.update()

        // ✅ Asegurar posición segura
        ensureCameraSafePosition()
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

            // ✅ Mostrar información de cámara en logs
            val center = Vector3(gridWidth / 2f, 0f, gridHeight / 2f)
            val direction = Vector3(camera.position).sub(center)
            val phi = Math.toDegrees(atan2(sqrt(direction.x * direction.x + direction.z * direction.z), direction.y).toDouble()).toInt()
            Gdx.app.log("GameScreen-DEBUG", "Cámara: Ángulo=$phi°, Altura=${cameraHeight.toInt()}, Dist=${cameraDistance.toInt()}, Zoom=${(currentZoomFactor * 100).toInt()}%")
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
        val controlsText = "CONTROLES DE CÁMARA:"
        font.draw(spriteBatch, controlsText, 20f, Gdx.graphics.height - 30f)

        val zoomText = "- Zoom: 2 dedos (pellizcar/separar)"
        font.draw(spriteBatch, zoomText, 40f, Gdx.graphics.height - 60f)

        val rotateText = "- Girar: 1 dedo arrastrar"
        font.draw(spriteBatch, rotateText, 40f, Gdx.graphics.height - 85f)

        val resetText = "- Reset: Botón 'RESET VISTA'"
        font.draw(spriteBatch, resetText, 40f, Gdx.graphics.height - 110f)

        spriteBatch.end()
    }

    private fun renderDebugInfo(state: com.tron3d.models.GameState) {
        spriteBatch.begin()
        font.color = Color.YELLOW
        font.data.setScale(1f)

        if (isBluetooth) {
            font.draw(spriteBatch, "BLUETOOTH: ${if (isHost) "HOST" else "CLIENT"}", 20f, 250f)
            font.draw(spriteBatch, "Control: ${if (isHost) "CYAN" else "ORANGE"}", 20f, 220f)
        }

        font.draw(spriteBatch, "P1: (${state.player1Position.x.toInt()}, ${state.player1Position.y.toInt()}) ${state.player1Direction}", 20f, 190f)
        font.draw(spriteBatch, "P2: (${state.player2Position.x.toInt()}, ${state.player2Position.y.toInt()}) ${state.player2Direction}", 20f, 160f)

        // ✅ Información básica de depuración
        font.color = Color.GREEN
        font.draw(spriteBatch, "Motos activas: P1 y P2", 20f, 130f)

        // ✅ Mostrar información de cámara
        font.color = Color.CYAN
        val cameraPos = "Cámara: (${camera.position.x.toInt()}, ${camera.position.y.toInt()}, ${camera.position.z.toInt()})"
        font.draw(spriteBatch, cameraPos, 20f, 100f)

        // Mostrar información de zoom
        font.draw(spriteBatch, "Zoom: ${(currentZoomFactor * 100).toInt()}%", 20f, 70f)

        // Calcular y mostrar ángulo vertical
        val center = Vector3(gridWidth / 2f, 0f, gridHeight / 2f)
        val direction = Vector3(camera.position).sub(center)
        val phi = Math.toDegrees(atan2(sqrt(direction.x * direction.x + direction.z * direction.z), direction.y).toDouble()).toInt()
        font.draw(spriteBatch, "Ángulo: ${phi}° (Límite: ${Math.toDegrees(minVerticalAngle.toDouble()).toInt()}°-${Math.toDegrees(maxVerticalAngle.toDouble()).toInt()}°)", 20f, 40f)

        // Mostrar ronda actual
        font.color = Color.MAGENTA
        font.draw(spriteBatch, "Ronda: ${state.currentRound}", 20f, 10f)

        // Mostrar gesto activo
        font.color = Color.WHITE
        val gestureInfo = when {
            isPinching -> "ZOOM ACTIVO"
            isDragging -> "GIRO ACTIVO"
            else -> "GESTOS DISPONIBLES"
        }
        font.draw(spriteBatch, gestureInfo, Gdx.graphics.width - 200f, 40f)

        spriteBatch.end()
    }

    private fun renderMenuButton() {
        spriteBatch.begin()
        font.color = Color.WHITE
        font.data.setScale(1.5f)
        font.draw(spriteBatch, "MENU", 30f, Gdx.graphics.height - 30f)

        // Botón para resetear vista
        font.draw(spriteBatch, "RESET VISTA", Gdx.graphics.width - 200f, Gdx.graphics.height - 30f)

        // ✅ Botón para forzar limpieza (debugging)
        font.draw(spriteBatch, "LIMPIAR", Gdx.graphics.width / 2 - 50f, Gdx.graphics.height - 30f)

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
                // ✅ PRIMERO: Limpiar todos los rastros visuales
                clearAllTrailsImmediately()

                // ✅ SEGUNDO: Ahora reiniciar la ronda
                gameViewModel.restartRound()
                Gdx.app.log("GameScreen", "🔄 Ronda reiniciada con rastros limpios")
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

            // ✅ Verificar si es clic en botón "LIMPIAR"
            if (touchX > Gdx.graphics.width / 2 - 100f &&
                touchX < Gdx.graphics.width / 2 + 100f &&
                touchY > Gdx.graphics.height - 100f) {
                clearAllTrailsImmediately()
                Gdx.app.log("GameScreen", "🧹 LIMPIEZA FORZADA MANUAL")
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
        if (this::player1Cycle.isInitialized) player1Cycle.dispose()
        if (this::player2Cycle.isInitialized) player2Cycle.dispose()
        arenaModel?.dispose()
        spriteBatch.dispose()
        font.dispose()
        shapeRenderer.dispose()
        gameHUD.dispose()
    }
}
