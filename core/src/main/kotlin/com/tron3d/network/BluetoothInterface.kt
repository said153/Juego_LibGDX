// core/src/main/kotlin/com/tron3d/network/BluetoothInterface.kt
package com.tron3d.network

import com.tron3d.models.Direction

/**
 * Interfaz multiplataforma para Bluetooth
 * CON SOPORTE PARA MENSAJES DE TEXTO (sincronización)
 */
interface BluetoothInterface {

    // Conexión básica
    fun startServer(onConnectionEstablished: (success: Boolean) -> Unit)
    fun connectToDevice(device: BluetoothDeviceInfo, onConnectionEstablished: (success: Boolean) -> Unit)
    fun getPairedDevices(): List<BluetoothDeviceInfo>
    fun disconnect()
    fun isConnected(): Boolean

    // Envío de datos raw
    fun sendData(data: ByteArray)
    fun receiveData(onDataReceived: (data: ByteArray) -> Unit)

    // ✅ NUEVOS: Métodos para sincronización de texto
    fun sendMessage(message: String)
    fun setOnMessageReceived(callback: (String) -> Unit)

    // Métodos de alto nivel para el juego
    fun sendMove(direction: Direction, playerId: Int)
    fun sendPause()
    fun sendResume()
    fun sendResetGame()

    // Listeners
    var messageListener: ((NetworkMessage) -> Unit)?
    var connectionListener: ((Boolean) -> Unit)?

    // Estado
    val isHost: Boolean
}
