// android/src/main/kotlin/com/juegotron/game/android/AndroidLauncher.kt
package com.juegotron.game.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.juegotron.game.android.bluetooth.AndroidBluetoothManager
import com.tron3d.game.Tron3DGame

class AndroidLauncher : AndroidApplication() {

    private val PERMISSION_REQUEST_CODE = 1001
    private lateinit var bluetoothManager: AndroidBluetoothManager
    private lateinit var config: AndroidApplicationConfiguration
    private var gameInitialized = false
    private var waitingForPermissions = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si ya está inicializado, no hacer nada
        if (gameInitialized) {
            return
        }

        config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useGyroscope = false
            useImmersiveMode = true
        }

        bluetoothManager = AndroidBluetoothManager()

        // Si estamos volviendo de la solicitud de permisos, inicializar directamente
        if (waitingForPermissions) {
            initializeGame()
            waitingForPermissions = false
            return
        }

        if (needsBluetoothPermissions()) {
            waitingForPermissions = true
            requestBluetoothPermissions()
        } else {
            initializeGame()
        }
    }

    private fun needsBluetoothPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
        } else {
            // API < 23 no necesita permisos en runtime
            return false
        }
    }

    private fun requestBluetoothPermissions() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                    )
                } else {
                    arrayOf(
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN
                    )
                }

                requestPermissions(permissions, PERMISSION_REQUEST_CODE)
            } else {
                // API < 23: permisos automáticamente concedidos
                initializeGame()
            }
        } catch (e: Exception) {
            android.util.Log.e("AndroidLauncher", "Error solicitando permisos", e)
            // Si hay error, inicializar sin Bluetooth
            initializeGameWithoutBluetooth()
        }
    }

    private fun initializeGame() {
        if (gameInitialized) return

        try {
            android.util.Log.d("AndroidLauncher", "Inicializando juego con Bluetooth")
            val game = Tron3DGame(bluetoothManager)
            initialize(game, config)
            gameInitialized = true
        } catch (e: Exception) {
            android.util.Log.e("AndroidLauncher", "Error inicializando con Bluetooth", e)
            initializeGameWithoutBluetooth()
        }
    }

    private fun initializeGameWithoutBluetooth() {
        if (gameInitialized) return

        try {
            android.util.Log.d("AndroidLauncher", "Inicializando juego sin Bluetooth")
            val game = Tron3DGame(null)
            initialize(game, config)
            gameInitialized = true
        } catch (e: Exception) {
            android.util.Log.e("AndroidLauncher", "Error crítico inicializando juego", e)
            // Si falla incluso sin Bluetooth, mostrar error y cerrar
            throw e
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                waitingForPermissions = false

                val allGranted = grantResults.isNotEmpty() &&
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }

                if (allGranted) {
                    android.util.Log.d("AndroidLauncher", "Permisos concedidos - inicializando juego")
                    // Usar postRunnable para asegurar que se ejecute en el hilo principal
                    handler.post {
                        if (!gameInitialized) {
                            initializeGame()
                        }
                    }
                } else {
                    android.util.Log.w("AndroidLauncher", "Permisos denegados - inicializando sin Bluetooth")
                    handler.post {
                        if (!gameInitialized) {
                            initializeGameWithoutBluetooth()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gameInitialized = false
        waitingForPermissions = false
    }
}
