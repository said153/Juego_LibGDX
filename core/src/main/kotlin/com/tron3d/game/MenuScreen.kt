package com.tron3d.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.JsonReader
import com.tron3d.config.TronVisualConfig
import com.badlogic.gdx.utils.UBJsonReader

/**
 * MenuScreen con DEBUG detallado para logo 3D
 */
class MenuScreen(private val game: Tron3DGame) : Screen {

    private val spriteBatch: SpriteBatch = SpriteBatch()
    private val font: BitmapFont = BitmapFont()
    private val titleFont: BitmapFont = BitmapFont()
    private val shapeRenderer: ShapeRenderer = ShapeRenderer()

    private var camera3D: PerspectiveCamera? = null
    private var modelBatch: ModelBatch? = null
    private var environment: Environment? = null
    private var logoModel: Model? = null
    private var logoInstance: ModelInstance? = null
    private var logoRotation = 0f
    private var hasLogo3D = false
    private var errorMessage = ""

    private val tronCyan = TronVisualConfig.NeonColors.CYAN
    private val tronOrange = TronVisualConfig.NeonColors.ORANGE

    private val buttons = mutableListOf<MenuButton>()

    private val buttonWidth = 400f
    private val buttonHeight = 80f
    private val buttonSpacing = 100f

    init {
        font.color = tronCyan
        font.data.setScale(2f)

        titleFont.color = tronCyan
        titleFont.data.setScale(4f)

        setupButtons()
    }

    private fun setupButtons() {
        val centerX = Gdx.graphics.width / 2f
        val startY = Gdx.graphics.height / 2f

        buttons.clear()
        buttons.add(MenuButton("MULTIJUGADOR LOCAL", centerX, startY, buttonWidth, buttonHeight))
        buttons.add(MenuButton("UN JUGADOR (IA)", centerX, startY - buttonSpacing, buttonWidth, buttonHeight))
        buttons.add(MenuButton("BLUETOOTH", centerX, startY - buttonSpacing * 2, buttonWidth, buttonHeight))
        buttons.add(MenuButton("SALIR", centerX, startY - buttonSpacing * 3, buttonWidth, buttonHeight))
    }

    override fun show() {
        Gdx.app.log("MenuScreen", "====== INICIANDO MENU ======")

        // Verificar si el archivo existe
        val fileExists = Gdx.files.internal("models/tron_logo.g3db").exists()
        Gdx.app.log("MenuScreen", "Archivo existe: $fileExists")

        if (fileExists) {
            val fileSize = Gdx.files.internal("models/tron_logo.g3db").length()
            Gdx.app.log("MenuScreen", "Tamaño del archivo: $fileSize bytes")
        }

        // Intentar cargar logo 3D
        try {
            setupLogo3D()
            loadLogo3D()
        } catch (e: Exception) {
            errorMessage = e.message ?: "Error desconocido"
            Gdx.app.error("MenuScreen", "❌ FALLO COMPLETO: $errorMessage")
            e.printStackTrace()
            hasLogo3D = false
        }

        Gdx.app.log("MenuScreen", "====== MENU LISTO (Logo 3D: $hasLogo3D) ======")
    }

