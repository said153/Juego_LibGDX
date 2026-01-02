package com.tron3d.rendering

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.Disposable
import com.tron3d.models.ArenaModel
import com.tron3d.models.LightCycle

/**
 * Sistema de renderizado TRON CON ARENA 3D PRIORIZADA
 */
class TronRenderer(private val camera: PerspectiveCamera) : Disposable {

    private val modelBatch: ModelBatch
    private val environment: Environment

    // Grid/Tablero TRON (fallback)
    private val floorGrid: FloorGrid

    // Frame buffers para efectos
    private var fboScene: FrameBuffer

    init {
        modelBatch = ModelBatch()

        // Crear grid del tablero (fallback)
        floorGrid = FloorGrid(width = 50, height = 30)

        environment = Environment()
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.9f, 0.9f, 1f, 1f))

        val mainLight = DirectionalLight()
        mainLight.set(Color.WHITE, -0.3f, -0.8f, -0.2f)
        environment.add(mainLight)

        val fillLight = DirectionalLight()
        fillLight.set(Color(0.8f, 0.8f, 0.9f, 1f), 0.5f, -0.3f, 0.5f)
        environment.add(fillLight)

        // Frame buffer para efectos
        val width = Gdx.graphics.width
        val height = Gdx.graphics.height
        fboScene = FrameBuffer(Pixmap.Format.RGBA8888, width, height, true)
    }

    /**
     * Renderiza una escena completa con efectos TRON
     */
    fun render(lightCycles: List<LightCycle>, arenaModel: ArenaModel? = null) {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        modelBatch.begin(camera)

        // Renderizar arena
        if (arenaModel != null && arenaModel.isReady()) {
            // Piso
            modelBatch.render(arenaModel.arenaInstance, environment)

            // ✅ GRADAS GENERADAS
            arenaModel.getStands().forEach { stand ->
                modelBatch.render(stand, environment)
            }
        } else {
            floorGrid.render(modelBatch)
        }

        // Renderizar motos
        lightCycles.forEachIndexed { index, cycle ->
            cycle.render(modelBatch, environment)
        }

        modelBatch.end()

        Gdx.gl.glDisable(GL20.GL_BLEND)
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
    }

    /**
     * ✅ Limpia todos los rastros visuales
     */
    fun clearTrails() {
        // Esta función es para compatibilidad, la limpieza real se hace en LightCycle
        Gdx.app.log("TronRenderer", "🧹 Solicitud de limpieza de rastros recibida")
    }

    fun resize(width: Int, height: Int) {
        fboScene.dispose()
        fboScene = FrameBuffer(Pixmap.Format.RGBA8888, width, height, true)
    }

    override fun dispose() {
        modelBatch.dispose()
        floorGrid.dispose()
        fboScene.dispose()
    }
}
