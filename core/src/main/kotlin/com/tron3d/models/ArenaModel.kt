package com.tron3d.models

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader
import com.badlogic.gdx.graphics.g3d.utils.TextureProvider
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.UBJsonReader

class ArenaModel : Disposable {

    private var arenaModel: Model? = null
    var arenaInstance: ModelInstance? = null
        private set

    private var isLoaded = false
    private val loadedTextures = mutableListOf<Texture>()

    private var standsGenerator: StandsGenerator? = null

    fun load(): Boolean {
        try {
            Gdx.app.log("ArenaModel", "🏟️ Cargando arena...")

            val textureProvider = object : TextureProvider {
                override fun load(fileName: String): Texture {
                    throw RuntimeException("TextureProvider no usado")
                }
            }

            val loader = G3dModelLoader(UBJsonReader())
            val modelData = loader.loadModelData(
                Gdx.files.internal("models/light_cycle_arena.g3db")
            )
            arenaModel = Model(modelData, textureProvider)
            arenaInstance = ModelInstance(arenaModel)

            // Aplicar texturas
            loadAndApplyTextures()

            // ✅ POSICIONAR Y ESCALAR PRIMERO
            arenaInstance?.transform?.idt()
            arenaInstance?.transform?.setToTranslation(25f, 0f, 15f)
            arenaInstance?.transform?.scale(0.01f, 0.01f, 0.01f)

            // ✅ DEBUG: Ver límites DESPUÉS de transformación
            val boundingBox = com.badlogic.gdx.math.collision.BoundingBox()
            arenaInstance?.calculateBoundingBox(boundingBox)
            val min = com.badlogic.gdx.math.Vector3()
            val max = com.badlogic.gdx.math.Vector3()
            boundingBox.getMin(min)
            boundingBox.getMax(max)

            Gdx.app.log("ArenaModel", "═══════════════════════════════════")
            Gdx.app.log("ArenaModel", "📐 LÍMITES REALES DE LA ARENA:")
            Gdx.app.log("ArenaModel", "   Min: X=${min.x} Y=${min.y} Z=${min.z}")
            Gdx.app.log("ArenaModel", "   Max: X=${max.x} Y=${max.y} Z=${max.z}")
            Gdx.app.log("ArenaModel", "   Ancho (X): ${max.x - min.x}")
            Gdx.app.log("ArenaModel", "   Alto (Y): ${max.y - min.y}")
            Gdx.app.log("ArenaModel", "   Profundidad (Z): ${max.z - min.z}")
            Gdx.app.log("ArenaModel", "   Centro: X=${(min.x + max.x)/2} Z=${(min.z + max.z)/2}")
            Gdx.app.log("ArenaModel", "═══════════════════════════════════")

            // ✅ GENERAR GRADAS CON LOS LÍMITES REALES
            standsGenerator = StandsGenerator()
            standsGenerator?.generate()  // ← Sin parámetros

            isLoaded = true
            Gdx.app.log("ArenaModel", "✅ Arena completa cargada")
            return true

        } catch (e: Exception) {
            Gdx.app.error("ArenaModel", "❌ Error: ${e.message}")
            e.printStackTrace()
            isLoaded = false
            return false
        }
    }

    fun getStands(): List<ModelInstance> {
        return standsGenerator?.instances ?: emptyList()
    }

    private fun loadAndApplyTextures() {
        val floorTexture = loadTexture("models/GridFloor01_DIFF.png")

        arenaInstance?.materials?.forEach { material ->
            val name = material.id ?: ""
            material.clear()

            if (name.contains("GridFloor", ignoreCase = true)) {
                floorTexture?.let { tex ->
                    material.set(TextureAttribute.createDiffuse(tex))
                    material.set(ColorAttribute.createDiffuse(Color.WHITE))
                    material.set(ColorAttribute.createEmissive(0.1f, 0.2f, 0.3f, 1f))
                }
            }
        }
    }

    private fun loadTexture(path: String): Texture? {
        return try {
            if (Gdx.files.internal(path).exists()) {
                Texture(Gdx.files.internal(path)).also { loadedTextures.add(it) }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun isReady(): Boolean = isLoaded && arenaInstance != null

    override fun dispose() {
        arenaModel?.dispose()
        arenaModel = null
        arenaInstance = null
        loadedTextures.forEach { it.dispose() }
        loadedTextures.clear()
        standsGenerator?.dispose()
        isLoaded = false
    }
}
