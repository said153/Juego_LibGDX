package com.tron3d.models

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3

/**
 * Segmento de rastro luminoso estilo Tron Legacy
 */
class TrailSegment(
    val start: Vector3,
    val end: Vector3,
    val color: Color,
    val width: Float = 0.3f,
    val height: Float = 2.5f
) {
    val modelInstance: ModelInstance
    private val model: Model

    init {
        model = createTrailModel()
        modelInstance = ModelInstance(model)
        updatePosition()
    }

    private fun createTrailModel(): Model {
        val builder = ModelBuilder()

        // Material con glow intenso
        val material = Material().apply {
            set(ColorAttribute.createDiffuse(color))
            set(ColorAttribute.createEmissive(
                color.r * 0.8f,  // Emisión muy fuerte para el rastro
                color.g * 0.8f,
                color.b * 0.8f,
                1f
            ))
        }

        builder.begin()

        // Crear un muro vertical entre start y end
        val part = builder.part(
            "trail",
            GL20.GL_TRIANGLES,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
            material
        )

        // Caja delgada que va de start a end
        part.box(width, height, 0.1f)

        return builder.end()
    }

    private fun updatePosition() {
        // Calcular el centro entre start y end
        val center = Vector3(start).add(end).scl(0.5f)

        // Calcular la dirección y longitud
        val direction = Vector3(end).sub(start)
        val length = direction.len()

        // Aplicar transformación
        modelInstance.transform.idt()
        modelInstance.transform.setToTranslation(center)

        // Rotar hacia la dirección correcta
        if (length > 0.01f) {
            direction.nor()
            val angle = Math.toDegrees(Math.atan2(direction.z.toDouble(), direction.x.toDouble())).toFloat()
            modelInstance.transform.rotate(Vector3.Y, angle)
        }

        // Escalar para que tenga la longitud correcta
        modelInstance.transform.scale(1f, 1f, length / 0.1f)
    }

    fun dispose() {
        model.dispose()
    }
}
