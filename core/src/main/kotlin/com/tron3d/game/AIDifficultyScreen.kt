package com.tron3d.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.tron3d.ai.AIDifficulty
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pantalla de selección de dificultad para modo 1 jugador
 * Diseño TRON LEGACY con hexágonos
 */
class AIDifficultyScreen(
    private val game: Tron3DGame
) : Screen {

    private val spriteBatch: SpriteBatch = SpriteBatch()
    private val shapeRenderer: ShapeRenderer = ShapeRenderer()
    private val font: BitmapFont = BitmapFont()
    private val titleFont: BitmapFont = BitmapFont()

    private val easyButton = Rectangle()
    private val normalButton = Rectangle()
    private val hardButton = Rectangle()
    private val backButton = Rectangle()

    private val buttonWidth = 450f
    private val buttonHeight = 100f
    private val buttonPadding = 40f
    private val smallButtonWidth = 200f
    private val smallButtonHeight = 80f

    private val tronCyan = Color(0f, 0.9f, 1f, 1f)
    private val tronOrange = Color(1f, 0.4f, 0f, 1f)
    private val tronGreen = Color(0f, 1f, 0.5f, 1f)
    private val tronRed = Color(1f, 0.2f, 0.2f, 1f)

    private var hoveredButton: Rectangle? = null
    private var animationTime = 0f

    init {
        font.color = Color.WHITE
        font.data.setScale(2f)
        titleFont.color = tronOrange
        titleFont.data.setScale(4f)
        setupButtons()
    }

    private fun setupButtons() {
        val centerX = Gdx.graphics.width / 2f - buttonWidth / 2f
        val centerY = Gdx.graphics.height / 2f

        easyButton.set(centerX, centerY + 120f, buttonWidth, buttonHeight)
        normalButton.set(centerX, centerY, buttonWidth, buttonHeight)
        hardButton.set(centerX, centerY - 120f, buttonWidth, buttonHeight)
        backButton.set(50f, 50f, smallButtonWidth, smallButtonHeight)
    }

    override fun show() {
        Gdx.app.log("AIDifficultyScreen", "Mostrando selección de dificultad")
    }

    override fun render(delta: Float) {
        animationTime += delta

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        checkHover()
        renderBackground()
        renderDecorations()
        renderTitle()
        renderButtons()
        handleInput()
    }

    private fun renderBackground() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glLineWidth(1f)

        val gridSize = 100f
        val offset = (animationTime * 20f) % gridSize

        var x = -offset
        while (x < Gdx.graphics.width) {
            val alpha = (sin(x / 50f + animationTime) * 0.2f + 0.3f).coerceIn(0.1f, 0.5f)
            shapeRenderer.color = Color(0.1f, 0.5f, 0.8f, alpha)
            shapeRenderer.line(x, 0f, x, Gdx.graphics.height.toFloat())
            x += gridSize
        }

        var y = -offset
        while (y < Gdx.graphics.height) {
            val alpha = (cos(y / 50f + animationTime) * 0.2f + 0.3f).coerceIn(0.1f, 0.5f)
            shapeRenderer.color = Color(0.1f, 0.5f, 0.8f, alpha)
            shapeRenderer.line(0f, y, Gdx.graphics.width.toFloat(), y)
            y += gridSize
        }

        shapeRenderer.end()
    }

    private fun renderDecorations() {
        val pulseAlpha = (sin(animationTime * 2f) * 0.3f + 0.5f).coerceIn(0.2f, 0.8f)

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        shapeRenderer.color = Color(tronOrange.r, tronOrange.g, tronOrange.b, pulseAlpha * 0.3f)
        shapeRenderer.rect(0f, Gdx.graphics.height / 2f - 150f, 5f, 300f)
        shapeRenderer.rect(Gdx.graphics.width - 5f, Gdx.graphics.height / 2f - 150f, 5f, 300f)

        shapeRenderer.end()

        drawHexagon(150f, Gdx.graphics.height - 150f, 50f, tronOrange, pulseAlpha * 0.4f)
        drawHexagon(Gdx.graphics.width - 150f, Gdx.graphics.height - 150f, 50f, tronCyan, pulseAlpha * 0.4f)
    }

    private fun drawHexagon(centerX: Float, centerY: Float, radius: Float, color: Color, alpha: Float) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glLineWidth(2f)
        shapeRenderer.color = Color(color.r, color.g, color.b, alpha)

        val vertices = mutableListOf<Pair<Float, Float>>()
        for (i in 0..6) {
            val angle = Math.toRadians((60 * i - 30).toDouble())
            val x = centerX + (radius * cos(angle)).toFloat()
            val y = centerY + (radius * sin(angle)).toFloat()
            vertices.add(Pair(x, y))
        }

        for (i in 0 until vertices.size - 1) {
            shapeRenderer.line(vertices[i].first, vertices[i].second,
                vertices[i + 1].first, vertices[i + 1].second)
        }

        shapeRenderer.end()
    }

    private fun renderTitle() {
        spriteBatch.begin()

        titleFont.color = tronOrange
        val pulseScale = 4f + sin(animationTime * 2f) * 0.2f
        titleFont.data.setScale(pulseScale)
        titleFont.draw(spriteBatch, "1 JUGADOR",
            Gdx.graphics.width / 2f - 250f,
            Gdx.graphics.height - 120f)

        font.color = Color.LIGHT_GRAY
        font.data.setScale(1.5f)
        font.draw(spriteBatch, "Selecciona la dificultad",
            Gdx.graphics.width / 2f - 180f,
            Gdx.graphics.height - 200f)

        spriteBatch.end()
    }

    private fun renderButtons() {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        // Botón FÁCIL (Verde)
        renderDifficultyButton(easyButton, "FACIL", tronGreen, "")

     // Botón NORMAL (Cyan)
        renderDifficultyButton(normalButton, "NORMAL", tronCyan, "")
        renderDifficultyButton(hardButton, "DIFICIL", tronRed, "")

        renderBackButton()

        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun renderDifficultyButton(button: Rectangle, text: String, color: Color, subtitle: String) {
        val isHovered = hoveredButton == button

        if (isHovered) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            val glowAlpha = (sin(animationTime * 4f) * 0.2f + 0.4f).coerceIn(0.2f, 0.6f)
            shapeRenderer.color = Color(color.r, color.g, color.b, glowAlpha * 0.5f)
            drawHexagonalRect(button.x - 8f, button.y - 8f, button.width + 16f, button.height + 16f)
            shapeRenderer.end()
        }

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.05f, 0.05f, 0.15f, 0.9f)
        drawHexagonalRect(button.x, button.y, button.width, button.height)
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glLineWidth(if (isHovered) 4f else 3f)
        shapeRenderer.color = color
        drawHexagonalRect(button.x, button.y, button.width, button.height)
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glLineWidth(1f)
        shapeRenderer.color = Color(color.r, color.g, color.b, 0.3f)
        val inset = 10f
        drawHexagonalRect(button.x + inset, button.y + inset,
            button.width - inset * 2, button.height - inset * 2)
        shapeRenderer.end()

        spriteBatch.begin()
        font.color = Color.WHITE
        font.data.setScale(if (isHovered) 2.3f else 2.1f)

        val textWidth = when (text) {
            "FACIL" -> 100f
            "NORMAL" -> 130f
            "DIFICIL" -> 140f
            else -> 100f
        }
        font.draw(spriteBatch, text,
            button.x + button.width / 2f - textWidth / 2f,
            button.y + button.height / 2f + 25f)

        // Subtítulo
        font.data.setScale(1.2f)
        font.color = Color.LIGHT_GRAY
        val subtitleWidth = when (subtitle) {
            "Ideal para principiantes" -> 200f
            "Desafio equilibrado" -> 170f
            "Solo para expertos" -> 170f
            else -> 150f
        }
        font.draw(spriteBatch, subtitle,
            button.x + button.width / 2f - subtitleWidth / 2f,
            button.y + 30f)

        spriteBatch.end()
    }

    private fun renderBackButton() {
        val isHovered = hoveredButton == backButton

        if (isHovered) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            val glowAlpha = (sin(animationTime * 4f) * 0.2f + 0.4f).coerceIn(0.2f, 0.6f)
            shapeRenderer.color = Color(tronCyan.r, tronCyan.g, tronCyan.b, glowAlpha * 0.5f)
            drawHexagonalRect(backButton.x - 5f, backButton.y - 5f,
                backButton.width + 10f, backButton.height + 10f)
            shapeRenderer.end()
        }

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.05f, 0.05f, 0.15f, 0.9f)
        drawHexagonalRect(backButton.x, backButton.y, backButton.width, backButton.height)
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glLineWidth(if (isHovered) 4f else 3f)
        shapeRenderer.color = tronCyan
        drawHexagonalRect(backButton.x, backButton.y, backButton.width, backButton.height)
        shapeRenderer.end()

        spriteBatch.begin()
        font.color = Color.WHITE
        font.data.setScale(if (isHovered) 1.5f else 1.3f)
        font.draw(spriteBatch, "← VOLVER",
            backButton.x + 30f,
            backButton.y + backButton.height / 2f + 15f)
        spriteBatch.end()
    }

    private fun drawHexagonalRect(x: Float, y: Float, width: Float, height: Float) {
        val cutSize = 20f

        val points = floatArrayOf(
            x + cutSize, y,
            x + width - cutSize, y,
            x + width, y + cutSize,
            x + width, y + height - cutSize,
            x + width - cutSize, y + height,
            x + cutSize, y + height,
            x, y + height - cutSize,
            x, y + cutSize
        )

        for (i in 0 until points.size step 2) {
            val nextIndex = (i + 2) % points.size
            shapeRenderer.line(points[i], points[i + 1], points[nextIndex], points[nextIndex + 1])
        }
    }

    private fun checkHover() {
        val touchX = Gdx.input.x.toFloat()
        val touchY = Gdx.graphics.height - Gdx.input.y.toFloat()

        hoveredButton = when {
            easyButton.contains(touchX, touchY) -> easyButton
            normalButton.contains(touchX, touchY) -> normalButton
            hardButton.contains(touchX, touchY) -> hardButton
            backButton.contains(touchX, touchY) -> backButton
            else -> null
        }
    }

    private fun handleInput() {
        if (Gdx.input.justTouched()) {
            val touchX = Gdx.input.x.toFloat()
            val touchY = Gdx.graphics.height - Gdx.input.y.toFloat()

            when {
                easyButton.contains(touchX, touchY) -> {
                    Gdx.app.log("AIDifficultyScreen", "Dificultad FÁCIL seleccionada")
                    game.startSinglePlayer(AIDifficulty.FACIL)
                }
                normalButton.contains(touchX, touchY) -> {
                    Gdx.app.log("AIDifficultyScreen", "Dificultad NORMAL seleccionada")
                    game.startSinglePlayer(AIDifficulty.NORMAL)
                }
                hardButton.contains(touchX, touchY) -> {
                    Gdx.app.log("AIDifficultyScreen", "Dificultad DIFÍCIL seleccionada")
                    game.startSinglePlayer(AIDifficulty.DIFICIL)
                }
                backButton.contains(touchX, touchY) -> {
                    Gdx.app.log("AIDifficultyScreen", "Volver al menú principal")
                    game.showMenu()
                }
            }
        }
    }

    override fun resize(width: Int, height: Int) {
        setupButtons()
    }

    override fun pause() {}
    override fun resume() {}
    override fun hide() {}

    override fun dispose() {
        spriteBatch.dispose()
        shapeRenderer.dispose()
        font.dispose()
        titleFont.dispose()
    }
}
