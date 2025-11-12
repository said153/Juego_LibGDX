// core/src/main/kotlin/com/tron3d/ui/BluetoothSelectionScreen.kt
package com.tron3d.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Array as GdxArray
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.tron3d.network.BluetoothInterface
import com.tron3d.network.BluetoothDeviceInfo

/**
 * Pantalla de selección de Bluetooth usando LibGDX Scene2D
 * Compatible con el juego Tron 3D - VERSIÓN MEJORADA
 */
class BluetoothSelectionScreen(
    private val game: com.badlogic.gdx.Game,
    private val bluetoothManager: BluetoothInterface?,
    private val onHostConnected: () -> Unit,
    private val onClientConnected: () -> Unit,
    private val onBack: () -> Unit
) : Screen {

    private val stage = Stage(ScreenViewport())
    private val skin = Skin()
    private val mainTable = Table()

    private var currentMode = BluetoothMode.SELECTION
    private var statusLabel: Label? = null

    // Lista de dispositivos Bluetooth
    private val pairedDevices = mutableListOf<BluetoothDeviceInfo>()

    enum class BluetoothMode {
        SELECTION,  // Elegir Host o Client
        HOST,       // Esperando conexión
        CLIENT      // Seleccionar dispositivo
    }

    init {
        setupSkin()
        setupUI()
        Gdx.input.inputProcessor = stage
    }

    private fun setupSkin() {
        // Crear fuentes
        val font = BitmapFont()
        font.data.setScale(2f)
        skin.add("default-font", font)

        val titleFont = BitmapFont()
        titleFont.data.setScale(3f)
        skin.add("title-font", titleFont)

        // Crear pixel blanco
        val pixmap = com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.fill()
        skin.add("white", com.badlogic.gdx.graphics.Texture(pixmap))
        pixmap.dispose()

        // Label styles
        val labelStyle = Label.LabelStyle()
        labelStyle.font = font
        labelStyle.fontColor = Color.CYAN
        skin.add("default", labelStyle)

        val titleStyle = Label.LabelStyle()
        titleStyle.font = titleFont
        titleStyle.fontColor = Color(0f, 1f, 1f, 1f)
        skin.add("title", titleStyle)

        val statusStyle = Label.LabelStyle()
        statusStyle.font = font
        statusStyle.fontColor = Color.ORANGE
        skin.add("status", statusStyle)

        // TextButton style
        val buttonStyle = TextButton.TextButtonStyle()
        buttonStyle.font = font
        buttonStyle.fontColor = Color.CYAN
        buttonStyle.overFontColor = Color.WHITE
        buttonStyle.downFontColor = Color.ORANGE
        buttonStyle.up = skin.newDrawable("white", Color(0f, 0.2f, 0.3f, 0.8f))
        buttonStyle.over = skin.newDrawable("white", Color(0f, 0.4f, 0.5f, 0.9f))
        buttonStyle.down = skin.newDrawable("white", Color(1f, 0.5f, 0f, 0.9f))
        skin.add("default", buttonStyle)

        // List style mejorado
        val listStyle = com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle()
        listStyle.font = font
        listStyle.fontColorSelected = Color.ORANGE
        listStyle.fontColorUnselected = Color.CYAN
        listStyle.selection = skin.newDrawable("white", Color(0f, 0.5f, 0.7f, 0.8f))
        listStyle.background = skin.newDrawable("white", Color(0f, 0f, 0.2f, 0.6f))
        skin.add("default", listStyle)

        // ScrollPane style
        val scrollStyle = ScrollPane.ScrollPaneStyle()
        scrollStyle.background = skin.newDrawable("white", Color(0f, 0f, 0.1f, 0.7f))
        skin.add("default", scrollStyle)
    }

    private fun setupUI() {
        mainTable.setFillParent(true)
        mainTable.center()
        stage.addActor(mainTable)

        showModeSelection()
    }

    private fun showModeSelection() {
        mainTable.clear()
        currentMode = BluetoothMode.SELECTION

        val title = Label("BLUETOOTH MULTIPLAYER", skin, "title")
        mainTable.add(title).padBottom(80f).colspan(2).row()

        val hostButton = TextButton("CREAR PARTIDA (HOST)", skin)
        hostButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                startAsHost()
            }
        })
        mainTable.add(hostButton).width(500f).height(80f).padBottom(30f).colspan(2).row()

        val clientButton = TextButton("UNIRSE (CLIENT)", skin)
        clientButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                showDeviceList()
            }
        })
        mainTable.add(clientButton).width(500f).height(80f).padBottom(30f).colspan(2).row()

        val backButton = TextButton("VOLVER", skin)
        backButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                onBack()
            }
        })
        mainTable.add(backButton).width(300f).height(70f).padTop(50f).colspan(2).row()
    }

    private fun startAsHost() {
        mainTable.clear()
        currentMode = BluetoothMode.HOST

        val title = Label("MODO HOST", skin, "title")
        mainTable.add(title).padBottom(60f).row()

        statusLabel = Label("Iniciando servidor...", skin, "status")
        mainTable.add(statusLabel).padBottom(40f).row()

        val info = Label("Esperando que un jugador\nse conecte desde otro dispositivo", skin)
        info.setAlignment(com.badlogic.gdx.utils.Align.center)
        mainTable.add(info).padBottom(60f).row()

        val cancelButton = TextButton("CANCELAR", skin)
        cancelButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                bluetoothManager?.disconnect()
                showModeSelection()
            }
        })
        mainTable.add(cancelButton).width(300f).height(70f).row()

        // Iniciar servidor Bluetooth
        Gdx.app.log("BluetoothScreen", "Iniciando servidor...")
        bluetoothManager?.startServer { success ->
            Gdx.app.log("BluetoothScreen", "Callback servidor: success=$success")
            Gdx.app.postRunnable {
                if (success) {
                    statusLabel?.setText("¡CLIENTE CONECTADO!")
                    Gdx.app.log("BluetoothScreen", "Cliente conectado, iniciando juego en 1s...")

                    // Esperar 1 segundo y navegar al juego
                    Thread {
                        Thread.sleep(1000)
                        Gdx.app.postRunnable {
                            Gdx.app.log("BluetoothScreen", "Llamando onHostConnected()")
                            onHostConnected()
                        }
                    }.start()
                } else {
                    Gdx.app.log("BluetoothScreen", "ERROR: Servidor falló")
                    statusLabel?.setText("ERROR: No se pudo iniciar el servidor")
                }
            }
        }
    }

    /**
     * Mostrar lista de dispositivos Bluetooth MEJORADA
     */
    private fun showDeviceList() {
        mainTable.clear()
        currentMode = BluetoothMode.CLIENT

        val title = Label("SELECCIONAR DISPOSITIVO", skin, "title")
        mainTable.add(title).padBottom(40f).colspan(2).row()

        loadPairedDevices()

        if (pairedDevices.isEmpty()) {
            val noDevices = Label("No hay dispositivos\nvinculados", skin, "status")
            noDevices.setAlignment(com.badlogic.gdx.utils.Align.center)
            mainTable.add(noDevices).padBottom(40f).colspan(2).row()

            val hint = Label("Vincula dispositivos Bluetooth\ndesde la configuracion de Android", skin)
            hint.setAlignment(com.badlogic.gdx.utils.Align.center)
            mainTable.add(hint).padBottom(40f).colspan(2).row()
        } else {
            // Crear lista SOLO con nombres (sin dirección para evitar encimado)
            val deviceNames = GdxArray<String>()
            pairedDevices.forEach { device ->
                deviceNames.add(device.name)
            }

            val list = com.badlogic.gdx.scenes.scene2d.ui.List<String>(skin)
            list.setItems(deviceNames)
            if (deviceNames.size > 0) {
                list.setSelected(deviceNames.first())
            }

            val scrollPane = ScrollPane(list, skin)
            scrollPane.setFadeScrollBars(false)
            scrollPane.setScrollingDisabled(true, false)

            // Altura dinámica según cantidad de dispositivos
            val listHeight = Math.min(deviceNames.size * 80f, 400f)
            mainTable.add(scrollPane).width(600f).height(listHeight).padBottom(20f).colspan(2).row()

            // Status - mostrar dirección del seleccionado
            statusLabel = Label("", skin, "status")
            updateSelectedDeviceInfo(list, 0)
            mainTable.add(statusLabel).padBottom(30f).colspan(2).row()

            // Listener para actualizar info al cambiar selección
            list.addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: com.badlogic.gdx.scenes.scene2d.Actor?) {
                    updateSelectedDeviceInfo(list, list.selectedIndex)
                }
            })

            val connectButton = TextButton("CONECTAR", skin)
            connectButton.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    val selectedIndex = list.selectedIndex
                    if (selectedIndex >= 0 && selectedIndex < pairedDevices.size) {
                        connectToDevice(pairedDevices[selectedIndex])
                    }
                }
            })
            mainTable.add(connectButton).width(250f).height(70f).padRight(20f)
        }

        val backButton = TextButton("VOLVER", skin)
        backButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                showModeSelection()
            }
        })
        mainTable.add(backButton).width(250f).height(70f).row()
    }

    /**
     * Actualizar información del dispositivo seleccionado
     */
    private fun updateSelectedDeviceInfo(list: com.badlogic.gdx.scenes.scene2d.ui.List<String>, index: Int) {
        if (index >= 0 && index < pairedDevices.size) {
            val device = pairedDevices[index]
            statusLabel?.setText("MAC: ${device.address}")
        }
    }

    private fun loadPairedDevices() {
        pairedDevices.clear()

        try {
            bluetoothManager?.let { manager ->
                val devices = manager.getPairedDevices()
                pairedDevices.addAll(devices)
            }
        } catch (e: Exception) {
            Gdx.app.log("Bluetooth", "Error obteniendo dispositivos: ${e.message}")
        }
    }

    private fun connectToDevice(device: BluetoothDeviceInfo) {
        statusLabel?.setText("Conectando...")

        try {
            bluetoothManager?.connectToDevice(device) { success ->
                Gdx.app.postRunnable {
                    if (success) {
                        statusLabel?.setText("¡CONECTADO!")
                        Thread {
                            Thread.sleep(1000)
                            Gdx.app.postRunnable {
                                onClientConnected()
                            }
                        }.start()
                    } else {
                        statusLabel?.setText("ERROR: No se pudo conectar")
                    }
                }
            }
        } catch (e: Exception) {
            Gdx.app.postRunnable {
                statusLabel?.setText("ERROR: ${e.message}")
            }
        }
    }

    override fun show() {
        Gdx.input.inputProcessor = stage
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0.05f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun pause() {}
    override fun resume() {}
    override fun hide() {}

    override fun dispose() {
        stage.dispose()
        skin.dispose()
    }
}
