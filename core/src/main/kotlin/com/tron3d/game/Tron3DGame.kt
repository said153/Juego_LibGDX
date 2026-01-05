// core/src/main/kotlin/com/tron3d/game/Tron3DGame.kt
package com.tron3d.game

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.tron3d.ai.AIDifficulty
import com.tron3d.network.BluetoothInterface
import com.tron3d.ui.BluetoothSelectionScreen
import com.tron3d.viewmodel.GameViewModel
import com.tron3d.viewmodel.GameViewModel_Bluetooth

/**
 * Clase principal del juego TRON 3D con soporte Bluetooth Y modo 1 jugador
 * ✅ SOPORTE COMPLETO PARA IA
 */
class Tron3DGame(private val bluetoothManager: BluetoothInterface? = null) : Game() {

    private lateinit var gameViewModel: GameViewModel
    private var bluetoothViewModel: GameViewModel_Bluetooth? = null

    override fun create() {
        Gdx.app.log("Tron3DGame", "Iniciando juego TRON 3D")
        Gdx.app.log("Tron3DGame", "Bluetooth disponible: ${bluetoothManager != null}")

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
     * ✅ NUEVO: Muestra la pantalla de selección de dificultad para 1 jugador
     */
    fun showDifficultySelection() {
        Gdx.app.log("Tron3DGame", "Mostrando selección de dificultad")
        setScreen(AIDifficultyScreen(this))
    }

    /**
     * Muestra la pantalla de selección de modo (multijugador)
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
     * ✅ NUEVO: Inicia partida un jugador (vs IA) con dificultad seleccionada
     */
    fun startSinglePlayer(difficulty: AIDifficulty) {
        Gdx.app.log("Tron3DGame", "")
        setScreen(GameScreen(
            game = this,
            gameViewModel = gameViewModel,
            isSinglePlayer = true,  // ✅ NUEVO PARÁMETRO
            aiDifficulty = difficulty  // ✅ NUEVO PARÁMETRO
        ))
    }

    /**
     * Muestra la pantalla de selección Bluetooth
     */
    fun startBluetoothMultiplayer() {
        Gdx.app.log("Tron3DGame", "Iniciando selección Bluetooth")

        if (bluetoothManager == null) {
            Gdx.app.error("Tron3DGame", "BluetoothManager no disponible")
            showBluetoothError()
            return
        }

        val bluetoothScreen = BluetoothSelectionScreen(
            game = this,
            bluetoothManager = bluetoothManager,
            onHostConnected = {
                // Host conectado, iniciar juego
                startBluetoothGame(isHost = true)
            },
            onClientConnected = {
                // Cliente conectado, iniciar juego
                startBluetoothGame(isHost = false)
            },
            onBack = {
                // Volver a selección de modo
                showModeSelection()
            }
        )

        setScreen(bluetoothScreen)
    }

    /**
     * Inicia el juego con Bluetooth y sincronización completa
     */
    private fun startBluetoothGame(isHost: Boolean) {
        Gdx.app.log("Tron3DGame", "🎮 Iniciando juego Bluetooth (Host: $isHost)")

        if (bluetoothManager == null) {
            Gdx.app.error("Tron3DGame", "❌ BluetoothManager es null!")
            showModeSelection()
            return
        }

        Gdx.app.log("Tron3DGame", "✅ Iniciando con sincronización Bluetooth")

        // Iniciar juego con Bluetooth Y sincronización
        setScreen(GameScreen(
            game = this,
            gameViewModel = gameViewModel,
            isBluetooth = true,
            isHost = isHost,
            bluetoothManager = bluetoothManager
        ))
    }

    /**
     * Muestra mensaje de error cuando Bluetooth no está disponible
     */
    private fun showBluetoothError() {
        Gdx.app.postRunnable {
            // Volver al menú de selección de modo
            showModeSelection()

            // Aquí podrías mostrar un mensaje en la UI indicando que Bluetooth no está disponible
            // dependiendo de cómo esté implementada tu UI
            Gdx.app.log("Tron3DGame", "Bluetooth no disponible - mostrando modo local")
        }
    }

    /**
     * Verifica si Bluetooth está disponible
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothManager != null
    }

    override fun dispose() {
        super.dispose()
        screen?.dispose()
        bluetoothViewModel?.dispose()
        bluetoothManager?.disconnect()
        Gdx.app.log("Tron3DGame", "Juego cerrado")
    }
}
