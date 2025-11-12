package com.tron3d.models

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader
import com.badlogic.gdx.graphics.g3d.utils.TextureProvider
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.JsonReader
import com.tron3d.config.TronVisualConfig

/**
 * Modelo de la arena cargado desde archivo .g3db
 * Con soporte para texturas faltantes
 */
class ArenaModel : Disposable {

    private var arenaModel: Model? = null
    var arenaInstance: ModelInstance? = null
        private set

    private var isLoaded = false
    private var placeholderTexture: Texture? = null

    /**
     * Crea textura placeholder para archivos faltantes
     */
    private fun createPlaceholderTexture(): Texture {
        if (placeholderTexture == null) {
            val pixmap = Pixmap(64, 64, Pixmap.Format.RGBA8888)
            pixmap.setColor(
                TronVisualConfig.NeonColors.CYAN.r * 0.3f,
                TronVisualConfig.NeonColors.CYAN.g * 0.3f,
                TronVisualConfig.NeonColors.CYAN.b * 0.3f,
                1f
            )
            pixmap.fill()
            placeholderTexture = Texture(pixmap)
            pixmap.dispose()
        }
        return placeholderTexture!!
    }

    /**
     * Carga el modelo de la arena desde archivo
     */
    fun load(): Boolean {
        try {
            Gdx.app.log("ArenaModel", "Cargando arena desde archivo...")

            // Crear TextureProvider que maneja texturas faltantes
            val textureProvider = object : TextureProvider {
                override fun load(fileName: String): Texture {
                    return try {
                        // Intentar cargar la textura
                        val fileHandle = Gdx.files.internal(fileName)
                        if (fileHandle.exists()) {
                            Gdx.app.log("ArenaModel", "✅ Textura cargada: $fileName")
                            Texture(fileHandle)
                        } else {
                            Gdx.app.log("ArenaModel", "⚠️ Textura no encontrada, usando placeholder: $fileName")
                            createPlaceholderTexture()
                        }
                    } catch (e: Exception) {
                        Gdx.app.log("ArenaModel", "⚠️ Error cargando textura, usando placeholder: $fileName")
                        createPlaceholderTexture()
                    }
                }
            }

            // El archivo es JSON texto, usar JsonReader
            val loader = G3dModelLoader(JsonReader())

            // Cargar con el TextureProvider personalizado
            val modelData = loader.loadModelData(Gdx.files.internal("models/arena_tron.g3db"))
            arenaModel = Model(modelData, textureProvider)

            arenaInstance = ModelInstance(arenaModel)

            // Aplicar materiales Tron
            applyTronMaterials()

            // POSICIONAR Y ESCALAR LA ARENA
            // Centrada en el tablero 50x30, en Y=0 (suelo), con escala ajustable
            arenaInstance?.transform?.idt()  // Resetear transformación
            arenaInstance?.transform?.setToTranslation(25f, -5f, 15f)  // Bajar arena (Y negativo)
            arenaInstance?.transform?.scale(0.01f, 0.01f, 0.01f)  // Hacer MUY grande para verla

            isLoaded = true
            Gdx.app.log("ArenaModel", "✅ Arena cargada exitosamente")
            Gdx.app.log("ArenaModel", "Arena posicionada en (25, -5, 15) con escala 10.0")
            return true

        } catch (e: Exception) {
            Gdx.app.error("ArenaModel", "❌ Error cargando arena: ${e.message}")
            e.printStackTrace()
            isLoaded = false
            return false
        }
    }

    /**
     * Aplica colores neón a los materiales de la arena
     */
    private fun applyTronMaterials() {
        arenaInstance?.materials?.forEach { material ->
            // Añadir emisión neón (sin quitar texturas existentes)
            material.set(ColorAttribute.createEmissive(
                TronVisualConfig.NeonColors.CYAN.r * 0.15f,
                TronVisualConfig.NeonColors.CYAN.g * 0.15f,
                TronVisualConfig.NeonColors.CYAN.b * 0.15f,
                1f
            ))
        }
    }

    fun isReady(): Boolean = isLoaded && arenaInstance != null

    override fun dispose() {
        arenaModel?.dispose()
        arenaModel = null
        arenaInstance = null
        placeholderTexture?.dispose()
        placeholderTexture = null
        isLoaded = false
    }
}
