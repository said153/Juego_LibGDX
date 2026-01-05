package com.tron3d.models

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3

/**
 * TrailSegment MEJORADO - Renderiza como LÍNEA en vez de rectángulo
 * Se ve bien desde cualquier ángulo
 */
class TrailSegment(
    val start: Vector3,
    val end: Vector3,
    val color: Color,
    val width: Float = 0.25f,      // Grosor de la línea
    val height: Float = 0.25f      // Altura del rastro (casi plano)
) {
    val modelInstance: ModelInstance

    init {
        val model = createLineModel()
        modelInstance = ModelInstance(model)
    }

    /**
     * Crear modelo de línea 3D (pared vertical)
     */
    private fun createLineModel(): Model {
        val builder = ModelBuilder()
        builder.begin()

        // Material con emisión para que brille
        val material = Material().apply {
            set(ColorAttribute.createDiffuse(color))
            set(ColorAttribute.createEmissive(
                color.r * 0.8f,
                color.g * 0.8f,
                color.b * 0.8f,
                1f
            ))
            set(BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA))
        }

        val node = builder.node()
        node.id = "trail_line"

        // Crear pared vertical (trail de luz)
        val partBuilder: MeshPartBuilder = builder.part(
            "trail",
            GL20.GL_TRIANGLES,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
            material
        )

        // Crear pared vertical entre dos puntos
        createVerticalWall(partBuilder, start, end, width, height)

        return builder.end()
    }

    /**
     * Crear pared vertical entre dos puntos
     */
    private fun createVerticalWall(
        builder: MeshPartBuilder,
        start: Vector3,
        end: Vector3,
        thickness: Float,
        wallHeight: Float
    ) {
        // Calcular vector dirección
        val direction = Vector3(end).sub(start).nor()

        // Vector perpendicular para el grosor
        val perpendicular = Vector3(-direction.z, 0f, direction.x).nor()
        val halfThickness = Vector3(perpendicular).scl(thickness / 2f)

        // ✅ LOS VÉRTICES INFERIORES ESTÁN EN Y=0 (SUELO)
        val v1 = Vector3(start).add(halfThickness)
        val v2 = Vector3(start).sub(halfThickness)
        val v3 = Vector3(end).sub(halfThickness)
        val v4 = Vector3(end).add(halfThickness)

        // ✅ LOS VÉRTICES SUPERIORES ESTÁN A wallHeight DE ALTURA
        val v5 = Vector3(v1).add(0f, wallHeight, 0f)
        val v6 = Vector3(v2).add(0f, wallHeight, 0f)
        val v7 = Vector3(v3).add(0f, wallHeight, 0f)
        val v8 = Vector3(v4).add(0f, wallHeight, 0f)

        // Normales para cada cara
        val normalFront = Vector3(0f, 0f, 1f)
        val normalBack = Vector3(0f, 0f, -1f)
        val normalLeft = Vector3(-perpendicular.x, 0f, -perpendicular.z).nor()
        val normalRight = Vector3(perpendicular.x, 0f, perpendicular.z).nor()
        val normalTop = Vector3(0f, 1f, 0f)

        // Cara frontal
        builder.rect(v1, v2, v6, v5, normalFront)

        // Cara trasera
        builder.rect(v4, v8, v7, v3, normalBack)

        // Cara izquierda
        builder.rect(v4, v1, v5, v8, normalLeft)

        // Cara derecha
        builder.rect(v2, v3, v7, v6, normalRight)

        // Tapa superior
        builder.rect(v5, v6, v7, v8, normalTop)
    }

    fun dispose() {
        modelInstance.model.dispose()
    }
}
