package com.tron3d.game

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.tron3d.viewmodel.GameViewModel

/**
 * Clase principal del juego TRON 3D
 * COMPLETO con todos los métodos necesarios
 */
class Tron3DGame : Game() {

    private lateinit var gameViewModel: GameViewModel

    override fun create() {
        Gdx.app.log("Tron3DGame", "Iniciando juego TRON 3D")

        // Crear ViewModel que maneja la lógica del juego
        gameViewModel = GameViewModel()

        // Mostrar menú principal
        setScreen(MenuScreen(this))
    }

    /**
     * Muestra el menú principal
     */
    fun showMenu() {
        Gdx.app.log("Tron3DGame", "Mostrando menú principal")
        setScreen(MenuScreen(this))
    }

    /**
     * Muestra la pantalla de selección de modo
     */
    fun showModeSelection() {
        Gdx.app.log("Tron3DGame", "Mostrando selección de modo")
        setScreen(ModeSelectionScreen(this))
    }

    /**
     * Inicia el juego directamente (desde el menú de opciones)
     */
    fun startGame() {
        Gdx.app.log("Tron3DGame", "Iniciando juego directo")
        setScreen(GameScreen(this, gameViewModel))
    }

    /**
     * Inicia partida multijugador local
     */
    fun startLocalMultiplayer() {
        Gdx.app.log("Tron3DGame", "Iniciando multijugador local")
        setScreen(GameScreen(this, gameViewModel))
    }

    /**
     * Inicia partida un jugador (vs IA)
     */
    fun startSinglePlayer() {
        Gdx.app.log("Tron3DGame", "Un jugador aún no disponible")
        // TODO: Implementar modo un jugador
        // Por ahora inicia multijugador local
        startLocalMultiplayer()
    }

    /**
     * Inicia partida multijugador por Bluetooth
     */
    fun startBluetoothMultiplayer() {
        Gdx.app.log("Tron3DGame", "Bluetooth aún no disponible")
        // TODO: Implementar bluetooth
        // Por ahora inicia multijugador local
        startLocalMultiplayer()
    }

    override fun dispose() {
        super.dispose()
        screen?.dispose()
        Gdx.app.log("Tron3DGame", "Juego cerrado")
    }
}
