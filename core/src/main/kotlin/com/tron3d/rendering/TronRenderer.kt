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
import com.tron3d.models.LightCycle

/**
 * Sistema de renderizado TRON CON SUELO/GRID
 */
class TronRenderer(private val camera: PerspectiveCamera) : Disposable {

    private val modelBatch: ModelBatch
    private val environment: Environment

    // Grid/Tablero TRON
    private val floorGrid: FloorGrid

    // Frame buffers para efectos
    private var fboScene: FrameBuffer

    init {
        modelBatch = ModelBatch()

        // Crear grid del tablero
        floorGrid = FloorGrid(width = 50, height = 30)

        // Configurar entorno TRON
        environment = Environment()
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.2f, 0.2f, 0.3f, 1f))

        // Luz direccional principal
        val mainLight = DirectionalLight()
        mainLight.set(Color(0.4f, 0.4f, 0.5f, 1f), -0.3f, -0.8f, -0.2f)
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
    fun render(lightCycles: List<LightCycle>) {
        // Habilitar blending para efectos de transparencia y brillo
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE)

        modelBatch.begin(camera)

        // 1. Renderizar SUELO/GRID primero
        floorGrid.render(modelBatch)

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
