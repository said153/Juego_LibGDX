package com.tron3d.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import com.tron3d.models.GameStatus
import com.tron3d.viewmodel.GameViewModel

/**
 * HUD del juego - muestra información en pantalla
 * CORREGIDO: Compatible con GameViewModel nuevo
 */
class GameHUD(
    private val batch: SpriteBatch,
    private val font: BitmapFont
) : Disposable {

    private val tronCyan = Color(0f, 0.9f, 1f, 1f)
    private val tronOrange = Color(1f, 0.5f, 0f, 1f)

    private var screenWidth = Gdx.graphics.width.toFloat()
    private var screenHeight = Gdx.graphics.height.toFloat()

    /**
     * Renderiza el HUD
     */
    fun render(gameViewModel: GameViewModel) {
        val state = gameViewModel.gameState.value

        batch.begin()

        // Puntuación Jugador 1 (esquina superior izquierda)
        font.color = tronCyan
        font.data.setScale(2f)
        font.draw(batch, "P1: ${state.player1Score}", 20f, screenHeight - 20f)

        // Puntuación Jugador 2 (esquina superior derecha)
        font.color = tronOrange
        font.draw(batch, "P2: ${state.player2Score}", screenWidth - 200f, screenHeight - 20f)

        // Indicador de turno (si está jugando)
        if (state.status == GameStatus.PLAYING && state.isMultiplayer) {
            font.color = Color.WHITE
            font.data.setScale(1.5f)
            val turnText = "Turno: ${state.currentTurn.getDisplayName()}"
            font.draw(batch, turnText, screenWidth / 2f - 100f, screenHeight - 20f)
        }

        // Round actual
        font.color = Color.WHITE
        font.data.setScale(1.2f)
        font.draw(batch, "Round ${state.currentRound}", screenWidth / 2f - 70f, screenHeight - 60f)

        // Mensaje de fin de juego
        when (state.status) {
            GameStatus.PLAYER1_WON -> {
                renderGameOverMessage("¡JUGADOR 1 GANA!", tronCyan)
            }
            GameStatus.PLAYER2_WON -> {
                renderGameOverMessage("¡JUGADOR 2 GANA!", tronOrange)
            }
            GameStatus.DRAW -> {
                renderGameOverMessage("¡EMPATE!", Color.YELLOW)
            }
            GameStatus.PAUSED -> {
                renderPausedMessage()
            }
            else -> {}
        }

        // Instrucciones de control (parte inferior)
        if (state.status == GameStatus.PLAYING) {
            font.color = Color.LIGHT_GRAY
            font.data.setScale(1f)
            font.draw(batch, "ESC: Menú | R: Reiniciar", 20f, 60f)
        } else if (state.status.isGameOver()) {
            font.color = Color.WHITE
            font.data.setScale(1.5f)
            font.draw(batch, "ESPACIO: Siguiente Round", screenWidth / 2f - 200f, 100f)
        }

        batch.end()
    }

    private fun renderGameOverMessage(message: String, color: Color) {
        // Fondo semi-transparente
        // (Nota: necesitarías ShapeRenderer para esto, por ahora solo texto)

        // Mensaje principal
        font.color = color
        font.data.setScale(4f)
        val messageWidth = 300f
        font.draw(batch, message, screenWidth / 2f - messageWidth / 2f, screenHeight / 2f + 50f)

        // Instrucción
        font.color = Color.WHITE
        font.data.setScale(1.5f)
        font.draw(batch, "Presiona ESPACIO para continuar",
            screenWidth / 2f - 250f, screenHeight / 2f - 50f)
    }

    private fun renderPausedMessage() {
        font.color = Color.WHITE
        font.data.setScale(3f)
        font.draw(batch, "PAUSADO", screenWidth / 2f - 150f, screenHeight / 2f)

        font.data.setScale(1.5f)
        font.draw(batch, "Presiona ESC para continuar",
            screenWidth / 2f - 200f, screenHeight / 2f - 50f)
    }

    /**
     * Actualiza el tamaño de la pantalla
     */
    fun resize(width: Int, height: Int) {
        screenWidth = width.toFloat()
        screenHeight = height.toFloat()
    }

    override fun dispose() {
        // El batch y font se disponen en el GameScreen
    }
}
