package com.tron3d.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
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
import com.tron3d.debug.DebugPointController
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
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import com.tron3d.ai.TronAI
import com.tron3d.ai.AIDifficulty
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer

/**
 * GameScreen 3D - CON ARENA 3D Y GESTOS DE ZOOM/PAN
 * AMBAS MOTOS VISIBLES Y FUNCIONALES
 * ZOOM CON DOS DEDOS IMPLEMENTADO
 * ✅ SISTEMA DE DEBUG INTEGRADO
 */
class GameScreen(
    private val game: Tron3DGame,
    private val gameViewModel: GameViewModel,
    private val isBluetooth: Boolean = false,
    private val isHost: Boolean = false,
    private val bluetoothManager: BluetoothInterface? = null,
    private val isSinglePlayer: Boolean = false,
    private val aiDifficulty: AIDifficulty = AIDifficulty.NORMAL
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

    private var cameraFollowPlayer = true
    private var cameraTarget = Vector3()  // Punto que sigue la cámara
    private val cameraFollowSpeed = 0.1f  // Suavidad del seguimiento
    private val cameraOffset = Vector3(-20f, 25f, -20f)  // Posición relativa al jugador


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
    private var isFirstPersonCamera = false
    private val firstPersonCameraHeight = 4.5f
    private val firstPersonCameraOffset = 4.0f
    private val minCameraHeight = 15f
    private val maxCameraHeight = 80f
    private val minCameraDistance = 10f
    private val maxCameraDistance = 50f
    private val minVerticalAngle = 0.3f
    private val maxVerticalAngle = 1.5f

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

    // Colores
    private val motoCyan = Color(0f, 0.5f, 0.7f, 1f)      // Cyan oscuro para moto
    private val trailCyan = Color(0f, 1f, 1f, 1f)         // Cyan brillante para rastro

    private val motoOrange = Color(0.8f, 0.3f, 0f, 1f)    // Naranja oscuro para moto
    private val trailOrange = Color(1f, 0.8f, 0f, 1f)     // Naranja/amarillo brillante para rastro

    private val motoBlue = Color(0f, 0.4f, 0.8f, 1f)      // Azul oscuro para moto (alternativa)
    private val trailBlue = Color(0.2f, 0.6f, 1f, 1f)     // Azul brillante para rastro

    // ✅ LÍMITES REALES DE LA ARENA
    private val GRID_MIN_X = -20f
    private val GRID_MAX_X = 70f
    private val GRID_MIN_Z = -15f
    private val GRID_MAX_Z = 25f
    private val GRID_CENTER_X = 25f
    private val GRID_CENTER_Z = 5f
    private val gridWidth = GRID_MAX_X - GRID_MIN_X
    private val gridHeight = GRID_MAX_Z - GRID_MIN_Z
    private var invertYAxis = true  // Añade esta variable

    private var controlledPlayer: PlayerTurn = if (isHost) PlayerTurn.PLAYER1 else PlayerTurn.PLAYER2

    // ✅ NUEVO: Variable para debug de límites
    private var showCollisionBounds = true

    // ✅ NUEVO: Controlador de punto de debug
    private lateinit var debugPointController: DebugPointController
    private var isDebugMode = false
    private var buttonDebugBounds = ButtonBounds(30f, 100f, 200f, 50f)
    private var buttonExitDebugBounds = ButtonBounds(Gdx.graphics.width / 2f - 150f, 100f, 300f, 50f)
    private val singlePlayerCameraHeight = 20f      // Altura de la cámara
    private val singlePlayerCameraDistance = 15f    // Distancia del jugador
    private val singlePlayerCameraAngle = 85f       // Ángulo en grados (0-90)
    private val singlePlayerCameraOffsetX = 0f      // Desplazamiento lateral extra
    private val singlePlayerCameraOffsetZ = 0f      // Desplazamiento frontal/trasero extra

    // ✅ IA
    private var tronAI: TronAI? = null

    // ✅ Giroscopio
    private var useGyroscope = false
    private var gyroSensitivity = 2.0f
    private var gyroThreshold = 0.5f
    private var lastGyroTime = 0f
    private val gyroDelay = 0.05f

    data class ButtonBounds(var x: Float, var y: Float, var width: Float, var height: Float) {
        fun contains(touchX: Float, touchY: Float): Boolean {
            return touchX >= x && touchX <= x + width && touchY >= y && touchY <= y + height
        }
    }

    init {
        font.data.setScale(1.8f)
        gameHUD = GameHUD(spriteBatch, font)
        updateJoystickPosition()
        lastSingleTouch = Vector2()
        firstFinger = Vector2()
        secondFinger = Vector2()
    }

    override fun show() {
        setupCamera()

        // ✅ INICIALIZAR CONTROLADOR DE DEBUG
        debugPointController = DebugPointController()
        debugPointController.setFixedY(2f, false)
        debugPointController.setMoveSpeed(0.2f)

        // 2. Cargar arena
        loadArena()

        // 3. Inicializar motos (ANTES de setupCamera)
        initializeCycles()

        // 4. Configurar cámara
        setupCamera()

        // 5. Limpiar rastros
        clearAllTrailsImmediately()

        // 6. Iniciar juego
        gameViewModel.startNewGame()
        observeGameState()

        // 7. IA y Giroscopio (si es modo single player)
        if (isSinglePlayer) {
            tronAI = TronAI(aiDifficulty)

            useGyroscope = true

            // ✅ CONFIGURAR SENSIBILIDAD (sin InputProcessor innecesario)
            gyroSensitivity = 3.0f  // Aumentar sensibilidad
            gyroThreshold = 0.15f   // Reducir umbral

            // ✅ VERIFICAR DISPONIBILIDAD DE SENSORES
            if (Gdx.input.isPeripheralAvailable(Input.Peripheral.Gyroscope)) {
                Gdx.app.log("GameScreen", "📱 Giroscopio disponible")
            } else if (Gdx.input.isPeripheralAvailable(Input.Peripheral.Accelerometer)) {
                Gdx.app.log("GameScreen", "📱 Acelerómetro disponible")
            } else {
                Gdx.app.log("GameScreen", "⚠️ Sin sensores - Usando joystick")
                useGyroscope = false
            }

            Gdx.app.log("GameScreen", "🤖 IA inicializada - Dificultad: $aiDifficulty")
            Gdx.app.log("GameScreen", "📹 Cámara primera persona activada")
        }

        // 8. Bluetooth
        if (isBluetooth && bluetoothManager != null) {
            gameViewModel.setupBluetooth(bluetoothManager, isHost)
            setupBluetoothListener()
            Gdx.app.log("GameScreen", "🔧 Bluetooth configurado - Host: $isHost")
        }

        val mode = when {
            isSinglePlayer -> "SINGLE PLAYER vs IA ($aiDifficulty)"
            isBluetooth -> if (isHost) "BLUETOOTH HOST" else "BLUETOOTH CLIENT"
            else -> "LOCAL MULTIPLAYER"
        }
        Gdx.app.log("GameScreen", "🎮 Modo: $mode")
    }

    private fun setupCamera() {
        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

        if (isSinglePlayer) {
            isFirstPersonCamera = false
            setupSinglePlayerCamera()
        } else {
            cameraHeight = 40f
            cameraDistance = 50f
            // ✅ INVERTIR ORIENTACIÓN de la cámara
            camera.position.set(
                GRID_CENTER_X,
                cameraHeight,
                GRID_CENTER_Z - cameraDistance * 0.8f  // Cambiar signo
            )
            camera.lookAt(GRID_CENTER_X, 0f, GRID_CENTER_Z)
            camera.up.set(0f, 1f, 0f)

            // ✅ Rotar cámara 180 grados para que coincida con controles
            camera.rotateAround(
                Vector3(GRID_CENTER_X, 0f, GRID_CENTER_Z),
                Vector3(0f, 1f, 0f),
                180f
            )
        }

        camera.near = 1f
        camera.far = 300f
        camera.update()
        renderer = TronRenderer(camera)
    }

    /**
     * ✅ NUEVO: Cámara para modo single player - VISTA DESDE ARRIBA
     */
    private fun setupSinglePlayerCamera() {
        val playerPos = player1Cycle.position

        // Calcular posición usando el ángulo configurado
        val radAngle = Math.toRadians(singlePlayerCameraAngle.toDouble()).toFloat()
        val offsetX = -singlePlayerCameraDistance * cos(radAngle) + singlePlayerCameraOffsetX
        val offsetZ = -singlePlayerCameraDistance * sin(radAngle) + singlePlayerCameraOffsetZ

        camera.position.set(
            playerPos.x + offsetX,
            playerPos.y + singlePlayerCameraHeight,
            playerPos.z + offsetZ
        )

        camera.lookAt(playerPos)
        camera.up.set(0f, 1f, 0f)
        camera.near = 1f
        camera.far = 300f
        camera.update()

        Gdx.app.log("Camera", "📹 Cámara single player - Altura: $singlePlayerCameraHeight, Distancia: $singlePlayerCameraDistance, Ángulo: $singlePlayerCameraAngle°")
    }

    /**
     * ✅ NUEVO: Cámara que sigue al jugador desde arriba (como un dron)
     */
    private fun updateSinglePlayerCamera() {
        if (!isSinglePlayer) return

        val playerPos = player1Cycle.position

        // Usar las variables configurables
        val radAngle = Math.toRadians(singlePlayerCameraAngle.toDouble()).toFloat()
        val offsetX = -singlePlayerCameraDistance * cos(radAngle) + singlePlayerCameraOffsetX
        val offsetZ = -singlePlayerCameraDistance * sin(radAngle) + singlePlayerCameraOffsetZ

        val desiredCameraPos = Vector3(
            playerPos.x + offsetX,
            playerPos.y + singlePlayerCameraHeight,
            playerPos.z + offsetZ
        )

        // Movimiento suave
        camera.position.lerp(desiredCameraPos, 0.1f)
        camera.lookAt(playerPos.x, playerPos.y + 2f, playerPos.z)
        camera.update()
    }

    private fun initializeCycles() {
        val player1Start = Vector3(15f, 0.05f, 5f)
        val player2Start = Vector3(55f, 0.05f, 5f)

        if (this::player1Cycle.isInitialized) {
            player1Cycle.clearTrail()
        }
        if (this::player2Cycle.isInitialized) {
            player2Cycle.dispose()
        }

        player1Cycle = LightCycle(
            colorNeon = motoCyan,      // ✅ Moto CYAN oscuro
            trailColor = trailCyan,    // ✅ Rastro CYAN brillante (nuevo parámetro)
            initialPosition = player1Start,
            playerId = 1,
            modelPath = "models/uploads_files_3392844_tron.g3db"
        )

        player2Cycle = LightCycle(
            colorNeon = motoOrange,    // ✅ Moto NARANJA oscuro
            trailColor = trailOrange,  // ✅ Rastro NARANJA/AMARILLO brillante
            initialPosition = player2Start,
            playerId = 2,
            modelPath = "models/uploads_files_3392844_tron.g3db"
        )

        player1Cycle.clearTrail()
        player2Cycle.clearTrail()

        Gdx.app.log("GameScreen", "✅ Motos con colores diferenciados")
    }

    private fun loadArena() {
        arenaModel = ArenaModel()
        val arenaLoaded = arenaModel?.load() ?: false

        if (arenaLoaded) {
            arenaModel?.collider?.let { collider ->
                val p1Pos = Vector2(15f, 5f)
                val p2Pos = Vector2(55f, 5f)
                gameViewModel.initializeWithArena(collider, p1Pos, p2Pos)
                Gdx.app.log("GameScreen", "✅ Arena cargada con collider")
            } ?: run {
                gameViewModel.startNewGame()
            }
        } else {
            gameViewModel.startNewGame()
        }
    }

    private fun observeGameState() {
        coroutineScope.launch {
            gameViewModel.gameState.collect { state ->
                player1Cycle.position.set(state.player1Position.x, 0.8f, state.player1Position.y)
                player1Cycle.rotation = state.player1Direction.getRotationAngle()

                player2Cycle.position.set(state.player2Position.x, 0.8f, state.player2Position.y)
                player2Cycle.rotation = state.player2Direction.getRotationAngle()
            }
        }
    }

    private fun clearAllTrailsImmediately() {
        if (!this::player1Cycle.isInitialized || !this::player2Cycle.isInitialized) return

        // ✅ LIMPIAR TRAILS DE LAS MOTOS
        player1Cycle.clearTrail()
        player2Cycle.clearTrail()

        // ✅ LIMPIAR TRAILS EN EL VIEWMODEL
        gameViewModel.clearTrails()

        // ✅ REINICIAR POSICIONES DE LAS MOTOS
        val p1Start = Vector3(15f, 0.8f, 5f)
        val p2Start = Vector3(55f, 0.8f, 5f)

        player1Cycle.position.set(p1Start)
        player2Cycle.position.set(p2Start)

        Gdx.app.log("GameScreen", "🧹 Rastros y posiciones limpiados completamente")
    }

    private fun setupBluetoothListener() {
        if (isBluetooth && bluetoothManager != null) {
            bluetoothManager.setOnMessageReceived { message ->
                Gdx.app.postRunnable {
                    handleBluetoothMessage(message)
                }
            }
        }
    }

    /**
     * ✅ Manejar mensajes Bluetooth - ACTUALIZADO PARA REINICIO
     */
    private fun handleBluetoothMessage(message: String) {
        val moveData = BluetoothProtocol.parsePlayerMoveMessage(message)
        if (moveData != null) {
            if (isHost && moveData.playerNumber == 2) {
                gameViewModel.updatePlayer2FromNetwork(moveData.position, moveData.direction, moveData.trail)
            } else if (!isHost && moveData.playerNumber == 1) {
                gameViewModel.updatePlayer1FromNetwork(moveData.position, moveData.direction, moveData.trail)
            }
            return
        }

        val gameOverData = BluetoothProtocol.parseGameOverMessage(message)
        if (gameOverData != null) {
            gameViewModel.updateScoreFromNetwork(gameOverData)
            return
        }

        // ✅ NUEVO: Intentar parsear como mensaje de reinicio
        val restartRound = BluetoothProtocol.parseRestartMessage(message)
        if (restartRound != null) {
            Gdx.app.log("GameScreen", "🔄 Recibido comando de reinicio: ronda $restartRound")
            // ✅ PRIMERO: Limpiar todos los rastros visuales
            clearAllTrailsImmediately()

            // ✅ SEGUNDO: Sincronizar el reinicio desde el host
            gameViewModel.syncRestartFromNetwork(restartRound)
            return
        }

        // ✅ También procesar el mensaje de colisión simple
        if (message.startsWith(BluetoothProtocol.MSG_COLLISION)) {
            val parts = message.split("|")
            if (parts.size >= 2) {
                val playerNumber = parts[1].toInt()
                Gdx.app.log("GameScreen", "📥 Recibido colisión para jugador $playerNumber")
                // Aquí podrías manejar la colisión si es necesario
            }
        }
    }

    private fun sendPlayerState() {
        if (!isBluetooth || bluetoothManager == null) return

        val state = gameViewModel.gameState.value

        if (state.status.isGameOver()) {
            val winner = when (state.status) {
                GameStatus.PLAYER1_WON -> 1
                GameStatus.PLAYER2_WON -> 2
                else -> 0
            }

            val message = BluetoothProtocol.createGameOverMessageWithScore(
                winner, state.player1Score, state.player2Score, state.currentRound
            )
            bluetoothManager.sendMessage(message)
        } else {
            val message = if (isHost) {
                BluetoothProtocol.createPlayerMoveMessage(1, state.player1Position, state.player1Direction, state.player1Trail)
            } else {
                BluetoothProtocol.createPlayerMoveMessage(2, state.player2Position, state.player2Direction, state.player2Trail)
            }
            bluetoothManager.sendMessage(message)
        }
    }

    override fun render(delta: Float) {
        // ✅ SIEMPRE manejar gestos de cámara (excepto en modo debug)
        if (!isDebugMode) {
            // Gestos de cámara (solo si NO es single player)
            if (!isSinglePlayer) {
                handleCameraGestures()
            }

            // Actualizar cámara single player
            if (isSinglePlayer) {
                updateSinglePlayerCamera()
            }
        }

        // ✅ MANEJAR TECLA ESPACIO PARA REINICIAR (modo local y Bluetooth)
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            handleRestartGame()
        }

        // ✅ Modo debug
        if (isDebugMode) {
            debugPointController.update(camera)
            Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

            val debugInstances = debugPointController.getDebugInstances()
            if (debugInstances.isNotEmpty()) {
                renderer.renderDebug(debugInstances)
            }
            renderer.render(listOf(), arenaModel)
            renderDebugPointInfo()
            renderDebugMenuButton()
            return
        }

        // ✅ Input normal
        if (isSinglePlayer && useGyroscope) {
            handleGyroscopeInput(delta)
            handleButtonsOnly()
        } else {
            handleInput()
        }

        // ✅ Actualizar IA
        if (isSinglePlayer && !gameViewModel.gameState.value.status.isGameOver()) {
            updateAIContinuous(delta)
        }

        // ✅ Actualizar motos
        player1Cycle.update(delta)
        player2Cycle.update(delta)

        // ✅ Renderizar
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        renderer.render(listOf(player1Cycle, player2Cycle), arenaModel)

        if (showCollisionBounds) {
            renderCollisionBounds()
            renderArenaPerimeter()
        }

        gameHUD.render(gameViewModel)

        if (!useGyroscope) {
            renderJoystickHexagonal()
        }

        renderMenuButton()
        renderDebugInfo(gameViewModel.gameState.value)
        renderCameraControls()
        renderRestartPrompt()
    }

    /**
     * ✅ NUEVO: Manejar solo botones (sin joystick) para modo giroscopio
     */
    private fun handleButtonsOnly() {
        val state = gameViewModel.gameState.value

        if (state.status.isGameOver()) {
            if (Gdx.input.justTouched()) {
                clearAllTrailsImmediately()
                gameViewModel.restartRound()
            }
            return
        }

        if (Gdx.input.justTouched()) {
            val touchX = Gdx.input.getX().toFloat()
            val touchY = Gdx.graphics.height - Gdx.input.getY().toFloat()

            // Botón RESET VISTA
            if (touchX > Gdx.graphics.width - 300f && touchY > Gdx.graphics.height - 80f) {
                resetCameraView()
                return
            }

            // Botón MENU
            if (touchX < 150f && touchY > Gdx.graphics.height - 100f) {
                game.showMenu()
                return
            }

            // Botón DEBUG
            if (buttonDebugBounds.contains(touchX, touchY)) {
                isDebugMode = true
                debugPointController.toggleEnabled()
                return
            }
        }
    }

    private fun updateAIContinuous(delta: Float) {
        val ai = tronAI ?: return
        val gameState = gameViewModel.gameState.value
        val aiMove = ai.decideMove(gameState, delta)

        if (aiMove != null) {
            Gdx.app.log("GameScreen", "🤖 IA decide: $aiMove")
            if (!aiMove.isOpposite(gameState.player2Direction)) {
                gameViewModel.makeMoveNoTurn(aiMove, PlayerTurn.PLAYER2)
            }
        }
    }

    /**
     * ✅ GIROSCOPIO SIMPLIFICADO - FUNCIONA CON ACELERÓMETRO BÁSICO
     */
    private fun handleGyroscopeInput(delta: Float) {
        if (!useGyroscope || !isSinglePlayer) return

        val gameState = gameViewModel.gameState.value
        if (gameState.status != GameStatus.PLAYING) return

        try {
            // Intentar varios sensores
            var tiltX = 0f

            if (Gdx.input.isPeripheralAvailable(Input.Peripheral.Gyroscope)) {
                tiltX = Gdx.input.getGyroscopeX() * 10f  // Multiplicador alto
            } else if (Gdx.input.isPeripheralAvailable(Input.Peripheral.Accelerometer)) {
                tiltX = Gdx.input.getAccelerometerX() * 5f
            } else {
                return
            }

            // DEBUG VISUAL EN PANTALLA
            spriteBatch.begin()
            font.color = Color.YELLOW
            font.data.setScale(1.5f)
            font.draw(spriteBatch, "INCLINACIÓN: ${"%.2f".format(tiltX)}",
                Gdx.graphics.width / 2f - 100f, 200f)
            spriteBatch.end()

            val currentDir = gameState.player1Direction
            var newDirection: Direction? = null

            // GIRAR CON CUALQUIER INCLINACIÓN (sin umbral)
            if (tiltX > 0.1f) {
                newDirection = when (currentDir) {
                    Direction.UP -> Direction.RIGHT
                    Direction.RIGHT -> Direction.DOWN
                    Direction.DOWN -> Direction.LEFT
                    Direction.LEFT -> Direction.UP
                }
            } else if (tiltX < -0.1f) {
                newDirection = when (currentDir) {
                    Direction.UP -> Direction.LEFT
                    Direction.LEFT -> Direction.DOWN
                    Direction.DOWN -> Direction.RIGHT
                    Direction.RIGHT -> Direction.UP
                }
            }

            if (newDirection != null) {
                gameViewModel.makeMoveNoTurn(newDirection, PlayerTurn.PLAYER1)
            }

        } catch (e: Exception) {
            Gdx.app.error("Gyro", "Error: ${e.message}")
        }
    }

    private fun handleCameraGestures() {
        val touchCount = getTouchCount()

        when (touchCount) {
            2 -> {
                handlePinchZoom()
                isDragging = false
            }
            1 -> {
                val touchX = Gdx.input.getX(0).toFloat()
                val touchY = Gdx.graphics.height - Gdx.input.getY(0).toFloat()
                if (!isJoystickAreaTouched(touchX, touchY)) {
                    handleDragPan()
                }
            }
            else -> {
                isPinching = false
                isDragging = false
            }
        }
    }

    private fun handlePinchZoom() {
        val currentFinger1 = Vector2(Gdx.input.getX(0).toFloat(), Gdx.input.getY(0).toFloat())
        val currentFinger2 = Vector2(Gdx.input.getX(1).toFloat(), Gdx.input.getY(1).toFloat())
        val currentDistance = currentFinger1.dst(currentFinger2)

        if (!isPinching) {
            firstFinger.set(currentFinger1)
            secondFinger.set(currentFinger2)
            initialPinchDistance = currentDistance
            previousPinchDistance = currentDistance
            isPinching = true
            isDragging = false
        } else {
            val distanceDelta = currentDistance - previousPinchDistance
            val zoomDelta = distanceDelta * pinchZoomSensitivity
            currentZoomFactor -= zoomDelta
            currentZoomFactor = currentZoomFactor.coerceIn(minZoomFactor, maxZoomFactor)
            applyZoomToCamera(currentZoomFactor)
            previousPinchDistance = currentDistance
        }

        ensureCameraSafePosition()
    }

    private fun applyZoomToCamera(zoomFactor: Float) {
        val center = Vector3(GRID_CENTER_X, 0f, GRID_CENTER_Z)
        cameraHeight = (20f * zoomFactor).coerceIn(minCameraHeight, maxCameraHeight)
        cameraDistance = (20f * zoomFactor).coerceIn(minCameraDistance, maxCameraDistance)

        val currentDirection = Vector3(camera.position).sub(center).nor()
        val newPosition = center.cpy().add(
            currentDirection.x * cameraDistance,
            cameraHeight,
            currentDirection.z * cameraDistance
        )

        camera.position.set(newPosition)
        camera.lookAt(center)
        camera.update()
    }

    private fun handleDragPan() {
        if (Gdx.input.isTouched(1)) return

        val currentTouch = Vector2(Gdx.input.getX(0).toFloat(), Gdx.input.getY(0).toFloat())

        if (!isDragging) {
            lastSingleTouch.set(currentTouch)
            isDragging = true
            isPinching = false
        } else {
            val deltaX = currentTouch.x - lastSingleTouch.x
            val deltaY = currentTouch.y - lastSingleTouch.y

            if (Math.abs(deltaX) > 2 || Math.abs(deltaY) > 2) {
                rotateCameraAroundCenter(deltaX, deltaY)
            }

            lastSingleTouch.set(currentTouch)
        }
    }

    private fun rotateCameraAroundCenter(deltaX: Float, deltaY: Float) {
        val center = Vector3(GRID_CENTER_X, 0f, GRID_CENTER_Z)
        val direction = Vector3(camera.position).sub(center)

        val radius = direction.len()
        var theta = atan2(direction.z, direction.x).toFloat()
        var phi = atan2(sqrt(direction.x * direction.x + direction.z * direction.z), direction.y).toFloat()

        theta -= deltaX * 0.005f
        phi -= deltaY * 0.005f
        phi = phi.coerceIn(minVerticalAngle, maxVerticalAngle)

        val newX = center.x + radius * sin(phi) * cos(theta)
        val newY = center.y + radius * cos(phi)
        val newZ = center.z + radius * sin(phi) * sin(theta)

        camera.position.set(newX, newY, newZ)
        camera.lookAt(center)
        camera.update()
    }

    private fun getTouchCount(): Int {
        var count = 0
        for (i in 0 until 5) {
            if (Gdx.input.isTouched(i)) count++
        }
        return count
    }

    private fun ensureCameraSafePosition() {
        val center = Vector3(GRID_CENTER_X, 0f, GRID_CENTER_Z)
        val direction = Vector3(camera.position).sub(center)
        val phi = atan2(sqrt(direction.x * direction.x + direction.z * direction.z), direction.y).toFloat()

        if (phi < minVerticalAngle || phi > maxVerticalAngle) {
            val safePhi = phi.coerceIn(minVerticalAngle, maxVerticalAngle)
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

    private fun isJoystickAreaTouched(touchX: Float, touchY: Float): Boolean {
        val distance = distance(touchX, touchY, joystickCenter.x, joystickCenter.y)
        return distance < joystickRadius * 1.5f
    }

    private fun resetCameraView() {
        currentZoomFactor = 1.0f
        cameraHeight = 20f
        cameraDistance = 20f

        val center = Vector3(GRID_CENTER_X, 0f, GRID_CENTER_Z)
        val safePhi = 0.7f

        camera.position.set(
            center.x + cameraDistance * sin(safePhi),
            center.y + cameraHeight * cos(safePhi),
            center.z + cameraDistance * sin(safePhi)
        )

        camera.lookAt(center)
        camera.update()
        ensureCameraSafePosition()
    }

    private fun renderCollisionBounds() {
        if (!showCollisionBounds) return

        arenaModel?.collider?.let { collider ->
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            shapeRenderer.color = Color(1f, 0f, 0f, 1f)

            try {
                val segments = collider.getPerimeterSegments()
                for ((start, end) in segments) {
                    shapeRenderer.line(start.x, 0.5f, start.y, end.x, 0.5f, end.y)
                    shapeRenderer.color = Color.GREEN
                    shapeRenderer.box(start.x - 0.1f, 0.3f, start.y - 0.1f, 0.2f, 0.2f, 0.2f)
                    shapeRenderer.color = Color.BLUE
                    shapeRenderer.box(end.x - 0.1f, 0.3f, end.y - 0.1f, 0.2f, 0.2f, 0.2f)
                    shapeRenderer.color = Color(1f, 0f, 0f, 1f)
                }
            } catch (e: Exception) {
                Gdx.app.error("GameScreen", "Error renderizando: ${e.message}")
            }

            shapeRenderer.end()
        }
    }

    private fun renderArenaPerimeter() {
        if (!showCollisionBounds) return

        arenaModel?.collider?.let { collider ->
            val perimeterPoints = collider.getPerimeterPoints()
            if (perimeterPoints.size < 2) return

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            shapeRenderer.color = Color(0f, 1f, 0f, 1f)

            for (i in 0 until perimeterPoints.size) {
                val current = perimeterPoints[i]
                val next = perimeterPoints[(i + 1) % perimeterPoints.size]
                shapeRenderer.line(current.x, 0.5f, current.y, next.x, 0.5f, next.y)
            }

            shapeRenderer.end()
        }
    }

    private fun renderDebugPointInfo() {
        if (!isDebugMode) return

        spriteBatch.begin()
        font.color = Color.CYAN
        font.data.setScale(1.5f)

        val debugPos = debugPointController.getCurrentPosition()
        val x = debugPos.x
        val y = debugPos.y
        val z = debugPos.z

        font.draw(spriteBatch, "🔴 DEBUG: X=${"%.2f".format(x)} Y=${"%.2f".format(y)} Z=${"%.2f".format(z)}",
            20f, Gdx.graphics.height - 50f)

        font.color = Color.YELLOW
        font.data.setScale(1.0f)
        font.draw(spriteBatch, "CONTROLES: A/D (X), Q/E (Y), W/S (Z), SHIFT (rápido), CTRL (lento)",
            20f, Gdx.graphics.height - 100f)

        spriteBatch.end()
    }

    /**
     * ✅ NUEVO: Renderizar mensaje para reiniciar
     */
    private fun renderRestartPrompt() {
        val state = gameViewModel.gameState.value

        if (state.status.isGameOver()) {
            spriteBatch.begin()
            font.data.setScale(2.5f)
            font.draw(spriteBatch, "TOCA PARA CONTINUAR", Gdx.graphics.width / 2f - 350f, Gdx.graphics.height / 2f)

            // ✅ NUEVO: Mostrar también opción de espacio
            font.color = Color.YELLOW
            font.data.setScale(1.5f)
            font.draw(spriteBatch, "o PRESIONA ESPACIO", Gdx.graphics.width / 2f - 200f, Gdx.graphics.height / 2f - 50f)

            // ✅ Mostrar información de sincronización Bluetooth
            if (isBluetooth) {
                font.color = if (isHost) Color.GREEN else Color.ORANGE
                font.data.setScale(1.2f)
                val syncText = if (isHost) "HOST: Reiniciarás ambos jugadores" else "CLIENTE: Esperando al host"
                font.draw(spriteBatch, syncText, Gdx.graphics.width / 2f - 250f, Gdx.graphics.height / 2f - 100f)
            }

            spriteBatch.end()
        }
    }

    /**
     * ✅ NUEVO: Manejar reinicio del juego
     */
    private fun handleRestartGame() {
        val state = gameViewModel.gameState.value

        // Solo permitir reinicio si el juego terminó
        if (state.status.isGameOver()) {
            // ✅ PRIMERO: Limpiar todos los rastros visuales
            clearAllTrailsImmediately()

            // ✅ SEGUNDO: Reiniciar la ronda
            gameViewModel.restartRound()

            Gdx.app.log("GameScreen", "🔄 Reinicio activado por ESPACIO - Ronda ${state.currentRound + 1}")

            // ✅ En modo Bluetooth, mostrar mensaje de sincronización
            if (isBluetooth && isHost) {
                Gdx.app.log("GameScreen", "🔄 Host enviando comando de reinicio al cliente")
            }
        } else if (state.status == GameStatus.PLAYING) {
            // Si el juego está en progreso, pausar/reanudar con espacio
            if (state.status == GameStatus.PLAYING) {
                gameViewModel.pauseGame()
            } else if (state.status == GameStatus.PAUSED) {
                gameViewModel.resumeGame()
            }
        }
    }

    private var frameCount = 0

    /**
     * Renderizar indicadores de controles de cámara
     */
    private fun renderCameraControls() {
        if (isFirstPersonCamera) return

        spriteBatch.begin()
        font.color = Color.LIGHT_GRAY
        font.data.setScale(1.0f)

        font.draw(spriteBatch, "CONTROLES:", 20f, Gdx.graphics.height - 90f)
        font.draw(spriteBatch, "Zoom: 2 dedos | Girar: 1 dedo", 40f, Gdx.graphics.height - 120f)

        spriteBatch.end()
    }

    private fun renderDebugInfo(state: com.tron3d.models.GameState) {
        spriteBatch.begin()
        font.color = Color.YELLOW
        font.data.setScale(1f)

        if (isSinglePlayer) {
            font.color = Color.MAGENTA
            font.draw(spriteBatch, "MODO: 1 JUGADOR vs IA ($aiDifficulty)", 20f, 370f)
        }

        font.color = Color.YELLOW
        font.draw(spriteBatch, "P1: (${state.player1Position.x.toInt()}, ${state.player1Position.y.toInt()})", 20f, 340f)
        font.draw(spriteBatch, "P2: (${state.player2Position.x.toInt()}, ${state.player2Position.y.toInt()})", 20f, 310f)

        spriteBatch.end()
    }

    private fun renderMenuButton() {
        spriteBatch.begin()
        font.color = Color.WHITE
        font.data.setScale(1.5f)
        font.draw(spriteBatch, "MENU", 30f, Gdx.graphics.height - 10f)
        font.draw(spriteBatch, "RESET VISTA", Gdx.graphics.width - 250f, Gdx.graphics.height - 10f)

        font.color = Color.CYAN
        font.data.setScale(2.0f)

        if (gameViewModel.gameState.value.status.isGameOver()) {
            font.data.setScale(2.5f)
            font.draw(spriteBatch, "TOCA PARA CONTINUAR", Gdx.graphics.width / 2f - 350f, Gdx.graphics.height / 2f)
        }
        spriteBatch.end()
    }

    private fun renderDebugMenuButton() {
        spriteBatch.begin()
        font.color = Color.RED
        font.data.setScale(2.0f)
        font.draw(spriteBatch, "🔧 MODO DEBUG", Gdx.graphics.width / 2 - 100f, Gdx.graphics.height - 50f)

        font.color = Color.CYAN
        font.data.setScale(1.5f)
        font.draw(spriteBatch, "SALIR DEBUG", buttonExitDebugBounds.x + 50f, buttonExitDebugBounds.y + 35f)

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
        // ✅ Usar colores de la moto (oscuros) para el joystick
        shapeRenderer.color = if (controlledPlayer == PlayerTurn.PLAYER1) motoCyan else motoOrange
        drawHexagon(joystickCenter.x, joystickCenter.y, joystickRadius, false)
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        if (joystickTouched) {
            shapeRenderer.color = Color(0.3f, 0.9f, 1f, 0.5f)
            shapeRenderer.circle(joystickPosition.x, joystickPosition.y, joystickInnerRadius + 20f)
        }

        shapeRenderer.color = if (joystickTouched) {
            if (controlledPlayer == PlayerTurn.PLAYER1) motoCyan else motoOrange
        } else {
            Color(0.2f, 0.5f, 0.7f, 0.7f)
        }
        drawHexagon(joystickPosition.x, joystickPosition.y, joystickInnerRadius, true)
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
                clearAllTrailsImmediately()
                gameViewModel.restartRound()
            }
            return
        }

        if (state.status != GameStatus.PLAYING) return

        if (Gdx.input.justTouched()) {
            val touchX = Gdx.input.getX().toFloat()
            val touchY = Gdx.graphics.height - Gdx.input.getY().toFloat()

            if (touchX > Gdx.graphics.width - 300f && touchY > Gdx.graphics.height - 80f) {
                resetCameraView()
                return
            }

            if (touchX < 150f && touchY > Gdx.graphics.height - 100f) {
                game.showMenu()
                return
            }

            if (buttonDebugBounds.contains(touchX, touchY)) {
                isDebugMode = true
                debugPointController.toggleEnabled()
                return
            }
        }

        if (isDebugMode && Gdx.input.justTouched()) {
            val touchX = Gdx.input.getX().toFloat()
            val touchY = Gdx.graphics.height - Gdx.input.getY().toFloat()

            if (buttonExitDebugBounds.contains(touchX, touchY)) {
                isDebugMode = false
                debugPointController.toggleEnabled()
                return
            }
        }

        if (!isDebugMode) {
            handleJoystickInput()
        }
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
                        if (isBluetooth) {
                            gameViewModel.makeMoveForPlayer(direction, controlledPlayer)
                            sendPlayerState()
                        } else if (isSinglePlayer) {
                            gameViewModel.makeMoveNoTurn(direction, PlayerTurn.PLAYER1)
                        } else {
                            gameViewModel.makeMove(direction)
                        }

                        moveExecuted = true
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
            // ✅ Control invertible
            if (invertYAxis) {
                if (deltaY > 0) Direction.DOWN else Direction.UP  // Invertido
            } else {
                if (deltaY > 0) Direction.UP else Direction.DOWN  // Normal
            }
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
        buttonExitDebugBounds = ButtonBounds(width / 2f - 150f, 100f, 300f, 50f)
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
        debugPointController.dispose()
        tronAI = null
        spriteBatch.dispose()
        font.dispose()
        shapeRenderer.dispose()
        gameHUD.dispose()
    }
}
