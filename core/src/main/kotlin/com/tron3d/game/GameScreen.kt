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

/**
 * GameScreen 3D FUNCIONAL
 * - Vista 3D completa
 * - Cámara fija desde arriba
 * - Tablero tamaño pantalla
 * - Motos 3D con trails 3D
 */
class GameScreen(
    private val game: Tron3DGame,
    private val gameViewModel: GameViewModel
) : Screen {

    private lateinit var camera: PerspectiveCamera
    private lateinit var renderer: TronRenderer
    private lateinit var player1Cycle: LightCycle
    private lateinit var player2Cycle: LightCycle

    private val spriteBatch: SpriteBatch = SpriteBatch()
    private val font: BitmapFont = BitmapFont()
    private val gameHUD: GameHUD
    private val shapeRenderer: ShapeRenderer = ShapeRenderer()

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    // Cámara 3D FIJA - vista isométrica
    private var cameraHeight = 45f
    private var cameraDistance = 25f

    // Joystick
    private val joystickRadius = 140f
    private val joystickInnerRadius = 60f
    private var joystickCenter = Vector2()
    private var joystickPosition = Vector2()
    private var joystickTouched = false
    private var joystickPointer = -1

    private val tronCyan = TronVisualConfig.NeonColors.CYAN
    private val tronOrange = TronVisualConfig.NeonColors.ORANGE

    // Tamaño del tablero
    private val gridWidth = 50f
    private val gridHeight = 30f

    init {
        font.color = tronCyan
        font.data.setScale(1.8f)
        gameHUD = GameHUD(spriteBatch, font)
        updateJoystickPosition()
    }

    override fun show() {
        // Cámara 3D perspectiva FIJA desde arriba
        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

        // Posicionar cámara arriba mirando hacia abajo
        camera.position.set(
            gridWidth / 2f,
            cameraHeight,
            gridHeight / 2f + cameraDistance
        )
        camera.lookAt(gridWidth / 2f, 0f, gridHeight / 2f)
        camera.near = 1f
        camera.far = 300f
        camera.update()

        renderer = TronRenderer(camera)

        // Crear motos 3D
        player1Cycle = LightCycle(
            colorNeon = tronCyan,
            initialPosition = Vector3(10f, 0f, 15f)
        )

        player2Cycle = LightCycle(
            colorNeon = tronOrange,
            initialPosition = Vector3(40f, 0f, 15f)
        )

        gameViewModel.startNewGame()
        observeGameState()

        Gdx.app.log("GameScreen", "Juego 3D iniciado - Cámara FIJA")
    }

    private fun observeGameState() {
        coroutineScope.launch {
            gameViewModel.gameState.collect { state ->
                // Sincronizar posiciones 3D
                player1Cycle.position.set(
                    state.player1Position.x,
                    0f,  // Y = 0 (suelo)
                    state.player1Position.y  // Z = posición Y del estado
                )
                player1Cycle.rotation = state.player1Direction.getRotationAngle()

                player2Cycle.position.set(
                    state.player2Position.x,
                    0f,
                    state.player2Position.y
                )
                player2Cycle.rotation = state.player2Direction.getRotationAngle()

                Gdx.app.log("GameScreen", "P1: (${state.player1Position.x.toInt()}, ${state.player1Position.y.toInt()}) Dir: ${state.player1Direction}")
            }
        }
    }

    override fun render(delta: Float) {
        handleInput()

        val state = gameViewModel.gameState.value

        // Actualizar motos (para trails 3D)
        player1Cycle.update(delta)
        player2Cycle.update(delta)

        // Limpiar pantalla
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        // Renderizar escena 3D
        renderer.render(listOf(player1Cycle, player2Cycle))

        // UI 2D encima
        gameHUD.render(gameViewModel)
        renderJoystickHexagonal()
        renderMenuButton()
        renderDebugInfo(state)
    }

    private fun renderDebugInfo(state: com.tron3d.models.GameState) {
        spriteBatch.begin()
        font.color = Color.YELLOW
        font.data.setScale(1f)
        font.draw(spriteBatch, "P1: (${state.player1Position.x.toInt()}, ${state.player1Position.y.toInt()})", 20f, 150f)
        font.draw(spriteBatch, "P2: (${state.player2Position.x.toInt()}, ${state.player2Position.y.toInt()})", 20f, 120f)
        font.draw(spriteBatch, "Turno: ${state.currentTurn.getDisplayName()}", 20f, 90f)
        font.draw(spriteBatch, "Trails P1: ${player1Cycle.trailInstances.size}", 20f, 60f)
        font.draw(spriteBatch, "Trails P2: ${player2Cycle.trailInstances.size}", 20f, 30f)
        spriteBatch.end()
    }

    private fun renderMenuButton() {
        val state = gameViewModel.gameState.value

        spriteBatch.begin()

        font.color = Color.WHITE
        font.data.setScale(1.5f)
        font.draw(spriteBatch, "MENU", 30f, Gdx.graphics.height - 30f)

        if (state.status.isGameOver()) {
            font.color = tronCyan
            font.data.setScale(2.5f)
            font.draw(spriteBatch, "TOCA PARA CONTINUAR",
                Gdx.graphics.width / 2f - 350f,
                Gdx.graphics.height / 2f)
        }

        spriteBatch.end()
    }

    private fun renderJoystickHexagonal() {
        val state = gameViewModel.gameState.value
        if (state.status != GameStatus.PLAYING) return

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.1f, 0.3f, 0.5f, 0.4f)
        drawHexagon(joystickCenter.x, joystickCenter.y, joystickRadius, true)
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glLineWidth(5f)
        shapeRenderer.color = if (state.currentTurn == PlayerTurn.PLAYER1) tronCyan else tronOrange
        drawHexagon(joystickCenter.x, joystickCenter.y, joystickRadius, false)
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        if (joystickTouched) {
            shapeRenderer.color = Color(0.3f, 0.9f, 1f, 0.5f)
            shapeRenderer.circle(joystickPosition.x, joystickPosition.y, joystickInnerRadius + 20f)
        }

        shapeRenderer.color = if (joystickTouched) {
            if (state.currentTurn == PlayerTurn.PLAYER1) tronCyan else tronOrange
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

        handleJoystickInput()

        if (Gdx.input.justTouched()) {
            val touchX = Gdx.input.getX().toFloat()
            val touchY = Gdx.graphics.height - Gdx.input.getY().toFloat()

            if (touchX < 150f && touchY > Gdx.graphics.height - 100f) {
                game.showMenu()
            }
        }
    }

    private fun handleJoystickInput() {
        var anyTouched = false

        for (i in 0 until 5) {
            if (Gdx.input.isTouched(i)) {
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
                        gameViewModel.makeMove(direction)
                        Gdx.app.log("GameScreen", "Movimiento: $direction")
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
        spriteBatch.dispose()
        font.dispose()
        shapeRenderer.dispose()
        gameHUD.dispose()
    }
}