    private fun setupLogo3D() {
        Gdx.app.log("MenuScreen", "Configurando cámara 3D...")
        camera3D = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera3D?.position?.set(0f, 0f, 50f)  // ⬅️ Cambia a (0f, 0f, 50f) para verlo de frente y más lejos
        camera3D?.lookAt(0f, 0f, 0f)
        camera3D?.near = 0.1f
        camera3D?.far = 300f
        camera3D?.update()

        modelBatch = ModelBatch()

        environment = Environment()
        environment?.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.6f, 1f))

        val mainLight = DirectionalLight()
        mainLight.set(tronCyan, -0.5f, -1f, -0.8f)
        environment?.add(mainLight)

        val accentLight = DirectionalLight()
        accentLight.set(tronOrange, 0.5f, -0.5f, 0.5f)
        environment?.add(accentLight)

        Gdx.app.log("MenuScreen", "✅ Cámara 3D configurada")
    }

    private fun loadLogo3D() {
        Gdx.app.log("MenuScreen", "Intentando cargar logo 3D...")

        try {

            // ✅ CORRECTO - Usar UBJsonReader para .g3db (binario)
            val loader = G3dModelLoader(com.badlogic.gdx.utils.UBJsonReader())
            Gdx.app.log("MenuScreen", "Loader creado (UBJson)")

            logoModel = loader.loadModel(Gdx.files.internal("models/tron_logo.g3db"))
            Gdx.app.log("MenuScreen", "Modelo cargado")

            logoInstance = ModelInstance(logoModel)
            Gdx.app.log("MenuScreen", "Instancia creada")

            // Aplicar color neón
            var materialCount = 0
            logoInstance?.materials?.forEach { material ->
                materialCount++
                material.set(ColorAttribute.createEmissive(tronCyan))
                material.set(ColorAttribute.createDiffuse(tronCyan))
            }
            Gdx.app.log("MenuScreen", "Materiales aplicados: $materialCount")

            // Posicionar y escalar
            logoInstance?.transform?.setToTranslation(0f, 0f, 0f)
            logoInstance?.transform?.scale(0.3f, 0.3f, 0.3f)
            Gdx.app.log("MenuScreen", "Transform aplicado")

            hasLogo3D = true
            Gdx.app.log("MenuScreen", "✅✅✅ Logo 3D cargado EXITOSAMENTE ✅✅✅")
        } catch (e: Exception) {
            hasLogo3D = false
            errorMessage = e.message ?: "Sin mensaje de error"
            Gdx.app.error("MenuScreen", "❌❌❌ ERROR CARGANDO LOGO 3D ❌❌❌")
            Gdx.app.error("MenuScreen", "Tipo de error: ${e.javaClass.simpleName}")
            Gdx.app.error("MenuScreen", "Mensaje: $errorMessage")
            e.printStackTrace()
        }
    }

    override fun render(delta: Float) {
        handleInput()

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        if (hasLogo3D && logoModel != null && logoInstance != null) {
            renderLogo3D()
        } else {
            renderTitleText()
            renderErrorMessage()
        }

        renderButtons()
        renderSubtitle()
    }

    private fun renderLogo3D() {
        try {
            val logoHeight = 300
            val logoWidth = 350
            val topMargin = 50  // ⬅️ AGREGA ESTO (margen superior en píxeles)

            // Calcula la posición X para centrarlo
            val logoX = (Gdx.graphics.width - logoWidth) / 2

            Gdx.gl.glViewport(
                logoX.toInt(),
                Gdx.graphics.height - logoHeight - topMargin,  // ⬅️ RESTA EL MARGEN
                logoWidth,
                logoHeight
            )

            modelBatch?.begin(camera3D)
            modelBatch?.render(logoInstance, environment)
            modelBatch?.end()

            // Restaura el viewport completo
            Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        } catch (e: Exception) {
            Gdx.app.error("MenuScreen", "Error renderizando: ${e.message}")
            hasLogo3D = false
        }
    }

    private fun renderTitleText() {
        spriteBatch.begin()
        titleFont.color = tronCyan
        titleFont.data.setScale(5f)

        val titleText = "TRON"
        val layout = com.badlogic.gdx.graphics.g2d.GlyphLayout(titleFont, titleText)
        val titleX = (Gdx.graphics.width - layout.width) / 2f
        val titleY = Gdx.graphics.height - 100f

        titleFont.draw(spriteBatch, titleText, titleX, titleY)
        spriteBatch.end()
    }

    private fun renderErrorMessage() {
        if (errorMessage.isNotEmpty()) {
            spriteBatch.begin()
            font.data.setScale(0.8f)
            font.color = Color.RED

            val errorText = "Logo 3D no cargado"
            val layout = com.badlogic.gdx.graphics.g2d.GlyphLayout(font, errorText)
            val errorX = (Gdx.graphics.width - layout.width) / 2f
            val errorY = Gdx.graphics.height - 200f

            font.draw(spriteBatch, errorText, errorX, errorY)
            font.draw(spriteBatch, "Ver Logcat para detalles", errorX - 100f, errorY - 30f)

            spriteBatch.end()
        }
    }

    private fun renderSubtitle() {
        spriteBatch.begin()
        font.data.setScale(1.2f)
        font.color = tronOrange

        val subtitle = ""
        val layout = com.badlogic.gdx.graphics.g2d.GlyphLayout(font, subtitle)
        val subtitleX = (Gdx.graphics.width - layout.width) / 2f
        val subtitleY = if (hasLogo3D) {
            val topMargin = 50  // ⬅️ MISMO VALOR
            val logoHeight = 100  // ⬅️ MISMO VALOR
            Gdx.graphics.height - logoHeight - topMargin - 20f  // -20f para separación extra
        } else {
            Gdx.graphics.height - 250f
        }

        font.draw(spriteBatch, subtitle, subtitleX, subtitleY)
        spriteBatch.end()
    }

    private fun renderButtons() {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        buttons.forEach { button ->
            shapeRenderer.color = if (button.isHovered) {
                Color(0.2f, 0.6f, 0.9f, 0.7f)
            } else {
                Color(0.05f, 0.15f, 0.3f, 0.5f)
            }
            shapeRenderer.rect(
                button.x - button.width / 2f,
                button.y - button.height / 2f,
                button.width,
                button.height
            )
        }
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glLineWidth(4f)
        buttons.forEach { button ->
            shapeRenderer.color = if (button.isHovered) {
                Color.WHITE
            } else {
                tronCyan
            }
            shapeRenderer.rect(
                button.x - button.width / 2f,
                button.y - button.height / 2f,
                button.width,
                button.height
            )
        }
        shapeRenderer.end()

        spriteBatch.begin()
        font.data.setScale(1.8f)
        buttons.forEach { button ->
            font.color = if (button.isHovered) {
                Color.WHITE
            } else {
                tronCyan
            }

            val layout = com.badlogic.gdx.graphics.g2d.GlyphLayout(font, button.text)
            val textX = button.x - layout.width / 2f
            val textY = button.y + layout.height / 2f

            font.draw(spriteBatch, button.text, textX, textY)
        }
        spriteBatch.end()

        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun handleInput() {
        if (Gdx.input.justTouched()) {
            val touchX = Gdx.input.getX().toFloat()
            val touchY = Gdx.graphics.height - Gdx.input.getY().toFloat()

            buttons.forEach { button ->
                if (button.contains(touchX, touchY)) {
                    handleButtonClick(button.text)
                }
            }
        }

        val mouseX = Gdx.input.getX().toFloat()
        val mouseY = Gdx.graphics.height - Gdx.input.getY().toFloat()
        buttons.forEach { button ->
            button.isHovered = button.contains(mouseX, mouseY)
        }
    }

    private fun handleButtonClick(buttonText: String) {
        when (buttonText) {
            "MULTIJUGADOR LOCAL" -> {
                Gdx.app.log("MenuScreen", "Iniciando multijugador local")
                game.showModeSelection()
            }
            "UN JUGADOR (IA)" -> {
                Gdx.app.log("MenuScreen", "Un jugador seleccionado")
                game.startSinglePlayer()
            }
            "BLUETOOTH" -> {
                Gdx.app.log("MenuScreen", "Bluetooth seleccionado")
                game.startBluetoothMultiplayer()
            }
            "SALIR" -> {
                Gdx.app.log("MenuScreen", "Saliendo del juego")
                Gdx.app.exit()
            }
        }
    }

    override fun resize(width: Int, height: Int) {
        camera3D?.viewportWidth = width.toFloat()
        camera3D?.viewportHeight = height.toFloat()
        camera3D?.update()

        setupButtons()
    }

    override fun pause() {}
    override fun resume() {}
    override fun hide() {}

    override fun dispose() {
        spriteBatch.dispose()
        font.dispose()
        titleFont.dispose()
        shapeRenderer.dispose()
        modelBatch?.dispose()
        logoModel?.dispose()
    }

    data class MenuButton(
        val text: String,
        var x: Float,
        var y: Float,
        val width: Float,
        val height: Float,
        var isHovered: Boolean = false
    ) {
        fun contains(px: Float, py: Float): Boolean {
            return px >= x - width / 2f && px <= x + width / 2f &&
                py >= y - height / 2f && py <= y + height / 2f
        }
    }
}
