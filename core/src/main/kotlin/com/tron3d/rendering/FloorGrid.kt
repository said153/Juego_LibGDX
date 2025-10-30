package com.tron3d.rendering

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.utils.Disposable

/**
 * Grid/Tablero estilo TRON con líneas brillantes
 */
class FloorGrid(
    val width: Int = 50,
    val height: Int = 30
) : Disposable {

    private val modelBuilder = ModelBuilder()
    private val gridInstances = mutableListOf<ModelInstance>()

    // Colores TRON
    private val gridColor = Color(0.15f, 0.6f, 0.9f, 0.8f)
    private val floorColor = Color(0.02f, 0.05f, 0.12f, 1f)

    init {
        createFloor()
        createGrid()
    }

    /**
     * Crea el suelo base oscuro
     */
    private fun createFloor() {
        val floorMaterial = Material(
            ColorAttribute.createDiffuse(floorColor),
            ColorAttribute.createSpecular(0.3f, 0.3f, 0.4f, 1f),
            BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        )

        modelBuilder.begin()

        val floorPart = modelBuilder.part(
            "floor",
            GL20.GL_TRIANGLES,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
            floorMaterial
        )

        // Suelo grande y plano
        floorPart.setVertexTransform(
            com.badlogic.gdx.math.Matrix4().setToTranslation(
                width / 2f,
                -0.1f,  // Ligeramente debajo de Y=0
                height / 2f
            )
        )
        floorPart.box(width.toFloat() + 5f, 0.2f, height.toFloat() + 5f)

        val floorModel = modelBuilder.end()
        gridInstances.add(ModelInstance(floorModel))
    }

    /**
     * Crea las líneas del grid TRON
     */
    private fun createGrid() {
        val gridMaterial = Material(
            ColorAttribute.createDiffuse(gridColor),
            ColorAttribute.createEmissive(gridColor),
            BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE, 0.9f)
        )

        val lineHeight = 0.05f
        val lineWidth = 0.08f

        // Líneas verticales (cada 2 unidades)
        for (x in 0..width step 2) {
            modelBuilder.begin()

            val linePart = modelBuilder.part(
                "grid_line_v",
                GL20.GL_TRIANGLES,
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
                gridMaterial
            )

            linePart.setVertexTransform(
                com.badlogic.gdx.math.Matrix4().setToTranslation(
                    x.toFloat(),
                    lineHeight / 2f,
                    height / 2f
                )
            )
            linePart.box(lineWidth, lineHeight, height.toFloat())

            val lineModel = modelBuilder.end()
            gridInstances.add(ModelInstance(lineModel))
        }

        // Líneas horizontales (cada 2 unidades)
        for (z in 0..height step 2) {
            modelBuilder.begin()

            val linePart = modelBuilder.part(
                "grid_line_h",
                GL20.GL_TRIANGLES,
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
                gridMaterial
            )

            linePart.setVertexTransform(
                com.badlogic.gdx.math.Matrix4().setToTranslation(
                    width / 2f,
                    lineHeight / 2f,
                    z.toFloat()
                )
            )
            linePart.box(width.toFloat(), lineHeight, lineWidth)

            val lineModel = modelBuilder.end()
            gridInstances.add(ModelInstance(lineModel))
        }

        // Líneas de borde brillantes (perímetro)
        createBorderLines(gridMaterial, lineHeight, lineWidth * 2f)
    }

    /**
     * Crea líneas brillantes en el perímetro
     */
    private fun createBorderLines(material: Material, height: Float, width: Float) {
        // Borde superior
        modelBuilder.begin()
        var part = modelBuilder.part(
            "border_top",
            GL20.GL_TRIANGLES,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
            material
        )
        part.setVertexTransform(
            com.badlogic.gdx.math.Matrix4().setToTranslation(
                this.width / 2f, height / 2f, 0f
            )
        )
        part.box(this.width.toFloat(), height, width)
        gridInstances.add(ModelInstance(modelBuilder.end()))

        // Borde inferior
        modelBuilder.begin()
        part = modelBuilder.part(
            "border_bottom",
            GL20.GL_TRIANGLES,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
            material
        )
        part.setVertexTransform(
            com.badlogic.gdx.math.Matrix4().setToTranslation(
                this.width / 2f, height / 2f, this.height.toFloat()
            )
        )
        part.box(this.width.toFloat(), height, width)
        gridInstances.add(ModelInstance(modelBuilder.end()))

        // Borde izquierdo
        modelBuilder.begin()
        part = modelBuilder.part(
            "border_left",
            GL20.GL_TRIANGLES,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
            material
        )
        part.setVertexTransform(
            com.badlogic.gdx.math.Matrix4().setToTranslation(
                0f, height / 2f, this.height / 2f
            )
        )
        part.box(width, height, this.height.toFloat())
        gridInstances.add(ModelInstance(modelBuilder.end()))

        // Borde derecho
        modelBuilder.begin()
        part = modelBuilder.part(
            "border_right",
            GL20.GL_TRIANGLES,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
            material
        )
        part.setVertexTransform(
            com.badlogic.gdx.math.Matrix4().setToTranslation(
                this.width.toFloat(), height / 2f, this.height / 2f
            )
        )
        part.box(width, height, this.height.toFloat())
        gridInstances.add(ModelInstance(modelBuilder.end()))
    }

    /**
     * Renderiza el grid completo
     */
    fun render(batch: ModelBatch) {
        gridInstances.forEach { instance ->
            batch.render(instance)
        }
    }

    override fun dispose() {
        gridInstances.forEach { instance ->
            instance.model.dispose()
        }
        gridInstances.clear()
    }
}
