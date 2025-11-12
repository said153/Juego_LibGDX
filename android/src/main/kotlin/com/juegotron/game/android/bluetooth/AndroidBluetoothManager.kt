// android/src/main/kotlin/com/juegotron/game/android/bluetooth/AndroidBluetoothManager.kt
package com.juegotron.game.android.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.badlogic.gdx.Gdx
import com.tron3d.models.Direction
import com.tron3d.network.BluetoothDeviceInfo
import com.tron3d.network.BluetoothInterface
import com.tron3d.network.MessageType
import com.tron3d.network.NetworkMessage
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.*

/**
 * Implementación Android de la interfaz Bluetooth para LibGDX
 * CON SOPORTE PARA MENSAJES DE TEXTO (sincronización)
 */
@SuppressLint("MissingPermission")
class AndroidBluetoothManager : BluetoothInterface {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    private var receiveThread: Thread? = null

    // Listeners
    override var messageListener: ((NetworkMessage) -> Unit)? = null
    override var connectionListener: ((Boolean) -> Unit)? = null

    // ✅ NUEVO: Listener para mensajes de texto
    private var textMessageCallback: ((String) -> Unit)? = null

    // Estado
    override var isHost: Boolean = false
        private set

    companion object {
        private const val TAG = "BluetoothManager"
        private const val NAME = "Tron3D_Game"
        private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    override fun startServer(onConnectionEstablished: (success: Boolean) -> Unit) {
        isHost = true

        Thread {
            try {
                Log.d(TAG, "Iniciando servidor Bluetooth...")

                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(NAME, MY_UUID)
                Log.d(TAG, "Servidor esperando conexiones...")

                clientSocket = serverSocket?.accept()

                if (clientSocket != null) {
                    Log.d(TAG, "Cliente conectado!")
                    outputStream = clientSocket?.outputStream
                    inputStream = clientSocket?.inputStream
                    serverSocket?.close()

                    startReceiving()

                    Gdx.app.postRunnable {
                        connectionListener?.invoke(true)
                    }
                    onConnectionEstablished(true)
                } else {
                    Log.e(TAG, "Error: socket nulo")
                    onConnectionEstablished(false)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error en servidor: ${e.message}")
                e.printStackTrace()
                onConnectionEstablished(false)
            }
        }.start()
    }

    override fun connectToDevice(device: BluetoothDeviceInfo, onConnectionEstablished: (success: Boolean) -> Unit) {
        isHost = false

        Thread {
            try {
                Log.d(TAG, "Conectando a ${device.name} (${device.address})...")

                val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
                bluetoothAdapter?.cancelDiscovery()

                clientSocket = bluetoothDevice?.createRfcommSocketToServiceRecord(MY_UUID)
                clientSocket?.connect()

                Log.d(TAG, "Conectado exitosamente!")

                outputStream = clientSocket?.outputStream
                inputStream = clientSocket?.inputStream

                startReceiving()

                Gdx.app.postRunnable {
                    connectionListener?.invoke(true)
                }
                onConnectionEstablished(true)
            } catch (e: IOException) {
                Log.e(TAG, "Error al conectar: ${e.message}")
                e.printStackTrace()
                onConnectionEstablished(false)
            }
        }.start()
    }

    override fun getPairedDevices(): List<BluetoothDeviceInfo> {
        val devices = mutableListOf<BluetoothDeviceInfo>()

        try {
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            pairedDevices?.forEach { device ->
                devices.add(BluetoothDeviceInfo(
                    name = device.name ?: "Dispositivo desconocido",
                    address = device.address
                ))
            }
            Log.d(TAG, "Encontrados ${devices.size} dispositivos emparejados")
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo dispositivos: ${e.message}")
            e.printStackTrace()
        }

        return devices
    }

    override fun sendData(data: ByteArray) {
        try {
            outputStream?.write(data)
            outputStream?.flush()
            Log.d(TAG, "Datos enviados: ${data.size} bytes")
        } catch (e: IOException) {
            Log.e(TAG, "Error enviando datos: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun receiveData(onDataReceived: (data: ByteArray) -> Unit) {
        // Este método se llama automáticamente en startReceiving()
    }

    // ✅ NUEVO: Enviar mensaje de texto
    override fun sendMessage(message: String) {
        try {
            val data = message.toByteArray(Charsets.UTF_8)
            // Enviar longitud primero (4 bytes) y luego el mensaje
            val lengthBytes = ByteBuffer.allocate(4).putInt(data.size).array()

            outputStream?.write(lengthBytes)
            outputStream?.write(data)
            outputStream?.flush()

            Log.d(TAG, "📤 Mensaje enviado: ${message.take(50)}...")
        } catch (e: IOException) {
            Log.e(TAG, "Error enviando mensaje: ${e.message}")
            e.printStackTrace()
        }
    }

    // ✅ NUEVO: Configurar callback para mensajes de texto
    override fun setOnMessageReceived(callback: (String) -> Unit) {
        textMessageCallback = callback
        Log.d(TAG, "✅ Callback de mensajes configurado")
    }

    private fun startReceiving() {
        receiveThread?.interrupt()

        receiveThread = Thread {
            val buffer = ByteArray(4096)

            while (!Thread.currentThread().isInterrupted && isConnected()) {
                try {
                    // Primero leer longitud del mensaje (4 bytes)
                    val lengthBytes = ByteArray(4)
                    var totalRead = 0

                    while (totalRead < 4) {
                        val bytesRead = inputStream?.read(lengthBytes, totalRead, 4 - totalRead) ?: -1
                        if (bytesRead == -1) {
                            Log.w(TAG, "Conexión cerrada")
                            Gdx.app.postRunnable {
                                connectionListener?.invoke(false)
                            }
                            return@Thread
                        }
                        totalRead += bytesRead
                    }

                    val messageLength = ByteBuffer.wrap(lengthBytes).getInt()

                    if (messageLength <= 0 || messageLength > 10000) {
                        Log.e(TAG, "Longitud de mensaje inválida: $messageLength")
                        continue
                    }

                    // Leer el mensaje completo
                    val messageBytes = ByteArray(messageLength)
                    totalRead = 0

                    while (totalRead < messageLength) {
                        val bytesRead = inputStream?.read(messageBytes, totalRead, messageLength - totalRead) ?: -1
                        if (bytesRead == -1) {
                            Log.w(TAG, "Conexión cerrada durante lectura")
                            Gdx.app.postRunnable {
                                connectionListener?.invoke(false)
                            }
                            return@Thread
                        }
                        totalRead += bytesRead
                    }

                    // Convertir a String y notificar
                    val message = String(messageBytes, Charsets.UTF_8)
                    Log.d(TAG, "📥 Mensaje recibido: ${message.take(50)}...")

                    Gdx.app.postRunnable {
                        textMessageCallback?.invoke(message)
                    }

                } catch (e: IOException) {
                    Log.e(TAG, "Error recibiendo datos: ${e.message}")
                    Gdx.app.postRunnable {
                        connectionListener?.invoke(false)
                    }
                    break
                }
            }
        }

        receiveThread?.start()
    }

    override fun disconnect() {
        try {
            Log.d(TAG, "Desconectando...")
            receiveThread?.interrupt()
            outputStream?.close()
            inputStream?.close()
            clientSocket?.close()
            serverSocket?.close()

            Gdx.app.postRunnable {
                connectionListener?.invoke(false)
            }

            Log.d(TAG, "Desconectado correctamente")
        } catch (e: IOException) {
            Log.e(TAG, "Error al desconectar: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun isConnected(): Boolean {
        return clientSocket?.isConnected ?: false
    }

    // Métodos de alto nivel para el juego
    override fun sendMove(direction: Direction, playerId: Int) {
        val message = NetworkMessage(
            type = MessageType.MOVE,
            playerId = playerId,
            direction = direction
        )
        sendNetworkMessage(message)
    }

    override fun sendPause() {
        val message = NetworkMessage(type = MessageType.PAUSE)
        sendNetworkMessage(message)
    }

    override fun sendResume() {
        val message = NetworkMessage(type = MessageType.RESUME)
        sendNetworkMessage(message)
    }

    override fun sendResetGame() {
        val message = NetworkMessage(type = MessageType.RESET_GAME)
        sendNetworkMessage(message)
    }

    /**
     * Envía un NetworkMessage codificado (mantiene compatibilidad)
     */
    private fun sendNetworkMessage(message: NetworkMessage) {
        val data = encodeMessage(message)
        sendData(data)
    }

    /**
     * Codifica un NetworkMessage a ByteArray
     */
    private fun encodeMessage(message: NetworkMessage): ByteArray {
        val buffer = ByteBuffer.allocate(14)
        buffer.put(message.type.ordinal.toByte())
        buffer.putInt(message.playerId)
        val directionByte = message.direction?.ordinal?.toByte() ?: (-1).toByte()
        buffer.put(directionByte)
        buffer.putLong(message.timestamp)
        return buffer.array()
    }

    /**
     * Decodifica un ByteArray a NetworkMessage
     */
    private fun decodeMessage(data: ByteArray): NetworkMessage? {
        if (data.size < 14) {
            Log.e(TAG, "Datos insuficientes para decodificar mensaje")
            return null
        }

        try {
            val buffer = ByteBuffer.wrap(data)
            val typeOrdinal = buffer.get().toInt()
            val type = MessageType.values().getOrNull(typeOrdinal) ?: return null
            val playerId = buffer.getInt()
            val directionByte = buffer.get().toInt()
            val direction = if (directionByte >= 0) {
                Direction.values().getOrNull(directionByte)
            } else {
                null
            }
            val timestamp = buffer.getLong()

            return NetworkMessage(
                type = type,
                playerId = playerId,
                direction = direction,
                timestamp = timestamp
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error decodificando mensaje: ${e.message}")
            return null
        }
    }
}
