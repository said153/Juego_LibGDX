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

        // Configurar entorno TRON
        environment = Environment()
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.3f, 0.3f, 0.4f, 1f))

        // Luz direccional principal
        val mainLight = DirectionalLight()
        mainLight.set(Color(0.5f, 0.5f, 0.6f, 1f), -0.3f, -0.8f, -0.2f)
        environment.add(mainLight)

        // Luz de acento
        val accentLight = DirectionalLight()
        accentLight.set(Color(0.3f, 0.4f, 0.5f, 1f), 0.5f, -0.3f, 0.3f)
        environment.add(accentLight)

        // Frame buffer para efectos
        val width = Gdx.graphics.width
        val height = Gdx.graphics.height
        fboScene = FrameBuffer(Pixmap.Format.RGBA8888, width, height, true)
    }

    /**
     * Renderiza una escena completa con efectos TRON
     */
    fun render(lightCycles: List<LightCycle>, arenaModel: ArenaModel? = null) {
        // Habilitar depth test
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL)

        // Habilitar blending
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        modelBatch.begin(camera)

        // ✅ 1. RENDERIZAR ARENA 3D (si está disponible)
        if (arenaModel != null && arenaModel.isReady()) {
            Gdx.app.log("TronRenderer", "✅ Renderizando arena 3D")
            modelBatch.render(arenaModel.arenaInstance, environment)

            // NO renderizar grid si hay arena
        } else {
            // Fallback: renderizar grid tradicional
            Gdx.app.log("TronRenderer", "⚠️ Arena no disponible, usando grid")
            floorGrid.render(modelBatch)
        }

        // 2. Renderizar TRAILS con blending aditivo
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        lightCycles.forEach { cycle ->
            cycle.renderTrail(modelBatch)
        }

        // 3. Renderizar MOTOS
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        lightCycles.forEach { cycle ->
            modelBatch.render(cycle.instance, environment)
        }

        modelBatch.end()

        Gdx.gl.glDisable(GL20.GL_BLEND)
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
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
